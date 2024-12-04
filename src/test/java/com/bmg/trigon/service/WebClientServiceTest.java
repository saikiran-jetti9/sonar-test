package com.bmg.trigon.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bmg.trigon.config.SapTDAPIConfiguration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class WebClientServiceTest {

  @Mock private SapTDAPIConfiguration sapTDAPIConfiguration;

  @InjectMocks private WebClientService webClientService;

  @Test
  void testCreateWebClientWithBasicAuth()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    when(sapTDAPIConfiguration.getUsername()).thenReturn("ASJBFEBHS");
    when(sapTDAPIConfiguration.getPassword()).thenReturn("ASJG@3489ASB");

    Method method = WebClientService.class.getDeclaredMethod("createWebClientWithBasicAuth");
    method.setAccessible(true);

    WebClient webClient = (WebClient) method.invoke(webClientService);
    assertNotNull(webClient);
  }

  @Test
  void testConfigureExchangeStrategies()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = WebClientService.class.getDeclaredMethod("configureExchangeStrategies");
    method.setAccessible(true);

    ExchangeStrategies exchangeStrategies = (ExchangeStrategies) method.invoke(webClientService);
    assertNotNull(exchangeStrategies);
  }

  @Test
  void testBuildSAPTDAPIPaymentUrl()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    when(sapTDAPIConfiguration.getUrl()).thenReturn("http://exampleforthistestcase.com/");

    Method method =
        WebClientService.class.getDeclaredMethod(
            "buildSAPTDAPIPaymentUrl", List.class, SapTDAPIConfiguration.class);
    method.setAccessible(true);

    List<String> clearingAccountingDocuments =
        Arrays.asList("298877", "123912", "874932", "981233");

    String url =
        (String)
            method.invoke(webClientService, clearingAccountingDocuments, sapTDAPIConfiguration);
    assertNotNull(url);
    assertFalse(url.isEmpty());
  }

  @Test
  void testBuildSAPTDAPIPaymentUrlWhenClearingAccountingDocumentsIsNull()
      throws NoSuchMethodException {
    when(sapTDAPIConfiguration.getUrl()).thenReturn("http://exampleforthistestcase.com/");

    Method method =
        WebClientService.class.getDeclaredMethod(
            "buildSAPTDAPIPaymentUrl", List.class, SapTDAPIConfiguration.class);
    method.setAccessible(true);

    List<String> clearingAccountingDocuments = null;
    assertThrows(
        InvocationTargetException.class,
        () -> method.invoke(webClientService, clearingAccountingDocuments, sapTDAPIConfiguration));
  }

  @Test
  void testBuildSAPTDAPIPaymentUrlWhenSapTDAPIConfigurationIsNull() throws NoSuchMethodException {
    Method method =
        WebClientService.class.getDeclaredMethod(
            "buildSAPTDAPIPaymentUrl", List.class, SapTDAPIConfiguration.class);
    method.setAccessible(true);

    List<String> clearingAccountingDocuments =
        Arrays.asList("298877", "123912", "874932", "981233");
    SapTDAPIConfiguration mockSapTDAPIConfiguration = null;

    assertThrows(
        InvocationTargetException.class,
        () ->
            method.invoke(
                webClientService, clearingAccountingDocuments, mockSapTDAPIConfiguration));
  }

  @Test
  void testRetrieveSAPDataMono()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        WebClientService.class.getDeclaredMethod(
            "retrieveSAPDataMono", WebClient.class, String.class);
    method.setAccessible(true);

    WebClient webClient = WebClient.builder().build();
    String url = "https://example.com/api/data";

    Mono<JSONObject> jsonObjectMono =
        (Mono<JSONObject>) method.invoke(webClientService, webClient, url);
    assertNotNull(jsonObjectMono);
  }

  @Test
  void testRetrieveSAPDataMonoWhenWebClientIsNull() throws NoSuchMethodException {
    Method method =
        WebClientService.class.getDeclaredMethod(
            "retrieveSAPDataMono", WebClient.class, String.class);
    method.setAccessible(true);

    WebClient webClient = null;
    String url = "https://example.com/api/data";

    assertThrows(
        InvocationTargetException.class, () -> method.invoke(webClientService, webClient, url));
  }

  // need to write test cases getSAPTransactionalDataForHGDocumentType and
  // getSAPTransactionalDataForZPDocumentType methods
}
