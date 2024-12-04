package com.bmg.trigon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SAPTDResponseData {

  @JsonProperty("ID")
  private String id;

  @JsonProperty("AccountingDocument")
  private String accountingDocument;

  @JsonProperty("AccountingDocumentType")
  private String accountingDocumentType;

  @JsonProperty("Supplier")
  private String supplier;

  @JsonProperty("Supplier_Text")
  private String supplier_Text;

  @JsonProperty("PostingKey")
  private String postingKey;

  @JsonProperty("PostingDate")
  private String postingDate;

  @JsonProperty("DebitCreditCode")
  private String debitCreditCode;

  @JsonProperty("FiscalYear")
  private String fiscalYear;

  @JsonProperty("DocumentDate")
  private String documentDate;

  @JsonProperty("DocumentItemText")
  private String documentItemText;

  @JsonProperty("ExchangeRateDate")
  private String exchangeRateDate;

  @JsonProperty("IsReversed")
  private Boolean isReversed;

  @JsonProperty("PaymentBlockingReason")
  private String paymentBlockingReason;

  @JsonProperty("StateCentralBankPaymentReason")
  private String stateCentralBankPaymentReason;

  @JsonProperty("WithholdingTaxCode")
  private String withholdingTaxCode;

  @JsonProperty("SpecialGLTransactionType")
  private String specialGLTransactionType;

  @JsonProperty("VATRegistration")
  private String vatRegistration;

  @JsonProperty("AccountingDocumentCatGroup")
  private String accountingDocumentCatGroup;

  @JsonProperty("AccountingDocumentItem")
  private String accountingDocumentItem;

  @JsonProperty("ChartOfAccounts")
  private String chartOfAccounts;

  @JsonProperty("ControllingArea")
  private String controllingArea;

  @JsonProperty("ControllingArea_Text")
  private String controllingArea_Text;

  @JsonProperty("IsOpenItemManaged")
  private boolean isOpenItemManaged;

  @JsonProperty("NumberOfItems")
  private String numberOfItems;

  @JsonProperty("WithholdingTaxExemptionAmt")
  private String withholdingTaxExemptionAmt;

  @JsonProperty("TaxAmountInCoCodeCrcy")
  private String taxAmountInCoCodeCrcy;

  @JsonProperty("TaxAmount")
  private String taxAmount;

  @JsonProperty("TaxBaseAmountInCoCodeCrcy")
  private String taxBaseAmountInCoCodeCrcy;

  @JsonProperty("CompanyCodeName")
  private String companyCodeName;

  @JsonProperty("AccountingDocumentType_Text")
  private String accountingDocumentType_Text;

  @JsonProperty("GLAccountLongName")
  private String glAccountLongName;

  @JsonProperty("DebitCreditCode_Text")
  private String debitCreditCode_Text;

  @JsonProperty("EmployeeFullName")
  private String employeeFullName;

  @JsonProperty("AccountingDocumentCatGroupName")
  private String accountingDocumentCatGroupName;

  // using fields

  @JsonProperty("DocumentReferenceID")
  private String documentReferenceID;

  @JsonProperty("AssignmentReference")
  private String assignmentReference;

  @JsonProperty("CompanyCode")
  private String companyCode;

  @JsonProperty("AccountingDocumentHeaderText")
  private String accountingDocumentHeaderText;

  @JsonProperty("GLAccount")
  private String glAccount;

  @JsonProperty("TransactionCurrency")
  private String transactionCurrency;

  @JsonProperty("CompanyCodeCurrency")
  private String companyCodeCurrency;

  @JsonProperty("TaxCode")
  private String taxCode;

  @JsonProperty("ExchangeRate")
  private String exchangeRate;

  @JsonProperty("ClearingAccountingDocument")
  private String clearingAccountingDocument;

  @JsonProperty("ClearingDate")
  private String clearingDate;

  @JsonProperty("IsCleared")
  private Boolean isCleared;

  @JsonProperty("AmountInTransactionCurrency")
  private String amountInTransactionCurrency;

  @JsonProperty("AmountInCompanyCodeCurrency")
  private String amountInCompanyCodeCurrency;

  @JsonProperty("WithholdingTaxAmount")
  private String withholdingTaxAmount;

  @JsonProperty("WithholdingTaxBaseAmount")
  private String withholdingTaxBaseAmount;
}
