package com.bmg.trigon.service;

import static com.bmg.trigon.util.ApplicationConstants.*;

import com.bmg.trigon.common.dto.DirectFiDto;
import com.bmg.trigon.common.dto.HoldingsPostingDto;
import com.bmg.trigon.common.enums.*;
import com.bmg.trigon.common.model.WorkbenchBatchData;
import com.bmg.trigon.common.service.AdminSettingsService;
import com.bmg.trigon.common.service.DirectFiSharedCalculationsService;
import com.bmg.trigon.common.service.WorkbenchBatchDataSharedService;
import com.bmg.trigon.common.service.WorkbenchSharedService;
import com.bmg.trigon.config.SchedulerConfig;
import com.bmg.trigon.config.SftpProperties;
import com.bmg.trigon.repository.WorkbenchBatchDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class DirectFIScheduler {

  @Value("${com.bmg.sap.direct-fi.url}")
  private String url;

  @Value("${com.bmg.sap.direct-fi.basic-auth.username}")
  private String userName;

  @Value("${com.bmg.sap.direct-fi.basic-auth.password}")
  private String password;

  private final List<PostingCategory> POSTING_CATEGORIES =
      Arrays.asList(
          PostingCategory.HOLD_RELEASE,
          PostingCategory.HOLD_THRESHOLD,
          //          PostingCategory.RECOUPMENT,
          PostingCategory.HOLD_CODE);

  private final WorkbenchBatchDataRepository workbenchBatchDataRepository;
  private final SchedulerConfig schedulerConfig;
  private final WorkbenchBatchDataSharedService workbenchBatchDataSharedService;
  private final AdminSettingsService adminSettingsService;
  private final WorkbenchSharedService workbenchSharedService;
  private final GCSService gcsService;
  private final DirectFiSharedCalculationsService directFiSharedCalculationsService;
  private final SftpProperties sftpProperties;
  private final SftpService sftpService;

  public DirectFIScheduler(
      WorkbenchBatchDataRepository workbenchBatchDataRepository,
      SchedulerConfig schedulerConfig,
      WorkbenchBatchDataSharedService workbenchBatchDataSharedService,
      AdminSettingsService adminSettingsService,
      WorkbenchSharedService workbenchSharedService,
      GCSService gcsService,
      SftpService sftpService,
      DirectFiSharedCalculationsService directFiSharedCalculationsService,
      SftpProperties sftpProperties) {

    this.workbenchBatchDataRepository = workbenchBatchDataRepository;
    this.schedulerConfig = schedulerConfig;
    this.workbenchBatchDataSharedService = workbenchBatchDataSharedService;
    this.workbenchSharedService = workbenchSharedService;
    this.adminSettingsService = adminSettingsService;
    this.gcsService = gcsService;
    this.directFiSharedCalculationsService = directFiSharedCalculationsService;
    this.sftpService = sftpService;
    this.sftpProperties = sftpProperties;
  }

  @Scheduled(fixedDelayString = "#{${sap-td.scheduler-interval-in-min} * 60000}")
  protected void scheduleTasks() {
    if (!schedulerConfig.isSchedulerJobsEnabled()) {
      log.info("Direct FI scheduler is disabled. Skipping this run.");
      return;
    }
    directFiPostings();
  }

  public void directFiPostings() {
    final int pageSize = 200;
    int pageNumber = 0;
    int totalPages = 0;
    int totalRetrievedRecords = 0;
    int totalSavedRecords = 0;

    log.info("Direct FI scheduler started.");
    Instant startTime = Instant.now();

    Map<AdminSettingType, Map<UUID, Map<String, String>>> adminSettingsCache =
        adminSettingsService.storeAllAdminSettingsDataBeforeProcess(
            new HashMap<>(),
            List.of(
                AdminSettingType.OFFSET_ACCOUNT,
                AdminSettingType.SAP_POSTING_KEY,
                AdminSettingType.PURCHASING_GROUP,
                AdminSettingType.ION_ASSIGNMENT_GE,
                AdminSettingType.DEFAULT_CURRENCY,
                AdminSettingType.MATERIAL,
                AdminSettingType.DEFAULT_VAT_CODE,
                AdminSettingType.SCB_INDICATOR,
                AdminSettingType.SGL_INDICATOR,
                AdminSettingType.MAIN_ADMIN_CLIENTS));

    try {
      Page<WorkbenchBatchData> workbenchBatchDataPage;

      do {
        log.info("Processing page: {}", pageNumber);

        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
        workbenchBatchDataPage =
            workbenchBatchDataRepository.findByDirectFIStatusAndCategories(
                DirectFIStatus.POSTED_TO_DIRECT_FI, POSTING_CATEGORIES, pageRequest);

        if (pageNumber == 0) {
          totalPages = workbenchBatchDataPage.getTotalPages();
        }

        int retrievedRecords = workbenchBatchDataPage.getNumberOfElements();
        totalRetrievedRecords += retrievedRecords;

        List<WorkbenchBatchData> batchDataList = workbenchBatchDataPage.getContent();
        List<DirectFiReportExcelFields> recordingDirectFiList = new ArrayList<>();
        List<DirectFiReportExcelFields> publishDirectFiList = new ArrayList<>();

        processWorkbenchBatchData(
            batchDataList, recordingDirectFiList, publishDirectFiList, adminSettingsCache);
        // Save the current batch
        if (!batchDataList.isEmpty()) {
          workbenchBatchDataSharedService.updatePostgresAndES(batchDataList);
          // audit tracking
          List<UUID> workBenchIds =
              batchDataList.stream().map(WorkbenchBatchData::getId).collect(Collectors.toList());
          workbenchBatchDataSharedService.auditTrackingAfterSavingRecords(workBenchIds);
          totalSavedRecords += batchDataList.size();
          log.info("Saved {} batch data records from page {}", batchDataList.size(), pageNumber);
        }

        // Upload Excel files to GCS
        uploadToGcs(recordingDirectFiList, publishDirectFiList);

        pageNumber++;

      } while (workbenchBatchDataPage.hasNext());

    } catch (Exception e) {
      log.error("Error occurred during direct FI postings: {}", ExceptionUtils.getStackTrace(e));
    }

    Instant endTime = Instant.now();
    Duration duration = Duration.between(startTime, endTime);
    log.info(
        "Direct FI scheduler finished in {}. Total retrieved records: {}. Total saved records: {}. Total pages retrieved: {}",
        formatDuration(duration.getSeconds()),
        totalRetrievedRecords,
        totalSavedRecords,
        totalPages);
  }

  private void processWorkbenchBatchData(
      List<WorkbenchBatchData> batchDataList,
      List<DirectFiReportExcelFields> recordingList,
      List<DirectFiReportExcelFields> publishList,
      Map<AdminSettingType, Map<UUID, Map<String, String>>> adminSettingsCache) {

    for (WorkbenchBatchData workbenchBatchData : batchDataList) {
      try {
        JsonNode response =
            prepareObjectAndXMlPayloadAndCallexternalApi(
                workbenchBatchData, recordingList, publishList, adminSettingsCache);

        log.info("Response: {} , for payee code: {}", response, workbenchBatchData.getPayeeCode());

        boolean isSuccess =
            response != null
                && response.hasNonNull(DirectFIStatus.POSTED_TO_DIRECT_FI.getDisplayName())
                && response.hasNonNull(SUCCESS_XML)
                && response.hasNonNull(ACCOUNTING_DOCUMENT);

        if (isSuccess) {
          String sapDocumentNumber = response.path(ACCOUNTING_DOCUMENT).asText();
          workbenchBatchData.setSapDocumentNumber(sapDocumentNumber);
          workbenchBatchData.setDirectFiStatus(DirectFIStatus.SUCCESS_FROM_DIRECT_FI);
        } else {
          workbenchBatchData.setDirectFiStatus(DirectFIStatus.FAILURE_FROM_DIRECT_FI);
          String errorMessage =
              response == null
                  ? TECHNICAL_ERROR
                  : response.hasNonNull(RESPONSE_LOG_ITEM)
                      ? response.get(RESPONSE_LOG_ITEM).asText()
                      : response.get(ERROR_RESPONSE).asText();
          workbenchBatchData.setDirectFiResponseNotes(errorMessage);
        }

        PostApprovalStatus postApprovalStatus =
            workbenchBatchDataSharedService.getPostApprovalStatusForPostings(workbenchBatchData);
        workbenchBatchData.setPostApprovalStatus(postApprovalStatus);
        workbenchBatchDataSharedService.updateSapDetailStatusAndHGDocument(workbenchBatchData);
      } catch (Exception e) {
        log.error(
            "Error processing batch data for payee code {}: {}",
            workbenchBatchData.getPayeeCode(),
            ExceptionUtils.getStackTrace(e));
      }
    }
  }

  private JsonNode prepareObjectAndXMlPayloadAndCallexternalApi(
      WorkbenchBatchData batchData,
      List<DirectFiReportExcelFields> recordingList,
      List<DirectFiReportExcelFields> publishList,
      Map<AdminSettingType, Map<UUID, Map<String, String>>> adminSettingsCache) {
    try {
      DirectFiDto directFiData =
          directFiSharedCalculationsService.prepareDirectFIObject(batchData, adminSettingsCache);
      String xmlPayload = createXmlPayload(directFiData);
      JsonNode response = invokeExternalApi(xmlPayload);
      checkAndAddSegementRelatedList(recordingList, publishList, xmlPayload, batchData, response);
      return response;
    } catch (Exception e) {
      log.error("Failed to get and parse API response: {}", ExceptionUtils.getStackTrace(e));
      return null;
    }
  }

  private String createXmlPayload(DirectFiDto directFiData) throws IOException {
    Segment segment = directFiData.getSegment();
    String xmlPayloadTemplate = null;

    // Read the appropriate XML template based on the segment
    if (Segment.RECORDING.equals(segment)) {
      xmlPayloadTemplate = readXmlTemplateFile(XML_PAYLOAD_TEMPLATE_REC_PATH);
    } else if (Segment.PUBLISHING.equals(segment)) {
      xmlPayloadTemplate = readXmlTemplateFile(XML_PAYLOAD_TEMPLATE_PUB_PATH);
    }

    if (xmlPayloadTemplate == null) {
      throw new IllegalStateException("XML template could not be loaded.");
    }

    return replacePlaceholders(xmlPayloadTemplate, directFiData);
  }

  // Replace placeholders in the XML template with values from Holding posting dto
  private String replacePlaceholders(String template, DirectFiDto data) {
    Field[] fields = HoldingsPostingDto.class.getDeclaredFields();
    for (Field field : fields) {
      field.setAccessible(true);
      try {
        String value = field.get(data) != null ? field.get(data).toString() : "";
        // Replace ${fieldName} placeholders
        template = template.replace("${" + field.getName() + "}", value);
      } catch (IllegalAccessException e) {
        log.error("Error while accessing field {}: {}", field.getName(), e.getMessage());
      }
    }
    return template;
  }

  private void checkAndAddSegementRelatedList(
      List<DirectFiReportExcelFields> recordingList,
      List<DirectFiReportExcelFields> publishList,
      String xml,
      WorkbenchBatchData batchData,
      JsonNode node) {
    Segment segment = batchData.getWorkbenchCriteria().getSegment();
    DirectFiReportExcelFields excelFields = createObjectForExcelGeneration(batchData, xml, node);

    if (Segment.RECORDING == segment) {
      recordingList.add(excelFields);
    } else if (Segment.PUBLISHING == segment) {
      publishList.add(excelFields);
    }
  }

  private DirectFiReportExcelFields createObjectForExcelGeneration(
      WorkbenchBatchData batchData, String xml, JsonNode node) {
    DirectFiReportExcelFields excelFields = new DirectFiReportExcelFields();
    excelFields.setWorkBenchCriteriaId(batchData.getWorkbenchCriteria().getId().toString());
    excelFields.setBatchId(batchData.getWorkbenchBatch().getId().toString());
    excelFields.setBatchName(batchData.getWorkbenchCriteria().getMainBatchName());
    if (Segment.PUBLISHING.equals(batchData.getWorkbenchCriteria().getSegment())) {
      excelFields.setClientCode(batchData.getClientCode());
    }
    if (Segment.RECORDING.equals(batchData.getWorkbenchCriteria().getSegment())) {
      excelFields.setPayContractKey(batchData.getPayeeContractKey());
    }
    excelFields.setPayeeCode(batchData.getPayeeCode());
    excelFields.setSegment(batchData.getWorkbenchCriteria().getSegment().toString());
    excelFields.setPayloadXmlContent(xml);
    excelFields.setSuccessXmlContent(
        node.has(SUCCESS_XML) ? node.get(SUCCESS_XML).asText().trim() : "");
    excelFields.setFailXmlContent(node.has(FAIL_XML) ? node.get(FAIL_XML).asText().trim() : "");
    return excelFields;
  }

  private void uploadToGcs(
      List<DirectFiReportExcelFields> recordingList, List<DirectFiReportExcelFields> publishList) {
    if (!recordingList.isEmpty() && generateExcelSheetAndUpload(recordingList)) {
      log.info("Upload done to GCS for recordings");
    }
    if (!publishList.isEmpty() && generateExcelSheetAndUpload(publishList)) {
      log.info("Upload done to GCS for publish");
    }
  }

  private String formatDuration(long seconds) {
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long remainingSeconds = seconds % 60;
    return String.format("%d hours, %d minutes, %d seconds", hours, minutes, remainingSeconds);
  }

  public JsonNode invokeExternalApi(String xmlPayload) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE);
    String auth = userName + ":" + password;
    String authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    headers.set(HttpHeaders.AUTHORIZATION, authHeader);
    HttpEntity<String> entity = new HttpEntity<>(xmlPayload, headers);
    List<String> successPrefixes = Arrays.asList("Document posted successfully");
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
      if (response.getStatusCode() == HttpStatus.OK) {
        String accountingDocumentPattern = "<AccountingDocument>(.*?)</AccountingDocument>";
        String notePattern = "<Note>(.*?)</Note>";
        String responseBody = response.getBody();
        String accountingDocument = extractSingleValue(accountingDocumentPattern, responseBody);
        List<String> notes = extractValues(notePattern, responseBody);
        ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
        jsonObject.put(ACCOUNTING_DOCUMENT, accountingDocument);
        String notesString = notes.stream().collect(Collectors.joining(", "));
        jsonObject.put(RESPONSE_LOG_ITEM, notesString);
        boolean isPosted =
            notes.stream().anyMatch(note -> successPrefixes.stream().anyMatch(note::contains));
        if (isPosted) {
          jsonObject.put(DirectFIStatus.POSTED_TO_DIRECT_FI.getDisplayName(), true);
        }
        jsonObject.put(SUCCESS_XML, responseBody);
        return jsonObject;
      } else {
        log.info("Error while triggering Direct FI API: {}", response);
        return createErrorResponse(
            TECHNICAL_ERROR + ":" + response.getStatusCode(), response.toString());
      }
    } catch (Exception e) {
      String statusCode = "";
      if (e instanceof HttpStatusCodeException) {
        statusCode = ((HttpStatusCodeException) e).getStatusCode().toString();
      }
      log.info("Exception occurred triggering Direct FI API: {}", ExceptionUtils.getStackTrace(e));
      return createErrorResponse(
          TECHNICAL_ERROR + ":" + statusCode, "Exception details: " + e.getMessage());
    }
  }

  private ObjectNode createErrorResponse(String errorMessage, String failXml) {
    ObjectNode errorJson = JsonNodeFactory.instance.objectNode();
    errorJson.put(ERROR_RESPONSE, errorMessage);
    errorJson.put(FAIL_XML, failXml);
    return errorJson;
  }

  private String extractSingleValue(String pattern, String xml) {
    Pattern regex = Pattern.compile(pattern);
    Matcher matcher = regex.matcher(xml);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return null;
  }

  private List<String> extractValues(String pattern, String xml) {
    Pattern regex = Pattern.compile(pattern);
    Matcher matcher = regex.matcher(xml);
    List<String> results = new ArrayList<>();
    while (matcher.find()) {
      results.add(matcher.group(1).trim());
    }
    return results;
  }

  public boolean generateExcelSheetAndUpload(List<DirectFiReportExcelFields> dataList) {
    if (dataList.isEmpty()) {
      log.info("no data found");
    }
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    String formattedDateTime = now.format(formatter);
    String modifiedName = dataList.get(0).getBatchName().replace("/", "-");
    String fileName = modifiedName + "_" + formattedDateTime + ".xlsx";
    File tempFile = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
    String segment = dataList.get(0).getSegment();
    try (Workbook workbook = new XSSFWorkbook();
        FileOutputStream fileOut = new FileOutputStream(tempFile)) {
      Sheet sheet = workbook.createSheet("report");

      List<Field> fields =
          Arrays.stream(DirectFiReportExcelFields.class.getDeclaredFields())
              .peek(field -> field.setAccessible(true))
              .toList();

      // Create the header row
      Row headerRow = sheet.createRow(0);
      fields.stream()
          .map(Field::getName)
          .forEachOrdered(
              name -> {
                Cell cell = headerRow.createCell(headerRow.getPhysicalNumberOfCells());
                cell.setCellValue(name);
              });

      // Add data rows
      dataList.forEach(
          rowData -> {
            Row row = sheet.createRow(sheet.getPhysicalNumberOfRows());
            fields.forEach(
                field -> {
                  try {
                    Object value = field.get(rowData);
                    Cell cell = row.createCell(fields.indexOf(field));
                    cell.setCellValue(value != null ? value.toString() : "");
                  } catch (IllegalAccessException e) {
                    log.error("Error accessing field value for field: {}", field.getName(), e);
                  }
                });
          });

      workbook.write(fileOut);
      log.info("Excel file created successfully at {}", tempFile.getAbsolutePath());
      try {
        String uploadedFileInfo = gcsService.uploadFile(tempFile, segment, fileName);
        log.info("File uploaded successfully to gcs :{}", uploadedFileInfo);
      } catch (Exception e) {
        log.error("Error uploading to GCS: {}", e.getMessage());
      }

      if (uploadToSftp(tempFile)) {
        log.info("upload file to ftp");
      } else {
        log.info("failed to upload  file to ftp");
      }
      return true;
    } catch (IOException e) {
      log.error("Failed to create or write to Excel file", e);
      return false;
    } finally {
      if (tempFile.exists() && tempFile.delete()) {
        log.info("file deleted successfully:{}", tempFile.getAbsolutePath());
      }
    }
  }

  public boolean uploadToSftp(File localFile) {
    String remotePath = sftpProperties.getSapMdDirectFiDir() + "/" + localFile.getName();
    return sftpService.uploadFile(localFile, remotePath);
  }

  public String readXmlTemplateFile(String filePath) throws IOException {
    ClassPathResource resource = new ClassPathResource(filePath);
    try (InputStream inputStream = resource.getInputStream()) {
      byte[] bytes = inputStream.readAllBytes();
      return new String(bytes);
    }
  }

  @Data
  public static class DirectFiReportExcelFields {
    String workBenchCriteriaId;
    String batchId;
    String payeeCode;
    String clientCode;
    String payContractKey;
    String batchName;
    String segment;
    String payloadXmlContent;
    String successXmlContent;
    String failXmlContent;
  }
}
