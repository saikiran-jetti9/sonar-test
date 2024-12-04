package com.bmg.trigon.service;

import com.bmg.trigon.common.util.ApplicationConstantsShared;
import com.bmg.trigon.config.SftpProperties;
import com.bmg.trigon.dto.SAPMDResponse;
import com.bmg.trigon.exception.CustomException;
import com.bmg.trigon.model.SAPMasterData;
import com.bmg.trigon.model.Segment;
import com.bmg.trigon.repository.SAPMasterDataRepository;
import com.bmg.trigon.util.ApplicationConstants;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import jakarta.xml.bind.JAXBException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@Transactional
public class FTPWatcherService {

  private final String XML = "xml";
  private final String VENDOR_TAG = "LIFNR";
  private final TaskScheduler taskScheduler;
  private final SftpService sftpService;
  private final SftpProperties sftpProperties;
  private final WebClient sapClient;
  private final SAPMasterDataRepository sapMasterDataRepository;
  private final JdbcTemplate jdbcTemplate;
  private final EmailService emailService;

  public FTPWatcherService(
      TaskScheduler taskScheduler,
      SftpService sftpService,
      SftpProperties sftpProperties,
      WebClient sapClient,
      SAPMasterDataRepository sapMasterDataRepository,
      JdbcTemplate jdbcTemplate,
      EmailService emailService) {
    this.taskScheduler = taskScheduler;
    this.sftpService = sftpService;
    this.sftpProperties = sftpProperties;
    this.sapClient = sapClient;
    this.sapMasterDataRepository = sapMasterDataRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.emailService = emailService;
  }

  @PostConstruct
  public void watchSftpForXml() {
    taskScheduler.scheduleAtFixedRate(
        () -> {
          try {
            log.info("Started reading Unprocessed master data");
            handleUnprocessedXmlFiles();
          } catch (IOException | JAXBException | XMLStreamException e) {
            log.error("Error occurred while processing master data xml files", e.getMessage());
          }
          // processVendor(Long.valueOf(30000367), "Vendor_20231013_151949_1.xml");
        },
        Duration.ofMinutes(sftpProperties.getSapMdWatchIntervalInMin()));
  }

  private String formatDate(String dateValue, String dateFormat) {
    if (StringUtils.isNotBlank(dateValue)) {
      return DateFormatUtils.format(Long.valueOf(dateValue), dateFormat);
    }
    return null;
  }

  private String getStringValue(JSONObject jsonObject, String jsonKey) {
    if (jsonObject.has(jsonKey)) {
      if (!jsonObject.isNull(jsonKey)) {
        return jsonObject.getString(jsonKey);
      }
    }
    return null;
  }

  private String getBooleanAsStringValue(JSONObject jsonObject, String jsonKey) {
    if (jsonObject.has(jsonKey)) {
      if (!jsonObject.isNull(jsonKey)) {
        return String.valueOf(jsonObject.getBoolean(jsonKey));
      }
    }
    return null;
  }

  private void handleUnprocessedXmlFiles() throws IOException, JAXBException, XMLStreamException {
    log.info(
        "Started reading Unprocessed master data xml files from ftp directory {}",
        sftpProperties.getSapMdUnprocessedDir());
    List<String> unProcessedFiles =
        sftpService.listAllFile(sftpProperties.getSapMdUnprocessedDir());
    if (CollectionUtils.isEmpty(unProcessedFiles)) {
      log.info(
          "No unprocessed files found in ftp location {}", sftpProperties.getSapMdUnprocessedDir());
    }
    int xmlFilesCount = 0;
    for (String filePath : unProcessedFiles) {
      String fileExt = FilenameUtils.getExtension(filePath);
      // ftp is giving . and .. also as unprocessed files. Hence added xml extension check
      if (XML.equalsIgnoreCase(fileExt)) {
        xmlFilesCount++;
        log.info("Downloading file {} from  ftp", filePath);
        Path path = Files.createTempFile("", filePath);
        File file = path.toFile();
        InputStream xmlStream =
            sftpService.downloadFileAsStream(
                sftpProperties.getSapMdUnprocessedDir() + filePath, file.getAbsolutePath());
        log.info("File {} has been downloaded to {}", filePath, file.getAbsolutePath());
        FileUtils.copyInputStreamToFile(xmlStream, file);
        Set<String> vendorNumbers = readXmlFileForVendors(file);
        processVendorsWithFtpFilePath(vendorNumbers, filePath);
        log.info("Found vendor number {} in file {} and processed", vendorNumbers, filePath);
      }
    }
    if (xmlFilesCount == 0) {
      log.info(
          "No unprocessed files found in ftp location {}", sftpProperties.getSapMdUnprocessedDir());
    }
  }

  private void processVendorsWithFtpFilePath(Set<String> vendorNumbers, String ftpFilePath) {
    CompletableFuture.runAsync(
        () -> {
          log.info("Processing vendors. FTP path: {}, Vendors: {}", ftpFilePath, vendorNumbers);

          Set<Long> successVendors = new HashSet<>();
          Set<SAPMDResponse> sapMDResponses = new HashSet<>();

          for (String vendor : vendorNumbers) {
            SAPMDResponse sapMDResponse = new SAPMDResponse();
            Long vendorNumber = Long.valueOf(vendor);
            try {
              processVendor(vendorNumber, ftpFilePath, successVendors, sapMDResponse);
            } catch (CustomException e) {
              log.error(
                  "Error while invoking the apis for vendor {} in file {}.",
                  vendorNumber,
                  ftpFilePath,
                  e);
              sapMDResponse =
                  SAPMDResponse.builder()
                      .vendorNumber(vendorNumber)
                      .status(ApplicationConstantsShared.FAIL)
                      .apiResults(Map.of(e.getApiUrl(), e.getErrorMessage()))
                      .build();
            } finally {
              if (ApplicationConstantsShared.FAIL.equals(sapMDResponse.getStatus())) {
                sapMDResponses.add(sapMDResponse);
              } else {
                successVendors.add(vendorNumber);
              }
            }
          }

          handlePostProcessing(ftpFilePath, sapMDResponses, successVendors);
        });
  }

  /**
   * Handles post-processing after the SAP Master Data (MD) refresh process. Based on the success
   * and failure of vendor processing, this method logs information, moves the file to the
   * appropriate folder (error or processed), and sends emails notifying about the outcome.
   *
   * @param ftpFilePath The file path of the FTP file being processed. If null, it indicates that
   *     the process was triggered from the UI.
   * @param sapMDResponses A set of {@link SAPMDResponse} objects representing vendors that failed
   *     during the SAP MD refresh process.
   * @param successVendors A set of vendor numbers that were processed successfully during the SAP
   *     MD refresh process.
   */
  public void handlePostProcessing(
      String ftpFilePath, Set<SAPMDResponse> sapMDResponses, Set<Long> successVendors) {

    if (!sapMDResponses.isEmpty()) {
      log.info(
          "Failed vendors: {}. Successful vendors: {}. FTP path: {}",
          sapMDResponses.stream().map(SAPMDResponse::getVendorNumber).collect(Collectors.toList()),
          successVendors,
          ftpFilePath != null ? ftpFilePath : "");
      if (ftpFilePath != null) {
        moveToError(ftpFilePath);
        emailService.sendSAPMDErrorEmail(
            ftpFilePath, sftpProperties.getSapMdErrorDir(), successVendors, sapMDResponses);
      } else {
        // When SAP MD is refreshed from UI, In that scenario ftp file path and error folder path
        // should be empty
        emailService.sendSAPMDErrorEmail("", "", successVendors, sapMDResponses);
      }

    } else if (!successVendors.isEmpty()) {
      log.info(
          "All vendors processed successfully. Successful vendors: {}. FTP path: {}",
          successVendors,
          ftpFilePath != null ? ftpFilePath : "");
      if (ftpFilePath != null) {
        moveToProcessed(ftpFilePath);
      }
    }
  }

  private Set<String> readXmlFileForVendors(File xmlFile) {
    log.info("Reading xml file {} for vendor numbers ", xmlFile.getAbsolutePath());
    Set<String> vendorNumbers = new HashSet<>();
    try (FileInputStream fileInputStream = new FileInputStream(xmlFile)) {
      // Path xmlFile = Paths.get("src", "main", "resources", "data",
      // "Vendor_20230913_165756_1.xml");
      XMLEventReader reader = getXmlInputFactory().createXMLEventReader(fileInputStream);
      // reading vendor number from whole xml document.
      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();
        if (event.isStartElement()) {
          StartElement element = event.asStartElement();
          switch (element.getName().getLocalPart()) {
            case VENDOR_TAG:
              event = reader.nextEvent();
              if (event.isCharacters()) {
                vendorNumbers.add(event.asCharacters().getData());
              }
              break;
          }
        }
      }
      log.info("Found vendor number {} in file {}", vendorNumbers, xmlFile.getAbsolutePath());
      return vendorNumbers;
    } catch (FileNotFoundException e) {
      log.info("File not found {}", e.getMessage());
    } catch (IOException e) {
      log.info("Unable to read file {}", e.getMessage());
    } catch (XMLStreamException e) {
      log.info("Unable to read xml file {}", e.getMessage());
    } finally {
      FileUtils.deleteQuietly(xmlFile);
    }
    return vendorNumbers;
  }

  public void processVendor(
      Long vendorNumber,
      @Nullable String ftpFilePath,
      Set<Long> successVendors,
      SAPMDResponse sapMDResponse) {

    sapMDResponse.setVendorNumber(vendorNumber);
    List<SAPMasterData> existingMDOfVendor =
        sapMasterDataRepository.findAllByAccountNumberOfVendorOrCreditor(vendorNumber);

    Map<String, Long> vendorCompanyCodeVsRecordId = new HashMap<>();
    existingMDOfVendor.forEach(
        sapMasterData -> {
          String vendorKey =
              sapMasterData.getAccountNumberOfVendorOrCreditor()
                  + "_"
                  + sapMasterData.getCompanyCode();
          vendorCompanyCodeVsRecordId.put(vendorKey, sapMasterData.getId());
        });

    // creating sapmaserdatas based on supplier companies available in the api
    List<SAPMasterData> newSapMasterDatas = createMasterDataFromSupplierCompanyApi(vendorNumber);
    if (!CollectionUtils.isEmpty(newSapMasterDatas)) {
      log.info("Found {} company codes for vendor{} ", newSapMasterDatas.size(), vendorNumber);
      for (SAPMasterData newSapMasterData : newSapMasterDatas) {
        log.info(
            "Fetching other information from rest of the apis for vendor {} and company code {} ",
            vendorNumber,
            newSapMasterData.getCompanyCode());
        // building rest of information for newly created sapmasterdata
        newSapMasterData.setAccountNumberOfVendorOrCreditor(vendorNumber);
        updateMasterDataSupplierInformation(vendorNumber, newSapMasterData);
        updateMasterDataBusinessPartnerAddressInformation(vendorNumber, newSapMasterData);
        updateMasterDataBusinessPartnerBankInformation(vendorNumber, newSapMasterData);
        updateMasterDataSupplierWithHoldingTaxInformation(vendorNumber, newSapMasterData);
        updateMasterDataSupplierPurchasingOrgInformation(vendorNumber, newSapMasterData);
        // TODO - we may not need segment in masterdata. avoid this column
        if (String.valueOf(vendorNumber).startsWith("30")) {
          newSapMasterData.setSegment(Segment.PUBLISHING);
        } else {
          newSapMasterData.setSegment(Segment.RECORDING);
        }
        log.info(
            "Fetched other information from rest of the apis for vendor {} and company code {} ",
            vendorNumber,
            newSapMasterData.getCompanyCode());
        log.info(
            "Saving masterdata for vendor {} and company code {}",
            vendorNumber,
            newSapMasterData.getCompanyCode());
        // Updating audit related fields

        // Set FTP-related info if ftpFilePath is available
        if (ftpFilePath != null) {
          newSapMasterData.setImportedFromFileName(ftpFilePath);
        }
        ZonedDateTime currentSystemTime = ZonedDateTime.now(ZoneId.of("UTC"));
        newSapMasterData.setCreatedTime(currentSystemTime);

        String vendorKey =
            newSapMasterData.getAccountNumberOfVendorOrCreditor()
                + "_"
                + newSapMasterData.getCompanyCode();
        Long oldVendorRecordId = vendorCompanyCodeVsRecordId.get(vendorKey);
        if (oldVendorRecordId != null) {
          sapMasterDataRepository.deleteById(oldVendorRecordId);
          // Ensures that the delete operation is executed immediately in the database,
          // preventing issues like duplicate entries before saving the new record.
          sapMasterDataRepository.flush();
          log.info(
              "Deleted old masterdata, vendor Code is {}, Company Code is {}, record Id is {}",
              newSapMasterData.getAccountNumberOfVendorOrCreditor(),
              newSapMasterData.getCompanyCode(),
              oldVendorRecordId);
        }
        try {
          sapMasterDataRepository.save(newSapMasterData);
        } catch (Exception e) {
          log.error("Vendor already exists", e);
        }
        log.info(
            "Saved masterdata for vendor {} and company code {}",
            vendorNumber,
            newSapMasterData.getCompanyCode());
      }
      log.info("Done with saving all new masterdatas");
      log.info("Master data updated successfully for vendor {}", vendorNumber);
      successVendors.add(vendorNumber);
    } else {
      log.warn(
          "Sap master data not found for vendor {} in Trigon database, FTP path {}",
          vendorNumber,
          ftpFilePath);
      sapMDResponse
          .getApiResults()
          .put(
              ApplicationConstants.SUPPLIER_COMPANY_API_ENDPOINT,
              ApplicationConstants.SAP_MASTER_DATA_NOT_FOUND_MESSAGE);
      sapMDResponse.setStatus(ApplicationConstantsShared.FAIL);
    }
  }

  private void updateMasterDataSupplierInformation(Long vendorNumber, SAPMasterData sapMasterData) {
    log.info(
        "Updating fields [Supplier, VATRegistration, TaxNumber1, TaxNumberResponsible, "
            + "PostingIsBlocked, DeletionIndicator, SupplierName, PurchasingIsBlockedForSupplier, LiableForVAT] "
            + "for vendor number {} from supplier API",
        vendorNumber);

    try {
      Mono<JSONObject> response =
          sapClient
              .get()
              .uri("/A_Supplier('" + vendorNumber + "')?$format=json")
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  clientResponse -> {
                    int statusCode = clientResponse.statusCode().value();
                    return Mono.error(
                        new CustomException(
                            ApplicationConstants.SUPPLIER_COMPANY_API_ENDPOINT,
                            statusCode,
                            ApplicationConstants.SAP_API_EXCEPTION_MESSAGE));
                  })
              .bodyToMono(String.class)
              .flatMap(
                  responseBody -> {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    return Mono.just(jsonObject);
                  });
      JSONObject jsonObject = response.block();
      log.debug(jsonObject.get(ApplicationConstants.DATA).toString());
      System.out.println(jsonObject.get(ApplicationConstants.DATA));

      if (!jsonObject.isEmpty() && jsonObject.has(ApplicationConstants.DATA)) {
        JSONObject vendorData = jsonObject.getJSONObject(ApplicationConstants.DATA);
        sapMasterData.setVatRegistrationNumber(
            getStringValue(vendorData, ApplicationConstants.VAT_REGISTRATION));
        sapMasterData.setTaxNumber1(getStringValue(vendorData, ApplicationConstants.TAX_NUMBER1));
        sapMasterData.setTaxNumberAtResponsibleTaxAuthority(
            getStringValue(vendorData, ApplicationConstants.TAX_NUMBER_RESPONSIBLE));
        sapMasterData.setCentralPostingBlock(
            getBooleanAsStringValue(vendorData, ApplicationConstants.POSTING_IS_BLOCKED));
        sapMasterData.setCentralDeletionFlagForMasterRecord(
            getBooleanAsStringValue(vendorData, ApplicationConstants.DELETION_INDICATOR));
        sapMasterData.setName1(getStringValue(vendorData, ApplicationConstants.SUPPLIER_NAME));
        sapMasterData.setLiableForVAT(
            getBooleanAsStringValue(vendorData, ApplicationConstants.ZZ1_STKZU_SUP));
        // TODO: This field is not coming up in the response json. Need to check.
        /*sapMasterData.setPurchaseBlockPurchasingOrganisationLevel(
                    getStringValue(vendorData, ApplicationConstants.PURCHASING_IS_BLOCKED_FOR_SUPPLIER));
        */
      } else {
        log.info("No results found from api A_Supplier for vendor {}", vendorNumber);
      }
    } catch (Exception e) {
      String apiURL = ApplicationConstants.SUPPLIER_API_ENDPOINT;
      handleCustomException(e, apiURL);
    }
  }

  /*
   * creates sapmaserdatas based on supplier companies available in the api
   *  */
  private List<SAPMasterData> createMasterDataFromSupplierCompanyApi(Long vendorNumber) {
    List<SAPMasterData> sapMasterDataList = new ArrayList<>();
    try {
      log.info(
          "Updating fields [CompanyCode, PaymentMethodsList, PaymentBlockingReason, SupplierIsBlockedForPosting, "
              + "SupplierAccountGroup, WithholdingTaxCountry, ReconciliationAccount] "
              + "for vendor number {} from supplierCompany Api",
          vendorNumber);
      Mono<JSONObject> response =
          sapClient
              .get()
              .uri("/A_SupplierCompany?$format=json&$filter=(Supplier eq '" + vendorNumber + "')")
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  clientResponse -> {
                    int statusCode = clientResponse.statusCode().value();
                    return Mono.error(
                        new CustomException(
                            ApplicationConstants.SUPPLIER_COMPANY_API_ENDPOINT,
                            statusCode,
                            ApplicationConstants.SAP_API_EXCEPTION_MESSAGE));
                  })
              .bodyToMono(String.class)
              .flatMap(
                  responseBody -> {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    return Mono.just(jsonObject);
                  });
      JSONObject jsonObject = response.block();
      log.debug(jsonObject.get(ApplicationConstants.DATA).toString());
      System.out.println(jsonObject.get(ApplicationConstants.DATA));
      JSONObject jsonDataObject = jsonObject.getJSONObject(ApplicationConstants.DATA);
      JSONArray results = jsonDataObject.getJSONArray(ApplicationConstants.RESULTS);
      if (!results.isEmpty()) {
        results.forEach(
            result -> {
              JSONObject vendorData = (JSONObject) result;
              SAPMasterData sapMasterData = new SAPMasterData();
              sapMasterData.setCompanyCode(
                  getStringValue(vendorData, ApplicationConstants.COMPANY_CODE));
              sapMasterData.setPaymentMethods(
                  getStringValue(vendorData, ApplicationConstants.PAYMENT_METHODS_LIST));
              sapMasterData.setBlockKeyForPayment(
                  getStringValue(vendorData, ApplicationConstants.PAYMENT_BLOCKING_REASON));
              sapMasterData.setVendorAccountGroup(
                  getStringValue(vendorData, ApplicationConstants.SUPPLIER_ACCOUNT_GROUP));
              sapMasterData.setWithholdingTaxCountryKey(
                  getStringValue(vendorData, ApplicationConstants.WITH_HOLDING_TAX_COUNTRY));
              sapMasterData.setReconciliationAccount(
                  getStringValue(vendorData, ApplicationConstants.RECONCILIATION_ACCOUNT));
              sapMasterDataList.add(sapMasterData);
            });
      } else {
        log.info("No results found from api A_SupplierCompany for vendor {}", vendorNumber);
      }
      return sapMasterDataList;
    } catch (Exception e) {
      String apiURL = ApplicationConstants.SUPPLIER_COMPANY_API_ENDPOINT;
      handleCustomException(e, apiURL);
    }
    return sapMasterDataList;
  }

  private void updateMasterDataBusinessPartnerAddressInformation(
      Long vendorNumber, SAPMasterData sapMasterData) {
    log.info(
        "Updating fields [Country, Language, StreetName, AdditionalStreetPrefixName, "
            + "AdditionalStreetSuffixName, CityName, Region, PostalCode] "
            + "for vendor number {} from businessPartnerAddress API",
        vendorNumber);
    try {

      Mono<JSONObject> response =
          sapClient
              .get()
              .uri(
                  "/A_BusinessPartnerAddress?$format=json&$filter=(BusinessPartner eq '"
                      + vendorNumber
                      + "')")
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  clientResponse -> {
                    int statusCode = clientResponse.statusCode().value();
                    return Mono.error(
                        new CustomException(
                            ApplicationConstants.BUSINESS_PARTNER_ADDRESS_API_END_POINT,
                            statusCode,
                            ApplicationConstants.SAP_API_EXCEPTION_MESSAGE));
                  })
              .bodyToMono(String.class)
              .flatMap(
                  responseBody -> {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    return Mono.just(jsonObject);
                  });

      JSONObject jsonObject = response.block();
      log.debug(jsonObject.get(ApplicationConstants.DATA).toString());
      System.out.println(jsonObject.get(ApplicationConstants.DATA));
      JSONObject jsonDataObject = jsonObject.getJSONObject(ApplicationConstants.DATA);
      JSONArray results = jsonDataObject.getJSONArray(ApplicationConstants.RESULTS);
      if (!results.isEmpty()) {
        JSONObject vendorData = results.getJSONObject(0);
        sapMasterData.setCountryKey(getStringValue(vendorData, ApplicationConstants.COUNTRY));
        sapMasterData.setLanguageKey(getStringValue(vendorData, ApplicationConstants.LANGUAGE));
        sapMasterData.setStreet(getStringValue(vendorData, ApplicationConstants.STREET_NAME));
        sapMasterData.setStreet2(
            getStringValue(vendorData, ApplicationConstants.ADDITIONAL_STREET_PREFIX_NAME));
        sapMasterData.setStreet3(
            getStringValue(vendorData, ApplicationConstants.ADDITIONAL_STREET_SUFFIX_NAME));
        sapMasterData.setCity(getStringValue(vendorData, ApplicationConstants.CITY_NAME));
        sapMasterData.setRegion(getStringValue(vendorData, ApplicationConstants.REGION));
        sapMasterData.setPostalCode(getStringValue(vendorData, ApplicationConstants.POSTAL_CODE));
      } else {
        log.info("No results found from api A_BusinessPartnerAddress for vendor {}", vendorNumber);
      }
    } catch (Exception e) {
      String apiURL = ApplicationConstants.BUSINESS_PARTNER_ADDRESS_API_END_POINT;
      handleCustomException(e, apiURL);
    }
  }

  private void updateMasterDataBusinessPartnerBankInformation(
      Long vendorNumber, SAPMasterData sapMasterData) {
    log.info(
        "Updating fields [BankCountryKey] " + "for vendor number {} from businessPartnerBank API",
        vendorNumber);
    try {
      Mono<JSONObject> response =
          sapClient
              .get()
              .uri(
                  "/A_BusinessPartnerBank?$format=json&$filter=(BusinessPartner eq '"
                      + vendorNumber
                      + "')")
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  clientResponse -> {
                    int statusCode = clientResponse.statusCode().value();
                    return Mono.error(
                        new CustomException(
                            ApplicationConstants.BUSINESS_PARTNER_BANK_API_END_POINT,
                            statusCode,
                            ApplicationConstants.SAP_API_EXCEPTION_MESSAGE));
                  })
              .bodyToMono(String.class)
              .flatMap(
                  responseBody -> {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    return Mono.just(jsonObject);
                  });
      JSONObject jsonObject = response.block();
      log.debug(jsonObject.get(ApplicationConstants.DATA).toString());
      System.out.println(jsonObject.get(ApplicationConstants.DATA));
      JSONObject jsonDataObject = jsonObject.getJSONObject(ApplicationConstants.DATA);
      JSONArray results = jsonDataObject.getJSONArray(ApplicationConstants.RESULTS);
      if (!results.isEmpty()) {
        JSONObject vendorData = results.getJSONObject(0);
        sapMasterData.setBankCountryKey(
            getStringValue(vendorData, ApplicationConstants.BANK_COUNTRY_KEY));
      } else {
        log.info("No results found from api A_BusinessPartnerBank for vendor {}", vendorNumber);
      }
    } catch (Exception e) {
      String apiURL = ApplicationConstants.BUSINESS_PARTNER_BANK_API_END_POINT;
      handleCustomException(e, apiURL);
    }
  }

  private void updateMasterDataSupplierWithHoldingTaxInformation(
      Long vendorNumber, SAPMasterData sapMasterData) {
    log.info(
        "Updating fields [IsWithholdingTaxSubject, WithholdingTaxType, WithholdingtaxCode, ExemptionReason, "
            + "WithholdingTaxExemptionCertificate, Exemption Date Begin, Exemption Date End] "
            + "for vendor number {} from supplierWithHoldingTax API",
        vendorNumber);
    String companyCode = sapMasterData.getCompanyCode();
    try {
      Mono<JSONObject> response =
          sapClient
              .get()
              .uri(
                  "/A_SupplierWithHoldingTax?$format=json&$filter=(Supplier eq '"
                      + vendorNumber
                      + "' and CompanyCode eq'"
                      + companyCode
                      + "')")
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  clientResponse -> {
                    int statusCode = clientResponse.statusCode().value();
                    return Mono.error(
                        new CustomException(
                            ApplicationConstants.SUPPLIER_WITH_HOLDING_TAX_API_ENDPOINT,
                            statusCode,
                            ApplicationConstants.SAP_API_EXCEPTION_MESSAGE));
                  })
              .bodyToMono(String.class)
              .flatMap(
                  responseBody -> {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    return Mono.just(jsonObject);
                  });
      System.out.println("response");
      System.out.println(response);
      JSONObject jsonObject = response.block();
      log.debug(jsonObject.get(ApplicationConstants.DATA).toString());
      System.out.println(jsonObject.get(ApplicationConstants.DATA));
      JSONObject jsonDataObject = jsonObject.getJSONObject(ApplicationConstants.DATA);
      JSONArray results = jsonDataObject.getJSONArray(ApplicationConstants.RESULTS);
      if (!results.isEmpty()) {
        JSONObject vendorData = results.getJSONObject(0);
        sapMasterData.setIndicatorSubjectToWithholdingTax(
            getBooleanAsStringValue(vendorData, ApplicationConstants.IS_WITH_HOLDING_TAX_SUBJECT));
        sapMasterData.setIndicatorForWithholdingTaxType(
            getStringValue(vendorData, ApplicationConstants.WITH_HOLDING_TAX_TYPE));
        sapMasterData.setWithholdingTaxCode(
            getStringValue(vendorData, ApplicationConstants.WITH_HOLDING_TAX_CODE));
        sapMasterData.setExemptionReason(
            getStringValue(vendorData, ApplicationConstants.EXEMPTION_REASON));
        sapMasterData.setExemptionCertificateNumber(
            getStringValue(
                vendorData, ApplicationConstants.WITH_HOLDING_TAX_EXEMPTION_CERTIFICATE));
        String exemptionBeginDate =
            getStringValue(vendorData, ApplicationConstants.EXEMPTION_DATE_BEGIN);
        if (exemptionBeginDate != null) {
          exemptionBeginDate = StringUtils.substringBetween(exemptionBeginDate, "(", ")");
          exemptionBeginDate =
              formatDate(exemptionBeginDate, ApplicationConstants.EXEMPTION_DATE_FORMAT);
        }
        sapMasterData.setDateOnWhichExemptionBegins(exemptionBeginDate);
        String exemptionEndDate =
            getStringValue(vendorData, ApplicationConstants.EXEMPTION_DATE_END);
        if (exemptionEndDate != null) {
          exemptionEndDate = StringUtils.substringBetween(exemptionEndDate, "(", ")");
          exemptionEndDate =
              formatDate(exemptionEndDate, ApplicationConstants.EXEMPTION_DATE_FORMAT);
        }
        sapMasterData.setDateOnWhichExemptionEnds(exemptionEndDate);
      } else {
        log.info("No results found from api A_SupplierWithHoldingTax for vendor {}", vendorNumber);
      }
    } catch (Exception e) {
      String apiURL = ApplicationConstants.SUPPLIER_WITH_HOLDING_TAX_API_ENDPOINT;
      handleCustomException(e, apiURL);
    }
  }

  private void updateMasterDataSupplierPurchasingOrgInformation(
      Long vendorNumber, SAPMasterData sapMasterData) {
    log.info(
        "Updating fields [PaymentTerms, InvoiceIsGoodsReceiptBased, DeletionIndicator, PurchasingIsBlockedForSupplier, PurchasingOrganization]"
            + "for vendor number {} from supplierPurchasingOrg API",
        vendorNumber);
    try {
      Mono<JSONObject> response =
          sapClient
              .get()
              .uri(
                  "/A_Supplier(Supplier='"
                      + vendorNumber
                      + "')"
                      + "/to_SupplierPurchasingOrg?$format=json")
              .retrieve()
              .onStatus(
                  HttpStatusCode::isError,
                  clientResponse -> {
                    int statusCode = clientResponse.statusCode().value();
                    return Mono.error(
                        new CustomException(
                            ApplicationConstants.SUPPLIER_TO_SUPPLIER_PURCHASING_ORG_API_ENDPOINT,
                            statusCode,
                            ApplicationConstants.SAP_API_EXCEPTION_MESSAGE));
                  })
              .bodyToMono(String.class)
              .flatMap(
                  responseBody -> {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    return Mono.just(jsonObject);
                  });
      JSONObject jsonObject = response.block();
      log.debug(jsonObject.get(ApplicationConstants.DATA).toString());
      System.out.println(jsonObject.get(ApplicationConstants.DATA));
      JSONObject jsonDataObject = jsonObject.getJSONObject(ApplicationConstants.DATA);
      JSONArray results = jsonDataObject.getJSONArray(ApplicationConstants.RESULTS);
      if (!results.isEmpty()) {
        JSONObject vendorData = results.getJSONObject(0);
        sapMasterData.setMmPaymentTerms(
            getStringValue(vendorData, ApplicationConstants.PAYMENT_TERMS));
        sapMasterData.setGrBasedInvVerification(
            getBooleanAsStringValue(
                vendorData, ApplicationConstants.INVOICE_IS_GOODS_RECEIPT_BASED));
        sapMasterData.setDeletionFlagForPurchasingOrganisation(
            getBooleanAsStringValue(vendorData, ApplicationConstants.DELETION_INDICATOR));
        sapMasterData.setPurchaseBlockPurchasingOrganisationLevel(
            getBooleanAsStringValue(
                vendorData, ApplicationConstants.PURCHASING_IS_BLOCKED_FOR_SUPPLIER));
        sapMasterData.setPurchaseOrganization(
            getStringValue(vendorData, ApplicationConstants.PURCHASING_ORGANIZATION));
      } else {
        log.info("No results found from api to_SupplierPurchasingOrg for vendor {}", vendorNumber);
      }
    } catch (Exception e) {
      String apiURL = ApplicationConstants.SUPPLIER_TO_SUPPLIER_PURCHASING_ORG_API_ENDPOINT;
      handleCustomException(e, apiURL);
    }
  }

  private void moveToError(String ftpFilePath) {
    String ftpUnprocessedFolder = sftpProperties.getSapMdUnprocessedDir() + ftpFilePath;
    String ftpErrorFolder = sftpProperties.getSapMdErrorDir() + ftpFilePath;
    log.info("Moving file from {} to {} ", ftpUnprocessedFolder, ftpErrorFolder);
    sftpService.move(ftpUnprocessedFolder, ftpErrorFolder);
    log.info("Moved file from {} to {} ", ftpUnprocessedFolder, ftpErrorFolder);
  }

  private void moveToProcessed(String ftpFilePath) {
    String ftpUnprocessedFolder = sftpProperties.getSapMdUnprocessedDir() + ftpFilePath;
    String ftpProcessedFolder = sftpProperties.getSapMdProcessedDir() + ftpFilePath;
    log.info("Moving file from {} to {} ", ftpUnprocessedFolder, ftpProcessedFolder);
    sftpService.move(ftpUnprocessedFolder, ftpProcessedFolder);
    log.info("Moved file from {} to {} ", ftpUnprocessedFolder, ftpProcessedFolder);
  }

  private XMLInputFactory getXmlInputFactory() {
    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    return xmlInputFactory;
  }

  /**
   * Logs the exception details and throws a {@link CustomException} with the API URL and status
   * code.
   *
   * @param e the exception to handle.
   * @param apiURL the API URL where the exception occurred.
   * @throws CustomException with the API URL, status code, and error message.
   */
  private void handleCustomException(Exception e, String apiURL) {
    // If the exception is already a CustomException, rethrow it
    if (e instanceof CustomException) {
      throw (CustomException) e; // Pass the original CustomException without altering
    }

    // Otherwise, handle the exception and throw a general CustomException
    HttpStatusCode statusCode = handleAllExceptions(e); // Default status code
    log.info(
        ApplicationConstants.TRIGON_APPLICATION_ERROR_MESSAGE + ": {}",
        ExceptionUtils.getStackTrace(e));
    throw new CustomException(
        apiURL, statusCode.value(), ApplicationConstants.TRIGON_APPLICATION_ERROR_MESSAGE);
  }

  /**
   * Determines the appropriate {@link HttpStatusCode} based on the exception type. Defaults to
   * {@link HttpStatus#INTERNAL_SERVER_ERROR} if no match is found.
   *
   * @param ex the exception to handle.
   * @return the corresponding {@link HttpStatusCode}.
   */
  public HttpStatusCode handleAllExceptions(Exception ex) {
    HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
    // Handle ResponseStatusException (common in Spring)
    if (ex instanceof ResponseStatusException responseStatusException) {
      status = responseStatusException.getStatusCode();
    }
    // Handle client errors (4xx)
    else if (ex instanceof HttpClientErrorException clientErrorException) {
      status = clientErrorException.getStatusCode();
    }
    // Handle server errors (5xx)
    else if (ex instanceof HttpServerErrorException serverErrorException) {
      status = serverErrorException.getStatusCode();
    }
    // Handle exceptions annotated with @ResponseStatus
    else if (ex.getClass().isAnnotationPresent(ResponseStatus.class)) {
      ResponseStatus responseStatus = ex.getClass().getAnnotation(ResponseStatus.class);
      status = responseStatus.value();
    }
    return status;
  }
}
