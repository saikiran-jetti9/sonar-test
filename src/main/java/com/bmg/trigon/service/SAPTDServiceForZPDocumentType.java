package com.bmg.trigon.service;

import com.bmg.trigon.common.dto.FilterCriteria;
import com.bmg.trigon.common.enums.*;
import com.bmg.trigon.common.model.WorkbenchBatch;
import com.bmg.trigon.common.model.WorkbenchBatchData;
import com.bmg.trigon.common.service.AdminSettingsService;
import com.bmg.trigon.common.service.WorkbenchBatchDataSharedService;
import com.bmg.trigon.common.util.*;
import com.bmg.trigon.dto.SAPTDResponse;
import com.bmg.trigon.dto.SAPTDResponseData;
import com.bmg.trigon.repository.WorkbenchBatchDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Transactional
public class SAPTDServiceForZPDocumentType {
  private final WorkbenchBatchDataRepository workbenchBatchDataRepository;
  private final WebClientService webClientService;
  private final ObjectMapper objectMapper;
  private final AdminSettingsService adminSettingsService;
  private final WorkbenchBatchDataSharedService workbenchBatchDataSharedService;

  public SAPTDServiceForZPDocumentType(
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
  public void scheduleSAPFxRateUpdateTask() {
    updateSAPFxRateBasedOnClearingAccountDocument();
  }

  public void updateSAPFxRateBasedOnClearingAccountDocument() {
    log.info("SAP TD API scheduler started for ZP document type");

    // Define pagination parameters for retrieving clearing accounting documents
    int pageSize = 70;
    int pageNumber = 0;
    Page<String> clearingAccountingDocumentPage;

    // Retrieve distinct clearing accounting documents by post-approval status
    do {
      PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
      clearingAccountingDocumentPage =
          workbenchBatchDataRepository.findDistinctClearingAccountingDocumentsByPostApprovalStatus(
              PostApprovalStatus.POSTED, pageRequest);
      log.info(
          "clearing accounting documents found are for ZP: {}",
          clearingAccountingDocumentPage.getContent());
      pageNumber++;
      if (!clearingAccountingDocumentPage.getContent().isEmpty()) {
        // Retrieve SAP transactional data for ZP document type with clearing accounting documents
        try {
          JSONObject sapTransactionData =
              webClientService.getSAPTransactionalDataForZPDocumentType(
                  clearingAccountingDocumentPage.getContent());
          SAPTDResponse sapTDResponse =
              objectMapper.readValue(sapTransactionData.toString(), SAPTDResponse.class);

          // Define pagination parameters for batch processing of workbench batch data
          int batchSize = 200;
          int batchPage = 0;
          Page<WorkbenchBatchData> workbenchBatchDataPage;

          // Process batches of workbench batch data
          do {
            PageRequest batchPageRequest = PageRequest.of(batchPage, batchSize);
            workbenchBatchDataPage =
                workbenchBatchDataRepository
                    .findByClearingAccountingDocumentInAndPostApprovalStatus(
                        clearingAccountingDocumentPage.getContent(),
                        PostApprovalStatus.POSTED,
                        batchPageRequest);

            log.info(
                "no. of records found for clearing accounting documents are for ZP: {}",
                workbenchBatchDataPage.getContent().size());
            // process the data and update fx rate and corresponding fields
            updateSapFXRate(workbenchBatchDataPage.getContent(), sapTDResponse);
            batchPage++;
          } while (workbenchBatchDataPage.hasNext());

        } catch (JsonProcessingException e) {
          log.error("Error processing JSON data: {}", e.getMessage());
          // Handle JSON processing exception
        }
      }
    } while (clearingAccountingDocumentPage.hasNext());
  }

  private void updateSapFXRate(
      List<WorkbenchBatchData> workbenchBatchDataList, SAPTDResponse saptdResponse) {
    // stores SAP Final payment amount for corresponding Workbench batch
    Map<UUID, Map<String, Double>> workbenchBatchIdSAPAmountMap = new HashMap<>();

    // stores territory-segment pairs and corresponding client GL account values
    Map<String, List<String>> territorySegmentVsClientGlAccount = new HashMap<>();

    workbenchBatchDataList.forEach(
        workbenchBatchData -> {
          String companyCode = workbenchBatchData.getSapCompanyCode();
          PostingCategory postingCategory = workbenchBatchData.getPostingCategoryBasedOnPriority();
          String territory = workbenchBatchData.getTerritory();
          String segment = String.valueOf(workbenchBatchData.getWorkbenchCriteria().getSegment());
          WorkbenchBatch workbenchBatch = workbenchBatchData.getWorkbenchBatch();

          // SRP-1312: If the batch is an admin batch, set the segment to 'ADMIN' for processing the
          // admin settings data
          if (Boolean.TRUE.equals(workbenchBatch.getIsAdminBatch())) {
            segment = ModeOfProcessing.ADMIN.name();
          }
          String finalSegment = segment;

          // Compute if absent for territory-segment key
          territorySegmentVsClientGlAccount.computeIfAbsent(
              territory + "_" + segment,
              key -> {
                // Define filter criteria for territory and segment
                FilterCriteria territoryCriteria =
                    new FilterCriteria("territory", List.of(territory), null);
                FilterCriteria segmentCriteria =
                    new FilterCriteria("segment", List.of(finalSegment), null);

                // Get client GL account values based on territory and segment criteria
                List<String> adminClientGlAccountValue =
                    adminSettingsService.getTheColumnSettingValueByCriteria(
                        AdminSettingType.CLIENT_GL_ACCOUNT,
                        List.of(territoryCriteria, segmentCriteria),
                        AdminSettingColumnConstants.ClientGlAccountConstants
                            .CLIENT_GL_ACCOUNT_VALUE);

                // Return the list of client GL account values if available, otherwise an empty list
                return adminClientGlAccountValue != null
                    ? adminClientGlAccountValue
                    : new ArrayList<>();
              });

          // Retrieve the client GL account value for the given territory-segment key
          List<String> clientGlAccountValue =
              territorySegmentVsClientGlAccount.get(territory + "_" + segment);

          String clearingAccountDocument = workbenchBatchData.getClearingAccountingDocument();

          SAPTDResponseData saptdResponseData =
              getSapTDResponseDataForFXRate(
                  saptdResponse, companyCode, clientGlAccountValue, clearingAccountDocument);

          if (saptdResponseData == null) {
            saptdResponseData =
                getSapTDResponseDataForFXRate(
                    saptdResponse, companyCode, null, clearingAccountDocument);
            if (saptdResponseData != null) {
              // only adding a 'GL account not found' if we get the response without a GL account
              Set<GLAccountNotFound> glAccountNotFounds = workbenchBatchData.getGlAccountNotFound();
              glAccountNotFounds.add(GLAccountNotFound.UNABLE_TO_FIND_CLIENT_GL_ACCOUNT_ZP);
              workbenchBatchData.setGlAccountNotFound(glAccountNotFounds);
            }
          }

          // sapFinalPaymentAmount not dependent on sapFXRate
          Double sapFinalPaymentAmount =
              getSapFinalPaymentAmount(
                  workbenchBatchData.getSapInvoiceAmount(),
                  workbenchBatchData.getSapFinalWHTAmount());
          if (saptdResponseData != null) {

            // apply fx rate to filed based on the currency codes of local and payment
            Boolean requiresFxRate =
                !(workbenchBatchData
                    .getSapLocalCurrencyCode()
                    .equalsIgnoreCase(workbenchBatchData.getSapPaymentCurrencyCode()));

            // fxRate from HG document type
            Double sapFxRate =
                workbenchBatchData.getSapFXRate() != null ? workbenchBatchData.getSapFXRate() : 0;

            // sapFxRate dependent fields
            Double sapPaymentAmountLocalCurrency =
                getSapPaymentAmountLocalCurrency(requiresFxRate, sapFinalPaymentAmount, sapFxRate);

            // fxRate from ZP or ZV document type
            Double sapFXRateOfClearingDocument =
                (saptdResponseData.getExchangeRate() != null
                        && !saptdResponseData.getExchangeRate().isEmpty())
                    ? Double.parseDouble(saptdResponseData.getExchangeRate())
                    : 0.0;

            // sapFXRateOfClearingDocument dependent fields
            Double sapFinalWHTAmountLocalCurrency =
                getSapFinalWhtAmountLocalCurrency(
                    requiresFxRate,
                    workbenchBatchData.getSapFinalWHTAmount(),
                    sapFXRateOfClearingDocument);
            Double sapWHTTaxBaseAmountLocalCurrency =
                getSapWhtTaxBaseAmountLocalCurrency(
                    workbenchBatchData.getSapWHTTaxBaseAmount(), sapFXRateOfClearingDocument);

            Double expectedWHTAmountDocumentCurrency =
                (workbenchBatchData.getExpectedWhtAmountDocumentCurrency() != null
                    ? workbenchBatchData.getExpectedWhtAmountDocumentCurrency()
                    : 0.0);

            Boolean expectedVsActualWHTValueCheck =
                getExpectedVsActualWhtValueCheck(
                    expectedWHTAmountDocumentCurrency, workbenchBatchData.getSapFinalWHTAmount());
            Map<String, Object> sapDetailStatusAndPostApprovalStatus =
                getSapDetailStatusAndPostApprovalStatus(
                    expectedVsActualWHTValueCheck,
                    postingCategory,
                    clearingAccountDocument,
                    workbenchBatchData);

            String sapDetailStatus =
                sapDetailStatusAndPostApprovalStatus
                    .getOrDefault("sapDetailStatus", workbenchBatchData.getSapDetailStatus())
                    .toString();
            PostApprovalStatus postApprovalStatus =
                PostApprovalStatus.valueOf(
                    sapDetailStatusAndPostApprovalStatus
                        .getOrDefault(
                            "postApprovalStatus", workbenchBatchData.getPostApprovalStatus())
                        .toString());

            if (sapDetailStatus.equalsIgnoreCase(SapDetailStatus.PAID.getDisplayName())
                || sapDetailStatus.equalsIgnoreCase(
                    SapDetailStatus.ERROR_FROM_WHT_AMOUNT_CHECK.getDisplayName())) {
              // only set values after paid.
              workbenchBatchData.setSapFXRateOfClearingDocument(sapFXRateOfClearingDocument);
              workbenchBatchData.setSapFinalPaymentAmount(sapFinalPaymentAmount);
              workbenchBatchData.setSapPaymentAmountLocalCurrency(sapPaymentAmountLocalCurrency);
              workbenchBatchData.setSapFinalWHTAmountLocalCurrency(sapFinalWHTAmountLocalCurrency);
              workbenchBatchData.setSapWHTTaxBaseAmountLocalCurrency(
                  sapWHTTaxBaseAmountLocalCurrency);

              // ExpectedVsActualWHTValueCheck is calculated/assigned for only UK territory
              if (WorkbenchBatchDataSharedUtil.checkIfTerritoryIsUK(workbenchBatchData)) {
                workbenchBatchData.setExpectedVsActualWHTValueCheck(
                    WorkbenchBatchDataSharedUtil.convertToSuccessOrFailString(
                        expectedVsActualWHTValueCheck));
              }

              updateSAPTotalAmount(workbenchBatchData, workbenchBatchIdSAPAmountMap);
            }
            log.info(
                "Sap FX Rate of clearing document is {} for Trigon id {}",
                sapFXRateOfClearingDocument,
                workbenchBatchData.getId());
            workbenchBatchData.setSapDetailStatus(sapDetailStatus);
            workbenchBatchData.setPostApprovalStatus(postApprovalStatus);
          } else {
            // we didn't find any matching record even with both ZP or ZV document type
            Set<GLAccountNotFound> glAccountNotFounds = workbenchBatchData.getGlAccountNotFound();
            glAccountNotFounds.add(GLAccountNotFound.UNABLE_TO_FIND_PAYMENT_DOCUMENT_ZP_OR_ZV);

            workbenchBatchData.setGlAccountNotFound(glAccountNotFounds);
            workbenchBatchData.setSapFinalPaymentAmount(sapFinalPaymentAmount);
            updateSAPTotalAmount(workbenchBatchData, workbenchBatchIdSAPAmountMap);

            // by default checks will be true
            Boolean expectedVsActualWHTValueCheck = true;

            Map<String, Object> sapDetailStatusAndPostApprovalStatus =
                getSapDetailStatusAndPostApprovalStatus(
                    expectedVsActualWHTValueCheck,
                    postingCategory,
                    clearingAccountDocument,
                    workbenchBatchData);

            String sapDetailStatus =
                sapDetailStatusAndPostApprovalStatus
                    .getOrDefault("sapDetailStatus", workbenchBatchData.getSapDetailStatus())
                    .toString();
            PostApprovalStatus postApprovalStatus =
                PostApprovalStatus.valueOf(
                    sapDetailStatusAndPostApprovalStatus
                        .getOrDefault(
                            "postApprovalStatus", workbenchBatchData.getPostApprovalStatus())
                        .toString());
            workbenchBatchData.setSapDetailStatus(sapDetailStatus);
            workbenchBatchData.setPostApprovalStatus(postApprovalStatus);

            // setting null for the fields which are related to SAPFXRate

            workbenchBatchData.setSapFXRateOfClearingDocument(null);
            workbenchBatchData.setSapPaymentAmountLocalCurrency(null);
            workbenchBatchData.setSapFinalWHTAmountLocalCurrency(null);
            workbenchBatchData.setSapWHTTaxBaseAmountLocalCurrency(null);

            // ExpectedVsActualWHTValueCheck is calculated/assigned for only UK territory
            if (WorkbenchBatchDataSharedUtil.checkIfTerritoryIsUK(workbenchBatchData)) {
              // by default, check fields are success
              workbenchBatchData.setExpectedVsActualWHTValueCheck(
                  WorkbenchBatchDataSharedUtil.convertToSuccessOrFailString(
                      expectedVsActualWHTValueCheck));
            }

            log.info(
                "Sap FX Rate of clearing document is {} for Trigon id {}",
                null,
                workbenchBatchData.getId());
          }
        });

    workbenchBatchDataList = workbenchBatchDataRepository.saveAll(workbenchBatchDataList);

    workbenchBatchDataSharedService.saveSAPTotalAmount(workbenchBatchIdSAPAmountMap);

    workbenchBatchDataSharedService
        .saveWbBatchDataIntoESAndUpdateLastModifiedAndUpdateStatsForBatches(workbenchBatchDataList);

    log.info(
        "The number of records processed for SAP FX Rate calculation for ZP document type {}",
        workbenchBatchDataList.size());
  }

  private SAPTDResponseData getSapTDResponseDataForFXRate(
      SAPTDResponse saptdResponse,
      String companyCode,
      List<String> clientGlAccountValue,
      String clearingAccountDocument) {
    // clearing_accounting_document_company_code vs List <SAP TD Response data>
    return saptdResponse.getResults().stream()
        .filter(
            data ->
                (companyCode == null || companyCode.equals(data.getCompanyCode()))
                    && (clientGlAccountValue == null
                        || clientGlAccountValue.contains(data.getGlAccount()))
                    && clearingAccountDocument.equals(data.getClearingAccountingDocument()))
        .findFirst()
        .orElse(null);
  }

  private void updateSAPTotalAmount(
      WorkbenchBatchData wbBatchData, Map<UUID, Map<String, Double>> workbenchBatchIdSAPAmountMap) {
    WorkbenchBatch workbenchBatch = wbBatchData.getWorkbenchBatch();

    UUID batchId = workbenchBatch.getId();
    Map<String, Double> currencyVsSAPAmount =
        workbenchBatchIdSAPAmountMap.computeIfAbsent(batchId, k -> new HashMap<>());

    String currencyCode = wbBatchData.getSapPaymentCurrencyCode();
    Double finalPaymentAmount = wbBatchData.getSapFinalPaymentAmount();

    Double totalAmountSAP =
        currencyVsSAPAmount.getOrDefault(currencyCode, 0.0) + finalPaymentAmount;
    currencyVsSAPAmount.put(currencyCode, totalAmountSAP);
  }

  private Double getSapFinalWhtAmountLocalCurrency(
      Boolean requiresFXRate, Double sapFinalWhtAmount, Double sapFxRate) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(requiresFXRate, sapFinalWhtAmount, sapFxRate))
      return null;
    if (!requiresFXRate) {
      return sapFinalWhtAmount;
    } else {
      return sapFinalWhtAmount * sapFxRate;
    }
  }

  private Double getSapPaymentAmountLocalCurrency(
      Boolean requiresFXRate, Double sapFinalPaymentAmount, Double sapFxRate) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(requiresFXRate, sapFinalPaymentAmount, sapFxRate))
      return null;
    if (!requiresFXRate) {
      return sapFinalPaymentAmount;
    } else {
      return sapFinalPaymentAmount * sapFxRate;
    }
  }

  private Double getSapFinalPaymentAmount(Double sapInvoiceAmount, Double sapFinalWhtAmount) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(sapFinalWhtAmount, sapInvoiceAmount)) return null;
    return sapInvoiceAmount - sapFinalWhtAmount;
  }

  private Map<String, Object> getSapDetailStatusAndPostApprovalStatus(
      Boolean expectedVsActualWhtValueCheck,
      PostingCategory postingCategory,
      String clearingAccountingDocument,
      WorkbenchBatchData workbenchBatchData) {

    // If territory is not UK then ExpectedVsActualWhtValueCheck should be true
    // so that it will not impact the sapDetailStatus
    if (!WorkbenchBatchDataSharedUtil.checkIfTerritoryIsUK(workbenchBatchData)) {
      expectedVsActualWhtValueCheck = true;
    }

    Map<String, Object> statusMap;
    String sapDetailStatus = SapDetailStatus.PENDING_PAYMENT.getDisplayName();
    PostApprovalStatus postApprovalStatus = PostApprovalStatus.POSTED;

    if (postingCategory.equals(PostingCategory.PAYOUT)
        || postingCategory.equals(PostingCategory.PAY_HR)) {
      if (clearingAccountingDocument.startsWith("2")) {
        sapDetailStatus =
            expectedVsActualWhtValueCheck
                ? SapDetailStatus.PAID.getDisplayName()
                : SapDetailStatus.ERROR_FROM_WHT_AMOUNT_CHECK.getDisplayName();

        postApprovalStatus = PostApprovalStatus.COMPLETE;
      } else if (clearingAccountingDocument.startsWith("4")) {
        sapDetailStatus = SapDetailStatus.POSTING_CANCELLED.getDisplayName();
        postApprovalStatus = PostApprovalStatus.COMPLETE;
      } else {
        sapDetailStatus = SapDetailStatus.CLEARING_ACCOUNT_NOT_RECOGNIZED.getDisplayName();
        postApprovalStatus = PostApprovalStatus.POSTING_ERROR;
      }
    }
    statusMap =
        Map.of("sapDetailStatus", sapDetailStatus, "postApprovalStatus", postApprovalStatus);
    return statusMap;
  }

  private Boolean getExpectedVsActualWhtValueCheck(
      Double expectedWhtAmountDocumentCurrency, Double sapFinalWhtAmount) {
    sapFinalWhtAmount = sapFinalWhtAmount != null ? sapFinalWhtAmount : 0;
    /**
     * Sum (Expected WHT amount document currency) and (SAP final WHT amount from SAP TD). If the
     * sum is not equal to ZERO, then flag the field. This should be populated only after payment
     * done.
     */
    Double expectedWhtAmount =
        expectedWhtAmountDocumentCurrency != null ? expectedWhtAmountDocumentCurrency : 0;

    // rounding off the value to two decimals to match with the SAP value
    expectedWhtAmount =
        Double.valueOf(
            WorkbenchBatchDataSharedUtil.formatToDecimalPlacesOrZero(expectedWhtAmount, 2));
    // sapFinalWhtAmount is coming from SAP, it is already rounded off to two decimals
    return (expectedWhtAmount + sapFinalWhtAmount) == 0;
  }

  private Double getSapWhtTaxBaseAmountLocalCurrency(Double sapWhtTaxBaseAmount, Double sapFxRate) {
    if (WorkbenchBatchDataSharedUtil.areAnyNull(sapWhtTaxBaseAmount, sapFxRate)) return null;
    return sapWhtTaxBaseAmount * sapFxRate;
  }
}
