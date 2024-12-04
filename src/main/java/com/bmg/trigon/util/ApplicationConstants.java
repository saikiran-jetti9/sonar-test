package com.bmg.trigon.util;

import java.util.List;

public class ApplicationConstants {
  public static final String MSG = "message";
  public static final String EXEMPTION_DATE_FORMAT = "M/d/yyyy";
  // SAP MD related constants
  public static final String DATA = "d";
  public static final String RESULTS = "results";
  public static final String SUPPLIER = "Supplier";
  public static final String VAT_REGISTRATION = "VATRegistration";
  public static final String TAX_NUMBER1 = "TaxNumber1";
  public static final String TAX_NUMBER_RESPONSIBLE = "TaxNumberResponsible";
  public static final String POSTING_IS_BLOCKED = "PostingIsBlocked";
  public static final String DELETION_INDICATOR = "DeletionIndicator";
  public static final String SUPPLIER_NAME = "SupplierName";
  public static final String PURCHASING_IS_BLOCKED_FOR_SUPPLIER = "PurchasingIsBlockedForSupplier";
  public static final String PURCHASING_ORGANIZATION = "PurchasingOrganization";

  public static final String COMPANY_CODE = "CompanyCode";
  public static final String PAYMENT_METHODS_LIST = "PaymentMethodsList";
  public static final String PAYMENT_BLOCKING_REASON = "PaymentBlockingReason";
  public static final String SUPPLIER_IS_BLOCKED_FOR_POSTING = "SupplierIsBlockedForPosting";
  public static final String SUPPLIER_ACCOUNT_GROUP = "SupplierAccountGroup";
  public static final String WITH_HOLDING_TAX_COUNTRY = "WithholdingTaxCountry";
  public static final String PAYMENT_TERMS = "PaymentTerms";
  public static final String RECONCILIATION_ACCOUNT = "ReconciliationAccount";

  public static final String COUNTRY = "Country";
  public static final String LANGUAGE = "Language";
  public static final String STREET_NAME = "StreetName";
  public static final String ADDITIONAL_STREET_PREFIX_NAME = "AdditionalStreetPrefixName";
  public static final String ADDITIONAL_STREET_SUFFIX_NAME = "AdditionalStreetSuffixName";
  public static final String CITY_NAME = "CityName";
  public static final String REGION = "Region";
  public static final String POSTAL_CODE = "PostalCode";

  public static final String BANK_COUNTRY_KEY = "BankCountryKey";
  public static final String ZZ1_STKZU_SUP = "ZZ1_STKZU_sup";

  public static final String IS_WITH_HOLDING_TAX_SUBJECT = "IsWithholdingTaxSubject";
  public static final String WITH_HOLDING_TAX_TYPE = "WithholdingTaxType";
  public static final String WITH_HOLDING_TAX_CODE = "WithholdingTaxCode";
  public static final String EXEMPTION_REASON = "ExemptionReason";
  public static final String WITH_HOLDING_TAX_EXEMPTION_CERTIFICATE =
      "WithholdingTaxExemptionCertificate";
  public static final String EXEMPTION_DATE_BEGIN = "ExemptionDateBegin";
  public static final String EXEMPTION_DATE_END = "ExemptionDateEnd";

  public static final String INVOICE_IS_GOODS_RECEIPT_BASED = "InvoiceIsGoodsReceiptBased";
  public static final List<String> SAP_MASTER_DATA_COLUMNS =
      List.of(
          "segment",
          "sap_key",
          "account_number_of_vendor_or_creditor",
          "company_code",
          "zrefnr_reference_number",
          "country_key",
          "language_key",
          "bank_country_key",
          "vat_registration_number",
          "tax_number_1",
          "tax_number_at_responsible_tax_authority",
          "payment_methods",
          "payment_method_description",
          "block_key_for_payment",
          "description_of_block_key_for_payment",
          "central_posting_block",
          "posting_block_for_company_code",
          "central_deletion_flag_for_master_record",
          "deletion_flag_for_master_record_company_code",
          "vendor_account_group",
          "name_1",
          "indicator_subject_to_withholding_tax",
          "indicator_for_withholding_tax_type",
          "withholding_tax_code",
          "withholding_tax_country_key",
          "exemption_reason",
          "exemption_certificate_number",
          "date_on_which_exemption_begins",
          "date_on_which_exemption_ends",
          "mm_payment_terms",
          "purchase_organization",
          "eval_receipt_sett",
          "aut_ev_gr_settmt_ret",
          "gr_based_inv_verification",
          "liable_for_vat",
          "name_2",
          "name_3",
          "street",
          "street_2",
          "street_3",
          "city",
          "region",
          "postal_code",
          "email",
          "telephone",
          "reconciliation_account",
          "deletion_flag_for_purchasing_organisation",
          "purchase_block_purchasing_organisation_level");

  public static final String SAP_MD_ERROR_SUBJECT =
      "Error while processing a XML file on FTP Server";
  public static final String XML_FILE_NAME = "xmlFileName";
  public static final String SUBJECT = "subject";
  public static final String FAILED_VENDOR_CODES_HTML = "failedVendorCodesHtml";
  public static final String SUCCESS_VENDOR_CODES_HTML = "successVendorCodesHtml";
  public static final String SEND_MAIL_ENDPOINT = "mail/send";
  public static final int SUCCESS_CODE_202 = 202;
  public static final String ERROR_DIR = "errorDirectory";
  public static final String RECOUP_DOCUMENT_NUMBER = "recoupDocumentNumber";
  public static final String SUCCESS_XML = "successXML";
  public static final String FAIL_XML = "errorXml";
  public static final String RESPONSE_LOG_ITEM = "logItemNote";
  public static final String ACCOUNTING_DOCUMENT = "accountingDocument";
  public static final String ERROR_RESPONSE = "error";
  public static final String TECHNICAL_ERROR = "Technical Error";
  public static final String XML_PAYLOAD_TEMPLATE_PUB_PATH =
      "data/DirectFiXMLPayloadTemplate/PayloadTemplateForPub.xml";
  public static final String XML_PAYLOAD_TEMPLATE_REC_PATH =
      "data/DirectFiXMLPayloadTemplate/PayloadTemplateForRec.xml";

  public static final String FAILED_SAP_MD_RESPONSES_HTML = "failedSAPMDResponsesHTML";
  public static final String SUPPLIER_COMPANY_API_ENDPOINT =
      "API_BUSINESS_PARTNER/A_SupplierCompany";
  public static final String SUPPLIER_API_ENDPOINT = "API_BUSINESS_PARTNER/A_Supplier";
  public static final String BUSINESS_PARTNER_ADDRESS_API_END_POINT =
      "API_BUSINESS_PARTNER/A_BusinessPartnerAddress";
  public static final String BUSINESS_PARTNER_BANK_API_END_POINT =
      "API_BUSINESS_PARTNER/A_BusinessPartnerBank";
  public static final String SUPPLIER_WITH_HOLDING_TAX_API_ENDPOINT =
      "API_BUSINESS_PARTNER/A_SupplierWithHoldingTax";
  public static final String SUPPLIER_TO_SUPPLIER_PURCHASING_ORG_API_ENDPOINT =
      "API_BUSINESS_PARTNER/A_Supplier/to_SupplierPurchasingOrg";

  public static final String SAP_API_EXCEPTION_MESSAGE = "Error while Triggering SAP API";
  public static final String TRIGON_APPLICATION_ERROR_MESSAGE = "Trigon Application Error";
  public static final String SAP_MASTER_DATA_NOT_FOUND_MESSAGE = "Sap master data not found";

  public static final String MD_PREFIX = "/md";
  public static final String REFRESH_MASTER_DATA = "/refresh-master-data";
  public static final String FAILED_VENDORS = "failedVendors";
  public static final String SUCCESSFUL_VENDORS = "successfulVendors";
}
