package com.bmg.trigon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.erhlc.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.client.erhlc.RestClients;

@Configuration
public class ElasticsearchConfiguration {

  @Value("${spring.data.elasticsearch.cluster-nodes}")
  private String clusterNodes;

  @Value("${spring.data.elasticsearch.cluster-name}")
  private String clusterName;

  @Value("${spring.data.elasticsearch.cluster-username}")
  private String clusterUsername;

  @Value("${spring.data.elasticsearch.cluster-password}")
  private String clusterPassword;

  @Bean
  public ElasticsearchRestTemplate elasticsearchTemplate() {
    ClientConfiguration clientConfiguration =
        ClientConfiguration.builder()
            .connectedTo(clusterNodes)
            .withBasicAuth(clusterUsername, clusterPassword)
            .build();

    return new ElasticsearchRestTemplate(RestClients.create(clientConfiguration).rest());
  }
}
