package com.bmg.trigon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bmg.trigon.common.enums.*;
import com.bmg.trigon.common.service.AdminSettingsService;
import com.bmg.trigon.common.service.WorkbenchBatchDataSharedService;
import com.bmg.trigon.dto.SAPTDResponseData;
import com.bmg.trigon.repository.WorkbenchBatchDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SAPTDServiceForHGDocumentTypeTest {

  @Mock private WorkbenchBatchDataRepository workbenchBatchDataRepository;
  @Mock private WebClientService webClientService;
  @Mock private ObjectMapper objectMapper;
  @Mock private AdminSettingsService adminSettingsService;
  @Mock private WorkbenchBatchDataSharedService workbenchBatchDataSharedService;

  @InjectMocks private SAPTDServiceForHGDocumentType saptdServiceForHGDocumentType;

  @Test
  void testGetSapAssignment()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapAssignment", SAPTDResponseData.class);
    method.setAccessible(true);
    SAPTDResponseData saptdResponseData =
        SAPTDResponseData.builder().assignmentReference("ABED").build();
    String response = (String) method.invoke(saptdServiceForHGDocumentType, saptdResponseData);
    assertEquals("ABED", response);
  }

  @Test
  void testGetSapAssignmentWhenSAPTDResponseDataIsNull()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapAssignment", SAPTDResponseData.class);
    method.setAccessible(true);
    SAPTDResponseData saptdResponseData = null;
    String response = (String) method.invoke(saptdServiceForHGDocumentType, saptdResponseData);
    assertNull(response);
  }

  @Test
  void testGetSapTDResponseData()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapTDResponseData",
            List.class,
            List.class,
            String.class,
            String.class,
            String.class);
    method.setAccessible(true);

    SAPTDResponseData saptdResponseData1 =
        SAPTDResponseData.builder()
            .accountingDocumentHeaderText("HeaderText")
            .accountingDocument("AccDoc")
            .companyCode("1234")
            .glAccount("9876")
            .build();
    SAPTDResponseData saptdResponseData2 =
        SAPTDResponseData.builder()
            .accountingDocumentHeaderText("AccDocHeaderText")
            .accountingDocument("AccDoc")
            .companyCode("567")
            .glAccount("1234")
            .build();
    List<SAPTDResponseData> saptdResponseDataList =
        new ArrayList<>(List.of(saptdResponseData1, saptdResponseData2));

    List<String> glAccountValues = new ArrayList<>(List.of("9876", "8765"));
    String fullURRI = "HeaderText";
    String companyCode = "1234";
    String sapDocumentNumber = "AccDoc";

    SAPTDResponseData saptdResponseData =
        (SAPTDResponseData)
            method.invoke(
                saptdServiceForHGDocumentType,
                saptdResponseDataList,
                glAccountValues,
                fullURRI,
                companyCode,
                sapDocumentNumber);
    assertEquals("AccDoc", saptdResponseData.getAccountingDocument());
    assertEquals("HeaderText", saptdResponseData.getAccountingDocumentHeaderText());
    assertEquals("1234", saptdResponseData.getCompanyCode());
    assertEquals("9876", saptdResponseData.getGlAccount());
  }

  @Test
  void testGetSapTDResponseDataWhenNoMatchFound()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapTDResponseData",
            List.class,
            List.class,
            String.class,
            String.class,
            String.class);
    method.setAccessible(true);

    SAPTDResponseData saptdResponseData1 =
        SAPTDResponseData.builder()
            .accountingDocumentHeaderText("HeaderText")
            .accountingDocument("AccDocument")
            .companyCode("1234")
            .glAccount("9876")
            .build();
    SAPTDResponseData saptdResponseData2 =
        SAPTDResponseData.builder()
            .accountingDocumentHeaderText("AccDocHeaderText")
            .accountingDocument("AccDoc")
            .companyCode("567")
            .glAccount("1234")
            .build();
    List<SAPTDResponseData> saptdResponseDataList =
        new ArrayList<>(List.of(saptdResponseData1, saptdResponseData2));

    List<String> glAccountValues = new ArrayList<>(List.of("9876", "5674"));
    String fullURRI = "HeaderText";
    String companyCode = "1234";
    String sapDocumentNumber = "AccDoc";

    SAPTDResponseData saptdResponseData =
        (SAPTDResponseData)
            method.invoke(
                saptdServiceForHGDocumentType,
                saptdResponseDataList,
                glAccountValues,
                fullURRI,
                companyCode,
                sapDocumentNumber);
    assertNull(saptdResponseData);
  }

  @Disabled
  @Test
  void testGetSapTDResponseDataWhenSAPTDResponseDataListIsNull() throws NoSuchMethodException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapTDResponseData",
            List.class,
            List.class,
            String.class,
            String.class,
            String.class);
    method.setAccessible(true);

    List<SAPTDResponseData> saptdResponseDataList = null;

    List<String> glAccountValues = new ArrayList<>(List.of("9876", "1234"));
    String fullURRI = "HeaderText";
    String companyCode = "1234";
    String sapDocumentNumber = "AccDoc";

    assertThrows(
        InvocationTargetException.class,
        () ->
            method.invoke(
                saptdServiceForHGDocumentType,
                saptdResponseDataList,
                glAccountValues,
                fullURRI,
                companyCode,
                sapDocumentNumber));
  }

  @Test
  void testGetSapDocumentHeaderText()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapDocumentHeaderText", List.class, String.class, String.class);
    method.setAccessible(true);

    SAPTDResponseData saptdResponseData1 =
        SAPTDResponseData.builder()
            .accountingDocumentHeaderText("HeaderText")
            .accountingDocument("AccDocument")
            .build();
    List<SAPTDResponseData> saptdResponseDataList = new ArrayList<>(List.of(saptdResponseData1));
    String fullURRI = "HeaderText";
    String sapDocumentNumber = "AccDocument";

    String headerText =
        (String)
            method.invoke(
                saptdServiceForHGDocumentType, saptdResponseDataList, fullURRI, sapDocumentNumber);
    assertEquals("HeaderText", headerText);
  }

  @Test
  void testGetSapDocumentHeaderTextWheSAPTDResponseDataListIsNull()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapDocumentHeaderText", List.class, String.class, String.class);
    method.setAccessible(true);

    List<SAPTDResponseData> saptdResponseDataList = null;
    String fullURRI = "HeaderText";
    String sapDocumentNumber = "AccDocument";

    String headerText =
        (String)
            method.invoke(
                saptdServiceForHGDocumentType, saptdResponseDataList, fullURRI, sapDocumentNumber);
    assertNull(headerText);
  }

  @Test
  void testGetSapReference()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapReference", List.class, String.class, String.class);
    method.setAccessible(true);

    SAPTDResponseData saptdResponseData1 =
        SAPTDResponseData.builder()
            .accountingDocumentHeaderText("HeaderText")
            .accountingDocument("AccDocument")
            .documentReferenceID("1234")
            .build();
    List<SAPTDResponseData> saptdResponseDataList = new ArrayList<>(List.of(saptdResponseData1));
    String fullURRI = "HeaderText";
    String sapDocumentNumber = "AccDocument";

    String sapReference =
        (String)
            method.invoke(
                saptdServiceForHGDocumentType, saptdResponseDataList, fullURRI, sapDocumentNumber);
    assertEquals("1234", sapReference);
  }

  @Test
  void testGetSapReferenceWhenSAPTDResponseDataListIsNull()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapReference", List.class, String.class, String.class);
    method.setAccessible(true);

    List<SAPTDResponseData> saptdResponseDataList = null;
    String fullURRI = "HeaderText";
    String sapDocumentNumber = "AccDocument";

    String sapReference =
        (String)
            method.invoke(
                saptdServiceForHGDocumentType, saptdResponseDataList, fullURRI, sapDocumentNumber);
    assertNull(sapReference);
  }

  @Test
  void testGetSapPaymentDate()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapPaymentDate", SAPTDResponseData.class);
    method.setAccessible(true);
    SAPTDResponseData saptdResponseData =
        SAPTDResponseData.builder().clearingDate("/Date(1749888000000)/").build();
    Date sapPaymentDate = (Date) method.invoke(saptdServiceForHGDocumentType, saptdResponseData);
    Date date = new Date(1749888000000L);
    assertEquals(date, sapPaymentDate);
  }

  @Test
  void testGetSapPaymentDate_IncorrectDateFormat() throws NoSuchMethodException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapPaymentDate", SAPTDResponseData.class);
    method.setAccessible(true);
    SAPTDResponseData saptdResponseData =
        SAPTDResponseData.builder().clearingDate("2024-05-03").build();
    assertThrows(
        InvocationTargetException.class,
        () -> method.invoke(saptdServiceForHGDocumentType, saptdResponseData));
  }

  @Test
  void testGetSapPaymentDateWhenSAPTDResponseDataIsNull()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapPaymentDate", SAPTDResponseData.class);
    method.setAccessible(true);
    SAPTDResponseData saptdResponseData = null;
    Date sapPaymentDate = (Date) method.invoke(saptdServiceForHGDocumentType, saptdResponseData);
    assertNull(sapPaymentDate);
  }

  @Test
  void testGetSapTaxCode()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapTaxCode", List.class, String.class, String.class);
    method.setAccessible(true);

    SAPTDResponseData saptdResponseData1 =
        SAPTDResponseData.builder()
            .accountingDocumentHeaderText("HeaderText")
            .accountingDocument("AccDoc")
            .taxCode("ABC")
            .build();
    List<SAPTDResponseData> saptdResponseDataList = new ArrayList<>(List.of(saptdResponseData1));
    String fullURRI = "HeaderText";
    String sapDocumentNumber = "AccDoc";

    String taxCode =
        (String)
            method.invoke(
                saptdServiceForHGDocumentType, saptdResponseDataList, fullURRI, sapDocumentNumber);
    assertEquals("ABC", taxCode);
  }

  @Test
  void testGetSapTaxCodeWhenSAPTDResponseDataListIsNull()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        SAPTDServiceForHGDocumentType.class.getDeclaredMethod(
            "getSapTaxCode", List.class, String.class, String.class);
    method.setAccessible(true);

    List<SAPTDResponseData> saptdResponseDataList = null;
    String fullURRI = "HeaderText";
    String sapDocumentNumber = "AccDoc";

    String taxCode =
        (String)
            method.invoke(
                saptdServiceForHGDocumentType, saptdResponseDataList, fullURRI, sapDocumentNumber);
    assertNull(taxCode);
  }
}
