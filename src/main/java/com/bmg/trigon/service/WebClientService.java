package com.bmg.trigon.service;

import com.bmg.trigon.config.SapTDAPIConfiguration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class WebClientService {

  @Autowired private SapTDAPIConfiguration sapTDAPIConfiguration;
  private static final int MAX_IN_MEMORY_SIZE_MB = 70;

  public JSONObject getSAPTransactionalDataForHGDocumentType(String urri, int skip, int top) {
    WebClient webClient = createWebClientWithBasicAuth();

    ExchangeStrategies strategies = configureExchangeStrategies();

    webClient = webClient.mutate().exchangeStrategies(strategies).build();

    String url =
        String.format(
            "%s(DocumentReferenceID eq '%s' and AccountingDocumentType eq 'HG')&$inlinecount=allpages&$skip=%d&$top=%d&$format=json",
            sapTDAPIConfiguration.getUrl(), urri, skip, top);

    Mono<JSONObject> transactionalMono = retrieveSAPDataMono(webClient, url);

    JSONObject transactionalData = transactionalMono.block();
    log.info(
        "data received from transactional api : {}",
        transactionalData != null ? transactionalData.keySet().size() : "No data received");
    return (JSONObject) (transactionalData != null ? transactionalData.get("d") : null);
  }

  public JSONObject getSAPTransactionalDataForZPDocumentType(
      List<String> clearingAccountingDocuments) {
    WebClient webClient = createWebClientWithBasicAuth();

    ExchangeStrategies strategies = configureExchangeStrategies();

    webClient = webClient.mutate().exchangeStrategies(strategies).build();

    String url = buildSAPTDAPIPaymentUrl(clearingAccountingDocuments, sapTDAPIConfiguration);

    Mono<JSONObject> transactionalMono = retrieveSAPDataMono(webClient, url);

    JSONObject transactionalData = transactionalMono.block();
    log.info(
        "data received from transactional api for Payment : {}",
        transactionalData != null ? transactionalData.keySet().size() : "No data received");
    return (JSONObject) (transactionalData != null ? transactionalData.get("d") : null);
  }

  private WebClient createWebClientWithBasicAuth() {
    return WebClient.builder()
        .filter(
            ExchangeFilterFunctions.basicAuthentication(
                sapTDAPIConfiguration.getUsername(), sapTDAPIConfiguration.getPassword()))
        .build();
  }

  private ExchangeStrategies configureExchangeStrategies() {
    return ExchangeStrategies.builder()
        .codecs(
            configurer -> {
              configurer.defaultCodecs().maxInMemorySize(1024 * 1024 * MAX_IN_MEMORY_SIZE_MB);
              configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder());
              configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder());
            })
        .build();
  }

  private Mono<JSONObject> retrieveSAPDataMono(WebClient webClient, String url) {
    return webClient
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(String.class)
        .flatMap(
            responseBody -> {
              try {
                JSONObject jsonObject = new JSONObject(responseBody);
                return Mono.just(jsonObject);
              } catch (JSONException e) {
                return Mono.error(e);
              }
            });
  }

  private String buildSAPTDAPIPaymentUrl(
      List<String> clearingAccountingDocuments, SapTDAPIConfiguration sapTDAPIConfiguration) {
    StringBuilder urlBuilder = new StringBuilder();

    urlBuilder.append(sapTDAPIConfiguration.getUrl());

    urlBuilder.append("(");
    int numberOfDocuments = clearingAccountingDocuments.size();
    for (int documentIndex = 0; documentIndex < numberOfDocuments; documentIndex++) {
      urlBuilder.append("ClearingAccountingDocument eq '");
      urlBuilder.append(clearingAccountingDocuments.get(documentIndex));
      urlBuilder.append("'");

      if (documentIndex < numberOfDocuments - 1) {
        urlBuilder.append(" or ");
      }
    }
    urlBuilder.append(") and (AccountingDocumentType eq 'ZP' or AccountingDocumentType eq 'ZV')");

    urlBuilder.append("&$inlinecount=allpages");
    urlBuilder.append("&$orderby=ClearingAccountingDocument");
    urlBuilder.append("&$format=json");

    return urlBuilder.toString();
  }
}
