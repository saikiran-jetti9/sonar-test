package com.bmg.trigon.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bmg.trigon.dto.SAPMDResponse;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

  @InjectMocks public EmailService emailService;

  @Test
  void testPrepareHtmlTableFromSAPMDResponseWithNoApiResults() {
    Set<SAPMDResponse> sapResponses = new HashSet<>();
    SAPMDResponse response = mock(SAPMDResponse.class);

    when(response.getVendorNumber()).thenReturn(123L);
    when(response.getStatus()).thenReturn("Active");
    when(response.getApiResults()).thenReturn(Collections.emptyMap());

    sapResponses.add(response);

    String result = emailService.prepareHtmlTableFromSAPMDResponse(sapResponses);

    assertTrue(result.contains("<td>123</td>"));
    assertTrue(result.contains("<td>Active</td>"));
    assertTrue(result.contains("<td colspan='2'>No API results</td>"));
  }

  @Test
  void testPrepareHtmlTableFromSAPMDResponseWithApiResults() {
    Set<SAPMDResponse> sapResponses = new HashSet<>();
    SAPMDResponse response = mock(SAPMDResponse.class);

    Map<String, String> apiResults = new HashMap<>();
    apiResults.put("API1", "Success");

    when(response.getVendorNumber()).thenReturn(456L);
    when(response.getStatus()).thenReturn("Pending");
    when(response.getApiResults()).thenReturn(apiResults);

    sapResponses.add(response);

    String result = emailService.prepareHtmlTableFromSAPMDResponse(sapResponses);

    assertTrue(result.contains("<td>456</td>"));
    assertTrue(result.contains("<td>Pending</td>"));
    assertTrue(result.contains("<td>API1</td>"));
    assertTrue(result.contains("<td>Success</td>"));
  }

  @Test
  void testPrepareHtmlTableFromSAPMDResponseWithEmptySet() {

    Set<SAPMDResponse> sapResponses = new HashSet<>();

    String result = emailService.prepareHtmlTableFromSAPMDResponse(sapResponses);

    assertTrue(result.contains("<table class='custom-table'>"));
    assertTrue(result.contains("</table>"));
  }

  @Test
  void testPrepareHtmlTableFromSAPMDResponseWithNullApiResults() {
    // Arrange
    Set<SAPMDResponse> sapResponses = new HashSet<>();
    SAPMDResponse response = mock(SAPMDResponse.class);

    Map<String, String> apiResults = new HashMap<>();
    apiResults.put(null, null);

    when(response.getVendorNumber()).thenReturn(null);
    when(response.getStatus()).thenReturn(null);
    when(response.getApiResults()).thenReturn(apiResults);

    sapResponses.add(response);

    String result = emailService.prepareHtmlTableFromSAPMDResponse(sapResponses);
    assertTrue(result.contains("<td></td><td></td><td></td><td></td>")); // For null values
  }
}
