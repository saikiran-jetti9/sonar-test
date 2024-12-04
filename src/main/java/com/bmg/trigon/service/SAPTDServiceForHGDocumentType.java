package com.bmg.trigon.service;

import com.bmg.trigon.common.dto.FilterCriteria;
import com.bmg.trigon.common.enums.*;
import com.bmg.trigon.common.model.WorkbenchBatch;
import com.bmg.trigon.common.model.WorkbenchBatchData;
import com.bmg.trigon.common.service.AdminSettingsService;
import com.bmg.trigon.common.service.WorkbenchBatchDataSharedService;
import com.bmg.trigon.common.util.AdminSettingColumnConstants;
import com.bmg.trigon.common.util.StringUtil;
import com.bmg.trigon.common.util.WorkbenchBatchDataSharedUtil;
import com.bmg.trigon.dto.SAPTDResponse;
import com.bmg.trigon.dto.SAPTDResponseData;
import com.bmg.trigon.dto.SAPTDResponseURRI;
import com.bmg.trigon.repository.WorkbenchBatchDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SAPTDServiceForHGDocumentType {
  private final WorkbenchBatchDataRepository workbenchBatchDataRepository;
  private final WebClientService webClientService;
  private final ObjectMapper objectMapper;
  private final AdminSettingsService adminSettingsService;
  private final WorkbenchBatchDataSharedService workbenchBatchDataSharedService;

  public SAPTDServiceForHGDocumentType(
      WorkbenchBatchDataRepository workbenchBatchDataRepository,
      WebClientService webClientService,
      ObjectMapper objectMapper,
      AdminSettingsService adminSettingsService,
      WorkbenchBatchDataSharedService workbenchBatchDataSharedService) {
    this.workbenchBatchDataRepository = workbenchBatchDataRepository;
    this.webClientService = webClientService;
    this.objectMapper = objectMapper;
    this.adminSettingsService = adminSettingsService;
    this.workbenchBatchDataSharedService = workbenchBatchDataSharedService;
  }

  @Scheduled(fixedDelayString = "#{${sap-td.scheduler-interval-in-min} * 60000}")
  public void scheduleTasks() {
    triggerSapTDAPIs();
  }

  public void triggerSapTDAPIs() {
    log.info("SAP TD API scheduler started for HG document type");
    int pageSize = 200;
    int pageNumber = 0;
    Page<String> urriPrefixesPage;

    // Create an ExecutorService with a fixed thread pool of 2 threads
    ExecutorService executorService = Executors.newFixedThreadPool(2);

    try {
      do {
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
        Instant startTimeFetch = Instant.now();
        urriPrefixesPage =
            workbenchBatchDataRepository
                .findDistinctURRIPrefixesByPostApprovalStatusAndClearingAccountingDocument(
                    PostApprovalStatus.POSTED, pageRequest);
        Instant endTimeFetch = Instant.now();
        Duration duration = Duration.between(startTimeFetch, endTimeFetch);
        log.info(
            "Time taken to fetch distinct URRI prefixes {} for POSTED records on page {} is {} seconds",
            urriPrefixesPage.getContent(),
            pageNumber,
            duration.getSeconds());
        pageNumber++;

        if (!urriPrefixesPage.getContent().isEmpty()) {
          List<String> urriPrefixes = urriPrefixesPage.getContent();

          for (String urriPrefix : urriPrefixes) {

            List<String> urriValues =
                workbenchBatchDataRepository
                    .getURRIBasedOnPostApprovalStatusAndClearingAccountingDocumentAndURRIPrefix(
                        PostApprovalStatus.POSTED, urriPrefix + "%");

            SAPTDResponse allSapTDResponse = new SAPTDResponse();
            allSapTDResponse.setResults(new ArrayList<>());

            int skip = 0;
            int top = 10000;
            while (true) {
              Instant sapTransactionDataStartTime = Instant.now();
              JSONObject sapTransactionData =
                  webClientService.getSAPTransactionalDataForHGDocumentType(urriPrefix, skip, top);
              Instant sapTransactionDataEndTime = Instant.now();
              Duration sapTransactionDataDuration =
                  Duration.between(sapTransactionDataStartTime, sapTransactionDataEndTime);
              SAPTDResponse sapTDResponse = null;
              try {
                sapTDResponse =
                    objectMapper.readValue(sapTransactionData.toString(), SAPTDResponse.class);
                allSapTDResponse.merge(sapTDResponse, urriValues);
              } catch (JsonProcessingException e) {
                log.error(
                    "Error occurred while parsing the SAP TD response for URRI Prefix {}",
                    urriPrefix,
                    e);
                throw new RuntimeException(e);
              }

              log.info(
                  "Time taken to fetch SAP TD data for URRI Prefix {} on skip {}, top {}, total records {} is {} seconds",
                  urriPrefix,
                  skip,
                  top,
                  sapTDResponse.get__count(),
                  sapTransactionDataDuration.getSeconds());
              if (sapTDResponse.getResults().isEmpty()) {
                log.info("No more records found for URRI prefix {}, terminated", urriPrefix);
                break;
              } else {
                skip += top;
              }
            }

            SAPTDResponseURRI saptdResponseURRI =
                new SAPTDResponseURRI(allSapTDResponse.getResults());

            log.info(
                "Total records fetched for URRI prefix {} is {}",
                urriPrefix,
                saptdResponseURRI.getResults().size());

            int batchSize = 500;
            int batchPage = 0;
            long totalRecords = 0;
            Page<WorkbenchBatchData> workbenchBatchDataPage;

            Instant startTime = Instant.now();

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            do {
              PageRequest batchPageRequest = PageRequest.of(batchPage, batchSize);

              Instant dataFetchStartTime = Instant.now();
              workbenchBatchDataPage =
                  workbenchBatchDataRepository
                      .findByPostApprovalStatusAndClearingAccountingDocumentAndURRIPrefix(
                          PostApprovalStatus.POSTED, urriPrefix + "%", batchPageRequest);
              Instant dataFetchEndTime = Instant.now();
              Duration dataFetchDuration = Duration.between(dataFetchStartTime, dataFetchEndTime);

              int retrievedRecordsSize = workbenchBatchDataPage.getContent().size();
              totalRecords = workbenchBatchDataPage.getTotalElements();
              log.info(
                  "Time taken to fetch {} records from the database for URRI Prefix {} on page {} is {} seconds",
                  workbenchBatchDataPage.getContent().size(),
                  urriPrefix,
                  batchPage,
                  dataFetchDuration.getSeconds());

              if (retrievedRecordsSize > 0) {
                // Process the records in parallel using CompletableFuture
                Page<WorkbenchBatchData> finalWorkbenchBatchDataPage = workbenchBatchDataPage;
                int finalBatchPage = batchPage;
                CompletableFuture<Void> future =
                    CompletableFuture.runAsync(
                        () -> {
                          try {
                            updateSapTD(
                                finalWorkbenchBatchDataPage.getContent(), saptdResponseURRI);
                          } catch (JsonProcessingException e) {
                            log.error(
                                "Error occurred while updating SAP TD data for URRI Prefix {} on page {}",
                                urriPrefix,
                                finalBatchPage,
                                e);
                            throw new RuntimeException(e);
                          }
                        },
                        executorService);
                futures.add(future);
              }

              batchPage++;
            } while (workbenchBatchDataPage.hasNext());

            // Wait for all futures to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            Instant endTime = Instant.now();
            Duration totalDuration = Duration.between(startTime, endTime);
            log.info(
                "Total time taken to process all {} records for URRI prefix {} is {} seconds",
                totalRecords,
                urriPrefix,
                totalDuration.getSeconds());
          }
        }

      } while (urriPrefixesPage.hasNext());

    } finally {
      executorService.shutdown();
      try {
        // Wait for existing tasks to terminate
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
          log.info("Forcefully terminate the executor service");
          executorService.shutdownNow(); // Cancel currently executing tasks
        }
      } catch (InterruptedException ex) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  public void updateSapTD(
      List<WorkbenchBatchData> workbenchBatchDataList, SAPTDResponseURRI sapTDResponseURRI)
      throws JsonProcessingException {

    // Map to store results for each territory-segment combination for GL accounts
    Map<String, Map<String, List<String>>> adminSettingsMap = new HashMap<>();

    workbenchBatchDataList.forEach(
        wbBatchData -> {
          String fullURRI = wbBatchData.getRoyaltyRunIdentifierUrri();
          List<SAPTDResponseData> sapTDResponse = sapTDResponseURRI.getResultsBasedOnURRI(fullURRI);
          String territory = wbBatchData.getTerritory();
          Segment segmentEnum = wbBatchData.getWorkbenchCriteria().getSegment();
          String segment = String.valueOf(segmentEnum);
          WorkbenchBatch workbenchBatch = wbBatchData.getWorkbenchBatch();

          // SRP-1312: If the batch is an admin batch, set the segment to 'ADMIN' for processing the
          // admin settings data
          if (Boolean.TRUE.equals(workbenchBatch.getIsAdminBatch())) {
            segment = ModeOfProcessing.ADMIN.name();
          }

          String companyCode = wbBatchData.getSapCompanyCode();

          String sapDocumentNumber = wbBatchData.getSapDocumentNumberBasedOnPriority(segmentEnum);
          log.info(
              "fullURRI getting processed for HG document type {} and Trigon Identifier {}",
              fullURRI,
              wbBatchData.getId().toString());

          // Formulate unique key for the territory and segment combination
          String territorySegmentKey = territory + "_" + segment;

          // Define variables to store values
          List<String> clientGlAccountValue;
          List<String> offsetAccountValue;
          List<String> vatGlAccountValue;

          // Check if the map already contains the result for the given territory-segment
          // combination
          if (adminSettingsMap.containsKey(territorySegmentKey)) {
            // If present, retrieve values from the map
            Map<String, List<String>> segmentMap = adminSettingsMap.get(territorySegmentKey);
            clientGlAccountValue =
                segmentMap.get(
                    AdminSettingColumnConstants.ClientGlAccountConstants.CLIENT_GL_ACCOUNT_VALUE);
            offsetAccountValue =
                segmentMap.get(AdminSettingColumnConstants.OffsetAccountConstants.OFFSET_ACCOUNT);
            vatGlAccountValue =
                segmentMap.get(AdminSettingColumnConstants.VatGlAccountConstants.VAT_GL_ACCOUNT);
          } else {
            // Define filter criteria for territory and segment
            FilterCriteria territoryCriteria =
                new FilterCriteria("territory", List.of(territory), null);
            FilterCriteria segmentCriteria = new FilterCriteria("segment", List.of(segment), null);

            // Fetch values from the admin settings service using defined criteria
            List<FilterCriteria> criteriaList = List.of(territoryCriteria, segmentCriteria);
            clientGlAccountValue =
                adminSettingsService.getTheColumnSettingValueByCriteria(
                    AdminSettingType.CLIENT_GL_ACCOUNT,
                    criteriaList,
                    AdminSettingColumnConstants.ClientGlAccountConstants.CLIENT_GL_ACCOUNT_VALUE);
            offsetAccountValue =
                adminSettingsService.getTheColumnSettingValueByCriteria(
                    AdminSettingType.OFFSET_ACCOUNT,
                    criteriaList,
                    AdminSettingColumnConstants.OffsetAccountConstants.OFFSET_ACCOUNT);
            vatGlAccountValue =
                adminSettingsService.getTheColumnSettingValueByCriteria(
                    AdminSettingType.VAT_GL_ACCOUNT,
                    criteriaList,
                    AdminSettingColumnConstants.VatGlAccountConstants.VAT_GL_ACCOUNT);

            // Store the fetched values in the map for future use
            Map<String, List<String>> segmentMap = new HashMap<>();
            segmentMap.put(
                AdminSettingColumnConstants.ClientGlAccountConstants.CLIENT_GL_ACCOUNT_VALUE,
                clientGlAccountValue);
            segmentMap.put(
                AdminSettingColumnConstants.OffsetAccountConstants.OFFSET_ACCOUNT,
                offsetAccountValue);
            segmentMap.put(
                AdminSettingColumnConstants.VatGlAccountConstants.VAT_GL_ACCOUNT,
                vatGlAccountValue);
            adminSettingsMap.put(territorySegmentKey, segmentMap);
          }

          // get SAP TD response based on GL Account value
          SAPTDResponseData sapTDResponseDataForClientGLAccount =
              getSapTDResponseData(
                  sapTDResponse, clientGlAccountValue, fullURRI, companyCode, sapDocumentNumber);
          SAPTDResponseData sapTDResponseDataForOffsetAccount =
              getSapTDResponseData(
                  sapTDResponse, offsetAccountValue, fullURRI, companyCode, sapDocumentNumber);
          SAPTDResponseData sapTDResponseDataForVATGLAccount =
              getSapTDResponseData(
                  sapTDResponse, vatGlAccountValue, fullURRI, companyCode, sapDocumentNumber);

          // not dependent to GL Account fields

          String sapTaxCode = getSapTaxCode(sapTDResponse, fullURRI, sapDocumentNumber);

          String sapReference = getSapReference(sapTDResponse, fullURRI, sapDocumentNumber);
          String sapDocumentHeaderText =
              getSapDocumentHeaderText(sapTDResponse, fullURRI, sapDocumentNumber);

          String sapPaymentCurrencyCode =
              getSapPaymentCurrencyCode(sapTDResponse, fullURRI, sapDocumentNumber);

          String sapLocalCurrencyCode =
              getSapLocalCurrencyCode(sapTDResponse, fullURRI, sapDocumentNumber);

          Double sapFxRate = getSapFxRate(sapTDResponse, fullURRI, sapDocumentNumber);

          // client GL account related fields
          Double sapFinalWhtAmount = getSapFinalWhtAmount(sapTDResponseDataForClientGLAccount);

          Double sapInvoiceAmount = getSapInvoiceAmount(sapTDResponseDataForClientGLAccount);

          String sapGLAccount = getSapGLAccount(sapTDResponseDataForClientGLAccount);

          String sapInvoiceAmountLocalCurrency =
              getSapInvoiceAmountLocalCurrency(sapTDResponseDataForClientGLAccount);

          Double withHoldingTaxBaseAmount =
              getWhtTaxBaseAmount(sapTDResponseDataForClientGLAccount);

          // WhtBaseValueCheck should be calculated only for UK territory
          Boolean whtBaseValueCheck = null;
          if (WorkbenchBatchDataSharedUtil.checkIfTerritoryIsUK(wbBatchData)) {
            whtBaseValueCheck = getWhtBaseValueCheck(withHoldingTaxBaseAmount, wbBatchData);
          }

          Double sapWhtTaxBaseAmount = getSapWhtTaxBaseAmount(sapTDResponseDataForClientGLAccount);

          Date sapPaymentDate = getSapPaymentDate(sapTDResponseDataForClientGLAccount);

          String sapAssignment = getSapAssignment(sapTDResponseDataForClientGLAccount);

          Double expectedPaymentAmount =
              getExpectedPaymentAmount(
                  wbBatchData.getExpectedWhtAmountDocumentCurrency(), sapInvoiceAmount);

          // till here

          Map<String, Object> clearingAccountingDataMap =
              retrieveClearingAccountingDataMap(
                  sapTDResponse,
                  clientGlAccountValue,
                  fullURRI,
                  companyCode,
                  wbBatchData,
                  sapDocumentNumber);
          // take clearingAccountingDocument
          String clearingAccountingDocument =
              clearingAccountingDataMap
                  .getOrDefault(
                      "clearingAccountingDocument", wbBatchData.getClearingAccountingDocument())
                  .toString();
          if (clearingAccountingDocument.isEmpty()) {
            clearingAccountingDocument = null;
          }

          // take clientGLAccountNotFound
          Set<GLAccountNotFound> glAccountNotFounds = new HashSet<>();
          GLAccountNotFound glAccountNotFound =
              GLAccountNotFound.fromDisplayName(
                  clearingAccountingDataMap
                      .getOrDefault("clientGLAccountNotFound", null)
                      .toString());
          if (glAccountNotFound != null) {
            glAccountNotFounds.add(glAccountNotFound);
          }

          if (sapTDResponseDataForOffsetAccount == null) {
            glAccountNotFounds.add(GLAccountNotFound.UNABLE_TO_FIND_OFFSET_ACCOUNT_HG);
          }

          // get Sap Detail Status
          String sapDetailStatus =
              clearingAccountingDataMap
                  .getOrDefault("sapDetailStatus", wbBatchData.getSapDetailStatus())
                  .toString();
          // get Post Approval Status
          PostApprovalStatus postApprovalStatus =
              PostApprovalStatus.valueOf(
                  clearingAccountingDataMap
                      .getOrDefault("postApprovalStatus", wbBatchData.getPostApprovalStatus())
                      .toString());

          // Offset account fields

          Double sapRoyaltiesAmount = getSapRoyaltiesAmount(sapTDResponseDataForOffsetAccount);

          String sapRoyaltiesAmountLocalCurrency =
              getSapRoyaltiesAmountLocalCurrency(sapTDResponseDataForOffsetAccount);

          // vat Gl account fields
          Double sapVatAmount = getSapVatAmount(sapTDResponseDataForVATGLAccount);

          String sapVatAmountLocalCurrency =
              getSapVatAmountLocalCurrency(sapTDResponseDataForVATGLAccount);

          //        end here

          // set values
          // GL fields
          wbBatchData.setSapFinalWHTAmount(sapFinalWhtAmount);
          wbBatchData.setSapInvoiceAmount(sapInvoiceAmount);
          wbBatchData.setSapGLAccountOrLedger(sapGLAccount);
          wbBatchData.setSapInvoiceAmountLocalCurrency(
              sapInvoiceAmountLocalCurrency != null
                  ? Double.parseDouble(sapInvoiceAmountLocalCurrency)
                  : null);
          // set checks
          // WhtBaseValueCheck should be calculated only for UK territory
          if (WorkbenchBatchDataSharedUtil.checkIfTerritoryIsUK(wbBatchData)) {
            wbBatchData.setWhtBaseValueCheck(
                WorkbenchBatchDataSharedUtil.convertToSuccessOrFailString(whtBaseValueCheck));
          }
          //
          wbBatchData.setSapWHTTaxBaseAmount(sapWhtTaxBaseAmount);

          // for PaymentDate there shouldn't set 'Error'
          wbBatchData.setSapPaymentDate(
              WorkbenchBatchDataSharedUtil.convertToSuccessOrFailString(sapPaymentDate));
          wbBatchData.setSapAssignment(sapAssignment);
          // till here

          // offset fields
          wbBatchData.setSapRoyaltiesAmount(sapRoyaltiesAmount);
          wbBatchData.setSapRoyaltiesAmountLocalCurrency(
              sapRoyaltiesAmountLocalCurrency != null
                  ? Double.parseDouble(sapRoyaltiesAmountLocalCurrency)
                  : null);

          // till here

          // vat fields
          wbBatchData.setSapVatAmount(sapVatAmount);
          wbBatchData.setSapVATAmountLocalCurrency(
              sapVatAmountLocalCurrency != null
                  ? Double.parseDouble(sapVatAmountLocalCurrency)
                  : null);
          // till here

          wbBatchData.setSapFXRate(sapFxRate);
          wbBatchData.setSapPaymentCurrencyCode(sapPaymentCurrencyCode);
          wbBatchData.setSapLocalCurrencyCode(sapLocalCurrencyCode);
          wbBatchData.setSapTaxCode(sapTaxCode);
          wbBatchData.setSapReference(sapReference);
          wbBatchData.setSapDocumentHeaderText(sapDocumentHeaderText);
          wbBatchData.setPostApprovalStatus(postApprovalStatus);
          wbBatchData.setClearingAccountingDocument(clearingAccountingDocument);
          wbBatchData.setGlAccountNotFound(glAccountNotFounds);

          // ExpectedPaymentAmount is calculated/assigned for only UK territory
          if (WorkbenchBatchDataSharedUtil.checkIfTerritoryIsUK(wbBatchData)) {
            wbBatchData.setExpectedPaymentAmount(expectedPaymentAmount);
          }

          // If the posting category of the record is not PAYOUT,
          // set the SAP detail status to POSTED and mark the document type as completed
          // for non-payouts to prevent the record from being tracked again.
          if (!wbBatchData.getPostingCategoryBasedOnPriority().equals(PostingCategory.PAYOUT)) {
            sapDetailStatus = SapDetailStatus.POSTED.getDisplayName();
            wbBatchData.setHGDocumentTypeCompletedForNonPayouts(true);
          }

          wbBatchData.setSapDetailStatus(sapDetailStatus);
        });

    workbenchBatchDataList = workbenchBatchDataRepository.saveAll(workbenchBatchDataList);

    workbenchBatchDataSharedService
        .saveWbBatchDataIntoESAndUpdateLastModifiedAndUpdateStatsForBatches(workbenchBatchDataList);
  }

  private String getSapVatAmountLocalCurrency(SAPTDResponseData sapTDResponseDataForVATGLAccount) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(sapTDResponseDataForVATGLAccount)) return null;
    return sapTDResponseDataForVATGLAccount.getAmountInCompanyCodeCurrency();
  }

  private SAPTDResponseData getSapTDResponseData(
      List<SAPTDResponseData> sapTdResponseData,
      List<String> glAccountValues,
      String fullURRI,
      String companyCode,
      String sapDocumentNumber) {
    if (sapTdResponseData == null || sapTdResponseData.isEmpty()) return null;
    return sapTdResponseData.stream()
        .filter(
            data ->
                (fullURRI == null || fullURRI.equals(data.getAccountingDocumentHeaderText()))
                    && (sapDocumentNumber.equals(data.getAccountingDocument()))
                    && (companyCode == null || companyCode.equals(data.getCompanyCode()))
                    && (glAccountValues == null || glAccountValues.contains(data.getGlAccount())))
        .findFirst()
        .orElse(null);
  }

  public Double getExpectedPaymentAmount(
      Double expectedWhtAmountDocumentCurrency, Double sapInvoiceAmount) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(
        expectedWhtAmountDocumentCurrency, sapInvoiceAmount)) return null;

    double expectedWhtAmount =
        expectedWhtAmountDocumentCurrency != null ? expectedWhtAmountDocumentCurrency : 0;
    return sapInvoiceAmount + (expectedWhtAmount);
  }

  //  if there is no matching GL account, we use this method for Payment
  private SAPTDResponseData getSapTDResponseDataForPayment(
      List<SAPTDResponseData> sapTdResponseData,
      String fullURRI,
      String companyCode,
      String sapDocumentNumber) {
    /**
     * if the SapTDResponseData is not found for the GL account, we take response based on isCleared
     * true
     */
    if (sapTdResponseData == null || sapTdResponseData.isEmpty()) return null;
    return sapTdResponseData.stream()
        .filter(
            data ->
                (fullURRI == null || fullURRI.equals(data.getAccountingDocumentHeaderText()))
                    && (sapDocumentNumber.equals(data.getAccountingDocument()))
                    && (companyCode == null || companyCode.equals(data.getCompanyCode()))
                    && (data.getIsCleared().equals(true)))
        .findFirst()
        .orElse(null);
  }

  private Map<String, Object> retrieveClearingAccountingDataMap(
      List<SAPTDResponseData> results,
      List<String> clientGlAccountValue,
      String fullURRI,
      String companyCode,
      WorkbenchBatchData workbenchBatchData,
      String sapDocumentNumber) {

    String clientGLAccountNotFound = "";

    SAPTDResponseData saptdResponseData =
        getSapTDResponseData(
            results, clientGlAccountValue, fullURRI, companyCode, sapDocumentNumber);
    PostingCategory postingCategory = workbenchBatchData.getPostingCategoryBasedOnPriority();

    // If no SAPTDResponseData found, attempt another retrieval method without using
    // clientGlAccountValue
    if (saptdResponseData == null) {
      saptdResponseData =
          getSapTDResponseDataForPayment(results, fullURRI, companyCode, sapDocumentNumber);
      if (saptdResponseData != null) {
        clientGLAccountNotFound =
            GLAccountNotFound.UNABLE_TO_FIND_CLIENT_GL_ACCOUNT_HG.getDisplayName();
      }
    }
    String sapDetailStatus = SapDetailStatus.PENDING_PAYMENT.getDisplayName();
    PostApprovalStatus postApprovalStatus = PostApprovalStatus.POSTED;
    String clearingAccountingDocument = "";

    if (saptdResponseData != null) {
      clearingAccountingDocument = saptdResponseData.getClearingAccountingDocument();
      Boolean isCleared = saptdResponseData.getIsCleared();
      if (isCleared) {
        if (postingCategory.equals(PostingCategory.PAYOUT)
            || postingCategory.equals(PostingCategory.PAY_HR)) {
          if (clearingAccountingDocument.startsWith("4")) {
            sapDetailStatus = SapDetailStatus.POSTING_CANCELLED.getDisplayName();
            postApprovalStatus = PostApprovalStatus.COMPLETE;
          } else if (!clearingAccountingDocument.startsWith("2")) {
            // not starts with 2 and 4
            sapDetailStatus = SapDetailStatus.CLEARING_ACCOUNT_NOT_RECOGNIZED.getDisplayName();
            postApprovalStatus = PostApprovalStatus.POSTING_ERROR;
          }
        }
      }
    }

    // Extracting a clearing accounting document

    return Map.of(
        "clientGLAccountNotFound",
        clientGLAccountNotFound,
        "clearingAccountingDocument",
        clearingAccountingDocument,
        "sapDetailStatus",
        sapDetailStatus,
        "postApprovalStatus",
        postApprovalStatus);
  }

  private Double getSapInvoiceAmount(SAPTDResponseData saptdResponseData) {

    if (WorkbenchBatchDataSharedUtil.areAnyNull(saptdResponseData)) return null;

    return StringUtil.parseDouble(saptdResponseData.getAmountInTransactionCurrency());
  }

  private String getSapInvoiceAmountLocalCurrency(SAPTDResponseData saptdResponseData) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(saptdResponseData)) return null;
    return saptdResponseData.getAmountInCompanyCodeCurrency();
  }

  private String getSapGLAccount(SAPTDResponseData saptdResponseData) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(saptdResponseData)) return null;

    return saptdResponseData.getGlAccount();
  }

  private Double getSapRoyaltiesAmount(SAPTDResponseData saptdResponseData) {

    if (WorkbenchBatchDataSharedUtil.areAnyNull(saptdResponseData)) return null;
    return StringUtil.parseDouble(saptdResponseData.getAmountInTransactionCurrency());
  }

  private String getSapRoyaltiesAmountLocalCurrency(SAPTDResponseData saptdResponseData) {

    if (WorkbenchBatchDataSharedUtil.areAnyNull(saptdResponseData)) return null;
    return saptdResponseData.getAmountInCompanyCodeCurrency();
  }

  private Double getSapVatAmount(SAPTDResponseData saptdResponseData) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(saptdResponseData)) return null;

    return StringUtil.parseDouble(saptdResponseData.getAmountInTransactionCurrency());
  }

  private String getSapPaymentCurrencyCode(
      List<SAPTDResponseData> results, String fullURRI, String sapDocumentNumber) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(results, fullURRI)) return null;
    SAPTDResponseData saptdResponseData =
        getSapTDResponseData(results, null, fullURRI, null, sapDocumentNumber);

    if (saptdResponseData != null) {
      return saptdResponseData.getTransactionCurrency();
    }
    return null;
  }

  private Double getWhtTaxBaseAmount(SAPTDResponseData saptdResponseData) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(saptdResponseData)) return null;
    return StringUtil.parseDouble(saptdResponseData.getWithholdingTaxBaseAmount());
  }

  private Boolean getWhtBaseValueCheck(
      Double withHoldingTaxBaseAmount, WorkbenchBatchData wbBatchData) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(withHoldingTaxBaseAmount, wbBatchData)) return null;
    /**
     * Sum of (Domestic Share doc currency) and (WithholdingTaxBaseAmount of the item belonging to
     * Client GL Account (TD API response)). If the sum value is not equal to ZERO, then flag the
     * field. 'WHT Tax Base amount loc curr' is an manual entry given by user. When user gives
     * value, we should consider this over Domestic Share doc currency. ie., Sum of (WHT Tax Base
     * amount loc curr * fx rate) and (WithholdingTaxBaseAmount) should be ZERO. If not, flag the
     * field
     */
    Double whtTaxBaseAmountLocalCurrency = wbBatchData.getWHTTaxBaseAmountLocalCurrency();
    double fxRate = WorkbenchBatchDataSharedUtil.getFxRateForCalculations(wbBatchData.getFxRate());

    double calculatedWhtBaseAmount = 0.0;

    if (whtTaxBaseAmountLocalCurrency != null) {
      calculatedWhtBaseAmount = whtTaxBaseAmountLocalCurrency * fxRate;
    } else {
      calculatedWhtBaseAmount = wbBatchData.getDomesticShareDocCurrency();
    }

    // rounding off the value to two decimals to match with the SAP value
    calculatedWhtBaseAmount =
        Double.parseDouble(
            WorkbenchBatchDataSharedUtil.formatToDecimalPlacesOrZero(calculatedWhtBaseAmount, 2));

    // withHoldingTaxBaseAmount is coming from SAP, it is already rounded off to two decimals.
    return (withHoldingTaxBaseAmount + calculatedWhtBaseAmount) == 0;
  }

  private String getSapLocalCurrencyCode(
      List<SAPTDResponseData> results, String fullURRI, String sapDocumentNumber) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(results, fullURRI)) return null;
    SAPTDResponseData saptdResponseData =
        getSapTDResponseData(results, null, fullURRI, null, sapDocumentNumber);

    if (saptdResponseData != null) {
      return saptdResponseData.getCompanyCodeCurrency();
    }
    return null;
  }

  private Double getSapFinalWhtAmount(SAPTDResponseData saptdResponseData) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(saptdResponseData)) return null;
    return StringUtil.parseDouble(saptdResponseData.getWithholdingTaxAmount());
  }

  private Double getSapWhtTaxBaseAmount(SAPTDResponseData saptdResponseData) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(saptdResponseData)) return null;
    return StringUtil.parseDouble(saptdResponseData.getWithholdingTaxBaseAmount());
  }

  private Double getSapFxRate(
      List<SAPTDResponseData> results, String fullURRI, String sapDocumentNumber) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(results, fullURRI)) return null;
    SAPTDResponseData saptdResponseData =
        getSapTDResponseData(results, null, fullURRI, null, sapDocumentNumber);

    if (saptdResponseData != null) {
      return StringUtil.parseDouble(saptdResponseData.getExchangeRate());
    }
    return null;
  }

  private String getSapTaxCode(
      List<SAPTDResponseData> results, String fullURRI, String sapDocumentNumber) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(results, fullURRI)) return null;
    SAPTDResponseData saptdResponseData =
        getSapTDResponseData(results, null, fullURRI, null, sapDocumentNumber);

    if (saptdResponseData != null) {
      return saptdResponseData.getTaxCode();
    }
    return null;
  }

  private Date getSapPaymentDate(SAPTDResponseData saptdResponseData) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(saptdResponseData)) return null;
    return StringUtil.convertDate(saptdResponseData.getClearingDate());
  }

  private String getSapReference(
      List<SAPTDResponseData> results, String fullURRI, String sapDocumentNumber) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(results, fullURRI)) return null;
    SAPTDResponseData saptdResponseData =
        getSapTDResponseData(results, null, fullURRI, null, sapDocumentNumber);

    if (saptdResponseData != null) {
      return saptdResponseData.getDocumentReferenceID();
    }
    return null;
  }

  private String getSapDocumentHeaderText(
      List<SAPTDResponseData> results, String fullURRI, String sapDocumentNumber) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(results, fullURRI)) return null;
    SAPTDResponseData saptdResponseData =
        getSapTDResponseData(results, null, fullURRI, null, sapDocumentNumber);

    if (saptdResponseData != null) {
      return saptdResponseData.getAccountingDocumentHeaderText();
    }
    return null;
  }

  private String getSapAssignment(SAPTDResponseData saptdResponseData) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(saptdResponseData)) return null;
    return saptdResponseData.getAssignmentReference();
  }
}
