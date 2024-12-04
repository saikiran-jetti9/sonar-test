package com.bmg.trigon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.bmg.trigon.common.enums.*;
import com.bmg.trigon.common.model.WorkbenchBatch;
import com.bmg.trigon.common.model.WorkbenchBatchData;
import com.bmg.trigon.common.model.WorkbenchCriteria;
import com.bmg.trigon.common.service.AdminSettingsService;
import com.bmg.trigon.common.service.WorkbenchBatchDataSharedService;
import com.bmg.trigon.common.util.ApplicationConstantsShared;
import com.bmg.trigon.dto.SAPTDResponse;
import com.bmg.trigon.dto.SAPTDResponseData;
import com.bmg.trigon.repository.WorkbenchBatchDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
public class SAPTDServiceForZPDocumentTypeTest {

  @Mock private WorkbenchBatchDataRepository workbenchBatchDataRepository;
  @Mock private WebClientService webClientService;
  @Mock private ObjectMapper objectMapper;
  @Mock private AdminSettingsService adminSettingsService;
  @Mock private WorkbenchBatchData workbenchBatchData;
  @Mock private WorkbenchBatchDataSharedService workbenchBatchDataSharedService;

  @InjectMocks private SAPTDServiceForZPDocumentType saptdServiceForZPDocumentType;

  @Test
  void testUpdateSAPFxRateBasedOnClearingAccountDocument() throws JsonProcessingException {

    List<String> pageContent = Arrays.asList("298877", "123912", "874932", "981233");
    when(workbenchBatchDataRepository.findDistinctClearingAccountingDocumentsByPostApprovalStatus(
            any(), any()))
        .thenReturn(new PageImpl<>(pageContent, PageRequest.of(0, 70), 4));

    when(webClientService.getSAPTransactionalDataForZPDocumentType(any()))
        .thenReturn(new JSONObject());

    List<SAPTDResponseData> saptdResponseDataList = new ArrayList<>();
    saptdResponseDataList.add(
        SAPTDResponseData.builder()
            .companyCode("1234")
            .glAccount("9876")
            .exchangeRate("20")
            .build());
    SAPTDResponse saptdResponse =
        SAPTDResponse.builder().__count("1").results(saptdResponseDataList).build();
    when(objectMapper.readValue(new JSONObject().toString(), SAPTDResponse.class))
        .thenReturn(saptdResponse);

    List<WorkbenchBatchData> workbenchBatchDataList = new ArrayList<>();
    WorkbenchBatch workbenchBatch =
        WorkbenchBatch.builder()
            .id(UUID.fromString("7f000001-8dfa-1d20-818d-fa6810de0002"))
            .build();
    workbenchBatchDataList.add(
        WorkbenchBatchData.builder()
            .sapCompanyCode("1234")
            .workbenchBatch(workbenchBatch)
            .finalCategory(PostingCategory.PAY_HR)
            .territory("UK")
            .workbenchCriteria(WorkbenchCriteria.builder().segment(Segment.PUBLISHING).build())
            .sapInvoiceAmount(14D)
            .sapFinalWHTAmount(12D)
            .sapWHTTaxBaseAmount(50D)
            .expectedWhtAmountDocumentCurrency(70D)
            .sapLocalCurrencyCode("US")
            .sapPaymentCurrencyCode("US")
            .sapFXRate(2D)
            .sapPaymentCurrencyCode("ABS")
            .sapDetailStatus("Status")
            .postApprovalStatus(PostApprovalStatus.APPROVED)
            .clearingAccountingDocument("298877")
            .build());

    when(workbenchBatchDataRepository.findByClearingAccountingDocumentInAndPostApprovalStatus(
            any(), any(), any()))
        .thenReturn(new PageImpl<>(workbenchBatchDataList, PageRequest.of(0, 200), 1));

    saptdServiceForZPDocumentType.updateSAPFxRateBasedOnClearingAccountDocument();

    WorkbenchBatchData updatedData = workbenchBatchDataList.get(0);
    assertEquals(2D, updatedData.getSapFXRate());

    verify(workbenchBatchDataRepository, times(1)).saveAll(any());
    verify(workbenchBatchDataSharedService, times(1)).saveSAPTotalAmount(anyMap());
    verify(workbenchBatchDataSharedService, times(1))
        .saveWbBatchDataIntoESAndUpdateLastModifiedAndUpdateStatsForBatches(any());
  }

  @Disabled
  @Test
  void testUpdateSAPFxRate()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "updateSapFXRate", List.class, SAPTDResponse.class);
    method.setAccessible(true);

    when(adminSettingsService.getTheColumnSettingValueByCriteria(any(), any(), any()))
        .thenReturn(new ArrayList<>());

    List<WorkbenchBatchData> workbenchBatchDataList = new ArrayList<>();

    WorkbenchBatch workbenchBatch =
        WorkbenchBatch.builder()
            .id(UUID.fromString("7f000001-8dfa-1d20-818d-fa6810de0002"))
            .build();
    workbenchBatchDataList.add(
        WorkbenchBatchData.builder()
            .sapCompanyCode("1234")
            .workbenchBatch(workbenchBatch)
            .finalCategory(PostingCategory.PAY_HR)
            .territory("UK")
            .workbenchCriteria(WorkbenchCriteria.builder().segment(Segment.PUBLISHING).build())
            .sapInvoiceAmount(14D)
            .sapFinalWHTAmount(12D)
            .sapWHTTaxBaseAmount(50D)
            .expectedWhtAmountDocumentCurrency(70D)
            .sapLocalCurrencyCode("US")
            .sapPaymentCurrencyCode("US")
            .sapFXRate(2D)
            .sapPaymentCurrencyCode("ABS")
            .sapDetailStatus("Status")
            .postApprovalStatus(PostApprovalStatus.APPROVED)
            .clearingAccountingDocument("298877")
            .build());
    List<SAPTDResponseData> saptdResponseDataList = new ArrayList<>();
    saptdResponseDataList.add(
        SAPTDResponseData.builder()
            .companyCode("1234")
            .glAccount("9876")
            .exchangeRate("20")
            .build());

    SAPTDResponse saptdResponse =
        SAPTDResponse.builder().__count("1").results(saptdResponseDataList).build();
    method.invoke(saptdServiceForZPDocumentType, workbenchBatchDataList, saptdResponse);

    assertEquals(20.0, workbenchBatchDataList.get(0).getSapFXRateOfClearingDocument());
    assertEquals(2.0, workbenchBatchDataList.get(0).getSapFinalPaymentAmount());
    assertEquals(4.0, workbenchBatchDataList.get(0).getSapPaymentAmountLocalCurrency());
    assertEquals(240.0, workbenchBatchDataList.get(0).getSapFinalWHTAmountLocalCurrency());
    assertEquals(1000.0, workbenchBatchDataList.get(0).getSapWHTTaxBaseAmountLocalCurrency());
    assertEquals(
        ApplicationConstantsShared.FAIL,
        workbenchBatchDataList.get(0).getExpectedVsActualWHTValueCheck());
    assertEquals(
        SapDetailStatus.ERROR_FROM_WHT_AMOUNT_CHECK.getDisplayName(),
        workbenchBatchDataList.get(0).getSapDetailStatus());
    assertEquals(
        PostApprovalStatus.COMPLETE, workbenchBatchDataList.get(0).getPostApprovalStatus());
    assertEquals(
        workbenchBatchDataList.get(0).getGlAccountNotFound(),
        new HashSet<>(
            Collections.singleton(GLAccountNotFound.UNABLE_TO_FIND_CLIENT_GL_ACCOUNT_ZP)));

    verify(workbenchBatchDataRepository, times(1)).saveAll(any());
    verify(workbenchBatchDataSharedService, times(1)).saveSAPTotalAmount(anyMap());
    verify(workbenchBatchDataSharedService, times(1))
        .saveWbBatchDataIntoESAndUpdateLastModifiedAndUpdateStatsForBatches(any());
  }

  @Test
  void testUpdateSAPFxRateWhenSAPTDResponseDataIsNull()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "updateSapFXRate", List.class, SAPTDResponse.class);
    method.setAccessible(true);

    when(adminSettingsService.getTheColumnSettingValueByCriteria(any(), any(), any()))
        .thenReturn(new ArrayList<>());

    List<WorkbenchBatchData> workbenchBatchDataList = new ArrayList<>();

    WorkbenchBatch workbenchBatch =
        WorkbenchBatch.builder()
            .id(UUID.fromString("7f000001-8dfa-1d20-818d-fa6810de0002"))
            .build();
    workbenchBatchDataList.add(
        WorkbenchBatchData.builder()
            .sapCompanyCode("1234")
            .workbenchBatch(workbenchBatch)
            .finalCategory(PostingCategory.PAY_HR)
            .territory("UK")
            .workbenchCriteria(WorkbenchCriteria.builder().segment(Segment.PUBLISHING).build())
            .sapInvoiceAmount(14D)
            .sapFinalWHTAmount(12D)
            .sapPaymentCurrencyCode("ABS")
            .sapDetailStatus("Status")
            .postApprovalStatus(PostApprovalStatus.APPROVED)
            .clearingAccountingDocument("998877")
            .build());
    List<SAPTDResponseData> saptdResponseDataList = new ArrayList<>();
    saptdResponseDataList.add(
        SAPTDResponseData.builder().companyCode("2345").glAccount("9876").build());

    SAPTDResponse saptdResponse =
        SAPTDResponse.builder().__count("1").results(saptdResponseDataList).build();
    method.invoke(saptdServiceForZPDocumentType, workbenchBatchDataList, saptdResponse);

    assertEquals(2.0, workbenchBatchDataList.get(0).getSapFinalPaymentAmount());
    assertEquals(
        SapDetailStatus.CLEARING_ACCOUNT_NOT_RECOGNIZED.getDisplayName(),
        workbenchBatchDataList.get(0).getSapDetailStatus());
    assertEquals(
        PostApprovalStatus.POSTING_ERROR, workbenchBatchDataList.get(0).getPostApprovalStatus());
    assertEquals(
        ApplicationConstantsShared.SUCCESS,
        workbenchBatchDataList.get(0).getExpectedVsActualWHTValueCheck());

    assertEquals(
        workbenchBatchDataList.get(0).getGlAccountNotFound(),
        new HashSet<>(
            Collections.singleton(GLAccountNotFound.UNABLE_TO_FIND_PAYMENT_DOCUMENT_ZP_OR_ZV)));

    verify(workbenchBatchDataRepository, times(1)).saveAll(any());
    verify(workbenchBatchDataSharedService, times(1)).saveSAPTotalAmount(anyMap());
    verify(workbenchBatchDataSharedService, times(1))
        .saveWbBatchDataIntoESAndUpdateLastModifiedAndUpdateStatsForBatches(any());
  }

  @Test
  void testUpdateSAPFxRateWhenWorkbenchBatchDataIsEmpty()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "updateSapFXRate", List.class, SAPTDResponse.class);
    method.setAccessible(true);

    List<WorkbenchBatchData> workbenchBatchDataList = new ArrayList<>();

    List<SAPTDResponseData> saptdResponseDataList = new ArrayList<>();
    saptdResponseDataList.add(
        SAPTDResponseData.builder().companyCode("2345").glAccount("9876").build());

    SAPTDResponse saptdResponse =
        SAPTDResponse.builder().__count("1").results(saptdResponseDataList).build();
    method.invoke(saptdServiceForZPDocumentType, workbenchBatchDataList, saptdResponse);

    // checking that saveAll is called with an empty list as we are passing empty list
    ArgumentCaptor<List<WorkbenchBatchData>> captor = ArgumentCaptor.forClass(List.class);
    verify(workbenchBatchDataRepository, times(1)).saveAll(captor.capture());
    List<WorkbenchBatchData> savedData = captor.getValue();

    assertTrue(savedData.isEmpty());

    verify(workbenchBatchDataRepository, times(1)).saveAll(any());
    verify(workbenchBatchDataSharedService, times(1)).saveSAPTotalAmount(anyMap());
    verify(workbenchBatchDataSharedService, times(1))
        .saveWbBatchDataIntoESAndUpdateLastModifiedAndUpdateStatsForBatches(any());
  }

  @Test
  void testUpdateSAPFxRateWhenWorkbenchBatchDataIsNull() throws NoSuchMethodException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "updateSapFXRate", List.class, SAPTDResponse.class);
    method.setAccessible(true);

    List<WorkbenchBatchData> workbenchBatchDataList = null;

    List<SAPTDResponseData> saptdResponseDataList = new ArrayList<>();
    saptdResponseDataList.add(
        SAPTDResponseData.builder().companyCode("2345").glAccount("9876").build());

    SAPTDResponse saptdResponse =
        SAPTDResponse.builder().__count("1").results(saptdResponseDataList).build();
    assertThrows(
        InvocationTargetException.class,
        () -> method.invoke(saptdServiceForZPDocumentType, workbenchBatchDataList, saptdResponse));
  }

  @Test
  void testUpdateSAPFxRateWhenSAPTDResponseIsNull() throws NoSuchMethodException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "updateSapFXRate", List.class, SAPTDResponse.class);
    method.setAccessible(true);

    when(adminSettingsService.getTheColumnSettingValueByCriteria(any(), any(), any()))
        .thenReturn(new ArrayList<>());

    List<WorkbenchBatchData> workbenchBatchDataList = new ArrayList<>();

    WorkbenchBatch workbenchBatch =
        WorkbenchBatch.builder()
            .id(UUID.fromString("7f000001-8dfa-1d20-818d-fa6810de0002"))
            .build();
    workbenchBatchDataList.add(
        WorkbenchBatchData.builder()
            .sapCompanyCode("1234")
            .workbenchBatch(workbenchBatch)
            .finalCategory(PostingCategory.PAY_HR)
            .territory("UK")
            .workbenchCriteria(WorkbenchCriteria.builder().segment(Segment.PUBLISHING).build())
            .sapInvoiceAmount(14D)
            .sapFinalWHTAmount(12D)
            .sapPaymentCurrencyCode("ABS")
            .sapDetailStatus("Status")
            .postApprovalStatus(PostApprovalStatus.APPROVED)
            .clearingAccountingDocument("998877")
            .build());

    SAPTDResponse saptdResponse = null;
    assertThrows(
        InvocationTargetException.class,
        () -> method.invoke(saptdServiceForZPDocumentType, workbenchBatchDataList, saptdResponse));
  }

  @Test
  void testGetSapWhtTaxBaseAmountLocalCurrency()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapWhtTaxBaseAmountLocalCurrency", Double.class, Double.class);
    method.setAccessible(true);

    Double sapWhtTaxBaseAmount = 10.1D;
    Double sapFxRate = 2.0D;

    Double sapWhtTaxBaseAmountLocalCurrency =
        (Double) method.invoke(saptdServiceForZPDocumentType, sapWhtTaxBaseAmount, sapFxRate);
    assertEquals(20.2D, sapWhtTaxBaseAmountLocalCurrency);
  }

  @Test
  void testGetSapWhtTaxBaseAmountLocalCurrencyWhenSapWhtTaxBaseAmount()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapWhtTaxBaseAmountLocalCurrency", Double.class, Double.class);
    method.setAccessible(true);

    Double sapWhtTaxBaseAmount = null;
    Double sapFxRate = 2.0D;

    Double sapWhtTaxBaseAmountLocalCurrency =
        (Double) method.invoke(saptdServiceForZPDocumentType, sapWhtTaxBaseAmount, sapFxRate);
    assertNull(sapWhtTaxBaseAmountLocalCurrency);
  }

  @Test
  void testGetExpectedVsActualWhtValueCheck()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getExpectedVsActualWhtValueCheck", Double.class, Double.class);
    method.setAccessible(true);

    Double expectedWhtAmountDocumentCurrency = 12.2567D;
    Double sapFinalWhtAmount = 10.34D;

    assertFalse(
        (Boolean)
            method.invoke(
                saptdServiceForZPDocumentType,
                expectedWhtAmountDocumentCurrency,
                sapFinalWhtAmount));
  }

  @Test
  void testGetExpectedVsActualWhtValueCheckWhenSumIsZero()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getExpectedVsActualWhtValueCheck", Double.class, Double.class);
    method.setAccessible(true);

    Double expectedWhtAmountDocumentCurrency = null;
    Double sapFinalWhtAmount = null;

    assertTrue(
        (Boolean)
            method.invoke(
                saptdServiceForZPDocumentType,
                expectedWhtAmountDocumentCurrency,
                sapFinalWhtAmount));
  }

  @Test
  void testGetSapDetailStatusAndPostApprovalStatus()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapDetailStatusAndPostApprovalStatus",
            Boolean.class,
            PostingCategory.class,
            String.class,
            WorkbenchBatchData.class);
    method.setAccessible(true);

    Boolean expectedVsActualWhtValueCheck = true;
    PostingCategory postingCategory = PostingCategory.PAY_HR;
    String clearingAccountingDocument = "2456";
    WorkbenchBatchData wbBatchData =
        WorkbenchBatchData.builder().territory(Territory.UK.name()).build();

    Map<String, Object> statusMap =
        (Map<String, Object>)
            method.invoke(
                saptdServiceForZPDocumentType,
                expectedVsActualWhtValueCheck,
                postingCategory,
                clearingAccountingDocument,
                wbBatchData);
    assertEquals(SapDetailStatus.PAID.getDisplayName(), statusMap.get("sapDetailStatus"));
    assertEquals(PostApprovalStatus.COMPLETE, statusMap.get("postApprovalStatus"));
  }

  @Test
  void testGetSapDetailStatusAndPostApprovalStatusWhenPostingCategoryIsNotPayoutAndPayHr()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapDetailStatusAndPostApprovalStatus",
            Boolean.class,
            PostingCategory.class,
            String.class,
            WorkbenchBatchData.class);
    method.setAccessible(true);

    Boolean expectedVsActualWhtValueCheck = true;
    PostingCategory postingCategory = PostingCategory.HOLD_CODE;
    String clearingAccountingDocument = "2456";
    WorkbenchBatchData wbBatchData =
        WorkbenchBatchData.builder().territory(Territory.UK.name()).build();

    Map<String, Object> statusMap =
        (Map<String, Object>)
            method.invoke(
                saptdServiceForZPDocumentType,
                expectedVsActualWhtValueCheck,
                postingCategory,
                clearingAccountingDocument,
                wbBatchData);
    assertEquals(
        SapDetailStatus.PENDING_PAYMENT.getDisplayName(), statusMap.get("sapDetailStatus"));
    assertEquals(PostApprovalStatus.POSTED, statusMap.get("postApprovalStatus"));
  }

  @Test
  void testGetSapDetailStatusAndPostApprovalStatusWhenExpectedVsActualWhtValueCheckIsNull()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapDetailStatusAndPostApprovalStatus",
            Boolean.class,
            PostingCategory.class,
            String.class,
            WorkbenchBatchData.class);
    method.setAccessible(true);

    Boolean expectedVsActualWhtValueCheck = null;
    PostingCategory postingCategory = PostingCategory.PAY_HR;
    String clearingAccountingDocument = "2456";
    WorkbenchBatchData wbBatchData =
        WorkbenchBatchData.builder().territory(Territory.UK.name()).build();

    assertThrows(
        InvocationTargetException.class,
        () ->
            method.invoke(
                saptdServiceForZPDocumentType,
                expectedVsActualWhtValueCheck,
                postingCategory,
                clearingAccountingDocument,
                wbBatchData));
  }

  @Test
  void testGetSapDetailStatusAndPostApprovalStatusWhenPostingCategoryIsNull()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapDetailStatusAndPostApprovalStatus",
            Boolean.class,
            PostingCategory.class,
            String.class,
            WorkbenchBatchData.class);
    method.setAccessible(true);

    Boolean expectedVsActualWhtValueCheck = true;
    PostingCategory postingCategory = null;
    String clearingAccountingDocument = "2456";
    WorkbenchBatchData wbBatchData =
        WorkbenchBatchData.builder().territory(Territory.UK.name()).build();

    assertThrows(
        InvocationTargetException.class,
        () ->
            method.invoke(
                saptdServiceForZPDocumentType,
                expectedVsActualWhtValueCheck,
                postingCategory,
                clearingAccountingDocument,
                wbBatchData));
  }

  @Test
  void testGetSapDetailStatusAndPostApprovalStatusWhenClearingAccountingDocumentIsNull()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapDetailStatusAndPostApprovalStatus",
            Boolean.class,
            PostingCategory.class,
            String.class,
            WorkbenchBatchData.class);
    method.setAccessible(true);

    Boolean expectedVsActualWhtValueCheck = false;
    PostingCategory postingCategory = PostingCategory.PAY_HR;
    String clearingAccountingDocument = null;
    WorkbenchBatchData wbBatchData =
        WorkbenchBatchData.builder().territory(Territory.UK.name()).build();

    assertThrows(
        InvocationTargetException.class,
        () ->
            method.invoke(
                saptdServiceForZPDocumentType,
                expectedVsActualWhtValueCheck,
                postingCategory,
                clearingAccountingDocument,
                wbBatchData));
  }

  @Test
  void testGetSapFinalPaymentAmount()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapFinalPaymentAmount", Double.class, Double.class);
    method.setAccessible(true);

    Double sapInvoiceAmount = 10.2456D;
    Double sapFinalWhtAmount = 5.34D;

    Double sapFinalPaymentAmount =
        (Double) method.invoke(saptdServiceForZPDocumentType, sapInvoiceAmount, sapFinalWhtAmount);
    assertEquals(4.9056D, sapFinalPaymentAmount);
  }

  @Test
  void testGetSapFinalPaymentAmountWhenSapInvoiceAmountIsNull()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapFinalPaymentAmount", Double.class, Double.class);
    method.setAccessible(true);

    Double sapInvoiceAmount = null;
    Double sapFinalWhtAmount = 5.34D;

    Double sapFinalPaymentAmount =
        (Double) method.invoke(saptdServiceForZPDocumentType, sapInvoiceAmount, sapFinalWhtAmount);
    assertNull(sapFinalPaymentAmount);
  }

  @Test
  void testGetSapPaymentAmountLocalCurrency()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapPaymentAmountLocalCurrency", Boolean.class, Double.class, Double.class);
    method.setAccessible(true);

    Boolean requiresFXRate = true;
    Double sapFinalPaymentAmount = 10D;
    Double sapFxRate = 2D;

    Double sapPaymentAmountLocalCurrency =
        (Double)
            method.invoke(
                saptdServiceForZPDocumentType, requiresFXRate, sapFinalPaymentAmount, sapFxRate);
    assertEquals(20D, sapPaymentAmountLocalCurrency);
  }

  @Test
  void testGetSapPaymentAmountLocalCurrencyWhenRequiresFXRateIsFalse()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapPaymentAmountLocalCurrency", Boolean.class, Double.class, Double.class);
    method.setAccessible(true);

    Boolean requiresFXRate = false;
    Double sapFinalPaymentAmount = 10.1D;
    Double sapFxRate = 2.2;

    Double sapPaymentAmountLocalCurrency =
        (Double)
            method.invoke(
                saptdServiceForZPDocumentType, requiresFXRate, sapFinalPaymentAmount, sapFxRate);
    assertEquals(sapFinalPaymentAmount, sapPaymentAmountLocalCurrency);
  }

  @Test
  void testGetSapPaymentAmountLocalCurrencyWhenSapFinalPaymentAmountIsNull()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapPaymentAmountLocalCurrency", Boolean.class, Double.class, Double.class);
    method.setAccessible(true);

    Boolean requiresFXRate = false;
    Double sapFinalPaymentAmount = null;
    Double sapFxRate = 2.2;

    Double sapPaymentAmountLocalCurrency =
        (Double)
            method.invoke(
                saptdServiceForZPDocumentType, requiresFXRate, sapFinalPaymentAmount, sapFxRate);
    assertNull(sapPaymentAmountLocalCurrency);
  }

  @Test
  void testGetSapFinalWhtAmountLocalCurrency()
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapFinalWhtAmountLocalCurrency", Boolean.class, Double.class, Double.class);
    method.setAccessible(true);

    Boolean requiresFXRate = true;
    Double sapFinalWhtAmount = 10D;
    Double sapFxRate = 2D;

    Double SapFinalWhtAmountLocalCurrency =
        (Double)
            method.invoke(
                saptdServiceForZPDocumentType, requiresFXRate, sapFinalWhtAmount, sapFxRate);
    assertEquals(20D, SapFinalWhtAmountLocalCurrency);
  }

  @Test
  void testGetSapFinalWhtAmountLocalCurrencyWhenRequiresFXRateIsFalse()
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapFinalWhtAmountLocalCurrency", Boolean.class, Double.class, Double.class);
    method.setAccessible(true);

    Boolean requiresFXRate = false;
    Double sapFinalWhtAmount = 10D;
    Double sapFxRate = 2D;

    Double SapFinalWhtAmountLocalCurrency =
        (Double)
            method.invoke(
                saptdServiceForZPDocumentType, requiresFXRate, sapFinalWhtAmount, sapFxRate);
    assertEquals(sapFinalWhtAmount, SapFinalWhtAmountLocalCurrency);
  }

  @Test
  void testGetSapFinalWhtAmountLocalCurrencySapFinalWhtAmountIsNull()
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapFinalWhtAmountLocalCurrency", Boolean.class, Double.class, Double.class);
    method.setAccessible(true);

    Boolean requiresFXRate = true;
    Double sapFinalWhtAmount = null;
    Double sapFxRate = 2D;

    Double SapFinalWhtAmountLocalCurrency =
        (Double)
            method.invoke(
                saptdServiceForZPDocumentType, requiresFXRate, sapFinalWhtAmount, sapFxRate);
    assertNull(SapFinalWhtAmountLocalCurrency);
  }

  @Test
  void testUpdateSAPTotalAmount()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "updateSAPTotalAmount", WorkbenchBatchData.class, Map.class);
    method.setAccessible(true);

    UUID uuid = UUID.randomUUID();
    WorkbenchBatchData wbBatchData =
        WorkbenchBatchData.builder()
            .workbenchBatch(WorkbenchBatch.builder().id(uuid).build())
            .sapPaymentCurrencyCode("ABC")
            .sapFinalPaymentAmount(100D)
            .build();

    Map<UUID, Map<String, Double>> workbenchBatchIdSAPAmountMap = new HashMap<>();
    method.invoke(saptdServiceForZPDocumentType, wbBatchData, workbenchBatchIdSAPAmountMap);
    Map<String, Double> currencyVsSAPAmount = workbenchBatchIdSAPAmountMap.get(uuid);
    assertEquals(100D, currencyVsSAPAmount.get("ABC"));
  }

  @Test
  void testUpdateSAPTotalAmountWhenBatchIdExistsInMap()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "updateSAPTotalAmount", WorkbenchBatchData.class, Map.class);
    method.setAccessible(true);

    UUID uuid = UUID.randomUUID();
    WorkbenchBatchData wbBatchData =
        WorkbenchBatchData.builder()
            .workbenchBatch(WorkbenchBatch.builder().id(uuid).build())
            .sapPaymentCurrencyCode("ABC")
            .sapFinalPaymentAmount(100D)
            .build();

    Map<UUID, Map<String, Double>> workbenchBatchIdSAPAmountMap = new HashMap<>();
    Map<String, Double> innerMap = new HashMap<>();
    innerMap.put("ABC", 50D);
    workbenchBatchIdSAPAmountMap.put(uuid, innerMap);

    method.invoke(saptdServiceForZPDocumentType, wbBatchData, workbenchBatchIdSAPAmountMap);
    Map<String, Double> currencyVsSAPAmount = workbenchBatchIdSAPAmountMap.get(uuid);
    assertEquals(150D, currencyVsSAPAmount.get("ABC"));
  }

  @Test
  void testUpdateSAPTotalAmountWhenWorkbenchBatchDataIsNull() throws NoSuchMethodException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "updateSAPTotalAmount", WorkbenchBatchData.class, Map.class);
    method.setAccessible(true);

    WorkbenchBatchData wbBatchData = null;

    Map<UUID, Map<String, Double>> workbenchBatchIdSAPAmountMap = new HashMap<>();
    assertThrows(
        InvocationTargetException.class,
        () ->
            method.invoke(
                saptdServiceForZPDocumentType, wbBatchData, workbenchBatchIdSAPAmountMap));
  }

  @Disabled
  @Test
  void testGetSapTDResponseDataForFXRate()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapTDResponseDataForFXRate", SAPTDResponse.class, String.class, List.class);
    method.setAccessible(true);

    List<SAPTDResponseData> saptdResponseDataList = new ArrayList<>();
    saptdResponseDataList.add(
        SAPTDResponseData.builder().companyCode("1234").glAccount("9876").build());
    saptdResponseDataList.add(
        SAPTDResponseData.builder().companyCode("2345").glAccount("8765").build());
    saptdResponseDataList.add(
        SAPTDResponseData.builder().companyCode("3456").glAccount("7654").build());
    saptdResponseDataList.add(
        SAPTDResponseData.builder().companyCode("4567").glAccount("6543").build());

    SAPTDResponse mockSaptdResponse =
        SAPTDResponse.builder().__count("1").results(saptdResponseDataList).build();
    String companyCode = "4567";
    List<String> clientGlAccountValue = new ArrayList<>(Arrays.asList("8765", "6543"));

    SAPTDResponseData saptdResponseData =
        (SAPTDResponseData)
            method.invoke(
                saptdServiceForZPDocumentType,
                mockSaptdResponse,
                companyCode,
                clientGlAccountValue);
    assertNotNull(saptdResponseData);
    assertEquals("4567", saptdResponseData.getCompanyCode());
    assertEquals("6543", saptdResponseData.getGlAccount());
  }

  @Disabled
  @Test
  void testGetSapTDResponseDataForFXRateWhenMatchingDataNotFound()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapTDResponseDataForFXRate", SAPTDResponse.class, String.class, List.class);
    method.setAccessible(true);

    List<SAPTDResponseData> saptdResponseDataList = new ArrayList<>();
    saptdResponseDataList.add(
        SAPTDResponseData.builder().companyCode("1234").glAccount("9876").build());
    saptdResponseDataList.add(
        SAPTDResponseData.builder().companyCode("2345").glAccount("8765").build());

    SAPTDResponse mockSaptdResponse =
        SAPTDResponse.builder().__count("1").results(saptdResponseDataList).build();
    String companyCode = "4567";
    List<String> clientGlAccountValue = new ArrayList<>(Arrays.asList("8765", "6543"));

    SAPTDResponseData saptdResponseData =
        (SAPTDResponseData)
            method.invoke(
                saptdServiceForZPDocumentType,
                mockSaptdResponse,
                companyCode,
                clientGlAccountValue);
    assertNull(saptdResponseData);
  }

  @Disabled
  @Test
  void testGetSapTDResponseDataForFXRateWhenSAPTDResponseIsNull() throws NoSuchMethodException {
    Method method =
        SAPTDServiceForZPDocumentType.class.getDeclaredMethod(
            "getSapTDResponseDataForFXRate", SAPTDResponse.class, String.class, List.class);
    method.setAccessible(true);

    SAPTDResponse mockSaptdResponse = null;
    String companyCode = "4567";
    List<String> clientGlAccountValue = new ArrayList<>(Arrays.asList("1234", "2345"));

    assertThrows(
        InvocationTargetException.class,
        () ->
            method.invoke(
                saptdServiceForZPDocumentType,
                mockSaptdResponse,
                companyCode,
                clientGlAccountValue));
  }
}
