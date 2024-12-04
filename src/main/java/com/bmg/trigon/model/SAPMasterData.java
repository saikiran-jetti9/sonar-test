package com.bmg.trigon.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "sap_master_data")
public class SAPMasterData {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false)
  private Long id;

  @Column(name = "segment", nullable = false)
  @Enumerated(EnumType.STRING)
  private Segment segment;

  @Column(name = "sap_key")
  private String sapKey;

  @Column(name = "account_number_of_vendor_or_creditor")
  private Long accountNumberOfVendorOrCreditor;

  @Column(name = "company_code")
  private String companyCode;

  @Column(name = "imported_from_file_name")
  private String importedFromFileName;

  @Column(name = "created_time", columnDefinition = "TIMESTAMP")
  private ZonedDateTime createdTime;

  @Column(name = "zrefnr_reference_number")
  private String zrefnrReferenceNumber;

  @Column(name = "country_key")
  private String countryKey;

  @Column(name = "language_key")
  private String languageKey;

  @Column(name = "bank_country_key")
  private String bankCountryKey;

  @Column(name = "vat_registration_number")
  private String vatRegistrationNumber;

  @Column(name = "tax_number_1")
  private String taxNumber1;

  @Column(name = "tax_number_at_responsible_tax_authority")
  private String taxNumberAtResponsibleTaxAuthority;

  @Column(name = "payment_methods")
  private String paymentMethods;

  @Column(name = "payment_method_description")
  private String paymentMethodDescription;

  @Column(name = "block_key_for_payment")
  private String blockKeyForPayment;

  @Column(name = "description_of_block_key_for_payment")
  private String descriptionOfBlockKeyForPayment;

  @Column(name = "central_posting_block")
  private String centralPostingBlock;

  @Column(name = "posting_block_for_company_code")
  private String postingBlockForCompanyCode;

  @Column(name = "central_deletion_flag_for_master_record")
  private String centralDeletionFlagForMasterRecord;

  @Column(name = "deletion_flag_for_master_record_company_code")
  private String deletionFlagForMasterRecordCompanyCode;

  @Column(name = "vendor_account_group")
  private String vendorAccountGroup;

  @Column(name = "name_1")
  private String name1;

  @Column(name = "indicator_subject_to_withholding_tax")
  private String indicatorSubjectToWithholdingTax;

  @Column(name = "indicator_for_withholding_tax_type")
  private String indicatorForWithholdingTaxType;

  @Column(name = "withholding_tax_code")
  private String withholdingTaxCode;

  @Column(name = "withholding_tax_country_key")
  private String withholdingTaxCountryKey;

  @Column(name = "exemption_reason")
  private String exemptionReason;

  @Column(name = "exemption_certificate_number")
  private String exemptionCertificateNumber;

  @Column(name = "date_on_which_exemption_begins")
  private String dateOnWhichExemptionBegins;

  @Column(name = "date_on_which_exemption_ends")
  private String dateOnWhichExemptionEnds;

  @Column(name = "mm_payment_terms")
  private String mmPaymentTerms;

  @Column(name = "purchase_organization")
  private String purchaseOrganization;

  @Column(name = "eval_receipt_sett")
  private String evaluationReceiptSett;

  @Column(name = "aut_ev_gr_settmt_ret")
  private String autEvGRSettmtRet;

  @Column(name = "gr_based_inv_verification")
  private String grBasedInvVerification;

  @Column(name = "liable_for_vat")
  private String liableForVAT;

  @Column(name = "name_2")
  private String name2;

  @Column(name = "name_3")
  private String name3;

  @Column(name = "street")
  private String street;

  @Column(name = "street_2")
  private String street2;

  @Column(name = "street_3")
  private String street3;

  @Column(name = "city")
  private String city;

  @Column(name = "region")
  private String region;

  @Column(name = "postal_code")
  private String postalCode;

  @Column(name = "email")
  private String email;

  @Column(name = "telephone")
  private String telephone;

  @Column(name = "reconciliation_account")
  private String reconciliationAccount;

  @Column(name = "deletion_flag_for_purchasing_organisation")
  private String deletionFlagForPurchasingOrganisation;

  @Column(name = "purchase_block_purchasing_organisation_level")
  private String purchaseBlockPurchasingOrganisationLevel;
}
