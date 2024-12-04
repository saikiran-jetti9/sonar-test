package com.bmg.trigon.service;

import com.bmg.trigon.dto.SAPMDResponse;
import com.bmg.trigon.util.ApplicationConstants;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

  private final SendGrid sendGrid;

  @Value("${sendgrid.from-email}")
  private String fromEmailAddress;

  @Value("${sendgrid.sap-md-error.to_emails}")
  private String sapMdErrorToEmailAddresses;

  @Value("${sendgrid.sap-md-error.cc_emails}")
  private String sapMdErrorCCEmailAddresses;

  @Value("${sendgrid.sap-md-error.template-id}")
  private String sapMDErrorTemplateId;

  public EmailService(SendGrid sendGrid) {
    this.sendGrid = sendGrid;
  }

  public void sendSAPMDErrorEmail(
      String XMLFileName,
      String errorFolderPath,
      Set<Long> successVendors,
      Set<SAPMDResponse> sapMDResponses) {

    try {
      List<Personalization> personalizations = new ArrayList<>();

      List<String> toEmailAddresses = Arrays.stream(sapMdErrorToEmailAddresses.split(",")).toList();
      List<String> ccEmailAddresses = Arrays.stream(sapMdErrorCCEmailAddresses.split(",")).toList();

      if (toEmailAddresses.isEmpty()) {
        log.warn("Unable to send SAP MD Error email, since to email addresses are empty");
        return;
      }

      String successVendorCodeHTML = prepareVendorCodeHtmlString(successVendors);
      String sapMDAPIResponsesHTML = prepareHtmlTableFromSAPMDResponse(sapMDResponses);

      personalizations =
          toEmailAddresses.stream()
              .map(
                  toEmailAddress -> {
                    Personalization personalization = new Personalization();
                    personalization.addTo(new Email(toEmailAddress));
                    ccEmailAddresses.forEach(
                        ccEmailAddress -> {
                          personalization.addCc(new Email(ccEmailAddress));
                        });
                    personalization.addDynamicTemplateData(
                        ApplicationConstants.XML_FILE_NAME, XMLFileName);
                    personalization.addDynamicTemplateData(
                        ApplicationConstants.SUBJECT, ApplicationConstants.SAP_MD_ERROR_SUBJECT);
                    personalization.addDynamicTemplateData(
                        ApplicationConstants.SUCCESS_VENDOR_CODES_HTML,
                        !successVendors.isEmpty() ? successVendorCodeHTML : "");
                    personalization.addDynamicTemplateData(
                        ApplicationConstants.FAILED_SAP_MD_RESPONSES_HTML,
                        !sapMDResponses.isEmpty() ? sapMDAPIResponsesHTML : "");
                    personalization.addDynamicTemplateData(
                        ApplicationConstants.ERROR_DIR, errorFolderPath);
                    return personalization;
                  })
              .toList();

      // Prepare the mail object
      Mail mail = new Mail();
      mail.setFrom(new Email(fromEmailAddress));
      mail.setTemplateId(sapMDErrorTemplateId);
      personalizations.forEach(mail::addPersonalization);

      // Prepare the request object
      Request sendgridRequest = new Request();
      sendgridRequest.setMethod(Method.POST);
      sendgridRequest.setEndpoint(ApplicationConstants.SEND_MAIL_ENDPOINT);
      sendgridRequest.setBody(mail.build());
      Response response = sendGrid.api(sendgridRequest);

      if (response.getStatusCode() != ApplicationConstants.SUCCESS_CODE_202) {
        log.error("Error while sending the SAP MD Error Email: {}", response.getBody());
      } else {
        log.info("SAP MD Error Email was sent successfully {}", response.getBody());
      }
    } catch (Exception e) {
      log.error("Unable to send the SAP MD Error Email: ", e);
    }
  }

  private String prepareVendorCodeHtmlString(Set<Long> vendorCodes) {
    StringBuilder vendorCodesHTMLStringBuilder = new StringBuilder();
    vendorCodesHTMLStringBuilder.append("<ol class='email-content' style='font-family: Calibri;'>");
    vendorCodes.forEach(
        vendorCode -> {
          String listItem = String.format("<li>%s</li>", vendorCode.toString());
          vendorCodesHTMLStringBuilder.append(listItem);
        });
    vendorCodesHTMLStringBuilder.append("</ol>");
    return vendorCodesHTMLStringBuilder.toString();
  }

  /**
   * Generates an HTML table from a set of {@link SAPMDResponse} objects. Each row contains the
   * vendor number, status, API, and remarks. If no API results are found, the table shows 'No API
   * results'.
   *
   * @param sapResponses a set of {@link SAPMDResponse} objects to be displayed in the table.
   * @return a {@link String} containing the HTML table.
   */
  public String prepareHtmlTableFromSAPMDResponse(Set<SAPMDResponse> sapResponses) {
    StringBuilder tableHtml = new StringBuilder();

    tableHtml.append("<table class='custom-table'>");

    // Add table headers
    tableHtml
        .append("<tr class='header-cell'>")
        .append("<th>Vendor</th>")
        .append("<th>Status</th>")
        .append("<th>API</th>")
        .append("<th>Remarks</th>")
        .append("</tr>");

    // Add table rows
    for (SAPMDResponse response : sapResponses) {
      tableHtml.append("<tr>");
      tableHtml
          .append("<td>")
          .append(response.getVendorNumber() != null ? response.getVendorNumber() : "")
          .append("</td>");
      tableHtml
          .append("<td>")
          .append(response.getStatus() != null ? response.getStatus() : "")
          .append("</td>");

      // Check and add API results
      if (response.getApiResults().isEmpty()) {
        // If no API results, add empty cells for API and Remarks
        tableHtml.append("<td colspan='2'>No API results</td>");
      } else {
        // Add API results
        response
            .getApiResults()
            .forEach(
                (key, value) -> {
                  tableHtml.append("<td>").append(key != null ? key : "").append("</td>");
                  tableHtml.append("<td>").append(value != null ? value : "").append("</td>");
                });
      }
      tableHtml.append("</tr>");
    }
    tableHtml.append("</table>");
    return tableHtml.toString();
  }
}
