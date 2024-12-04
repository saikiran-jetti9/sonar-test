package com.bmg.trigon.config;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class WebClientConfig {

  @Value("${com.bmg.sap.md.url}")
  private String sapMdUrl;

  @Value("${com.bmg.sap.md.basic-auth.username}")
  private String sapMdBasicAuthUsername;

  @Value("${com.bmg.sap.md.basic-auth.password}")
  private String sapMdBasicAuthPassword;

  @Bean
  public WebClient sapClient(WebClient.Builder webClientBuilder) {

    return webClientBuilder
        .baseUrl(sapMdUrl)
        .filter(basicAuth())
        .filter(errorHandler())
        .filter(logRequest())
        .filter(logResponse())
        .build();
  }

  private ExchangeFilterFunction logRequest() {
    return (clientRequest, next) -> {
      log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
      log.info("--- Http Headers: ---");
      clientRequest.headers().forEach(this::logHeader);
      log.info("--- Http Cookies: ---");
      clientRequest.cookies().forEach(this::logHeader);
      return next.exchange(clientRequest);
    };
  }

  private ExchangeFilterFunction basicAuth() {
    return (request, next) ->
        next.exchange(
            ClientRequest.from(request)
                .headers(
                    (headers) ->
                        headers.setBasicAuth(sapMdBasicAuthUsername, sapMdBasicAuthPassword))
                .build());
  }

  private ExchangeFilterFunction logResponse() {
    return ExchangeFilterFunction.ofResponseProcessor(
        clientResponse -> {
          log.info("Response: {}", clientResponse.statusCode());
          clientResponse
              .headers()
              .asHttpHeaders()
              .forEach((name, values) -> values.forEach(value -> log.info("{}={}", name, value)));
          return Mono.just(clientResponse);
        });
  }

  private void logHeader(String name, List<String> values) {
    values.forEach(value -> log.info("{}={}", name, value));
  }

  public ExchangeFilterFunction errorHandler() {
    return ExchangeFilterFunction.ofResponseProcessor(
        clientResponse -> {
          if (clientResponse.statusCode().is5xxServerError()) {
            return clientResponse
                .bodyToMono(String.class)
                .flatMap(errorBody -> Mono.error(new IllegalArgumentException(errorBody)));
          } else if (clientResponse.statusCode().is4xxClientError()) {
            return clientResponse
                .bodyToMono(String.class)
                .flatMap(errorBody -> Mono.error(new IllegalArgumentException(errorBody)));
          } else {
            return Mono.just(clientResponse);
          }
        });
  }
}
