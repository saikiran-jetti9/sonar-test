package com.bmg.trigon.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@ConfigurationProperties(prefix = "sap-td")
@Component
public class SapTDAPIConfiguration {
  private String url;
  private String username;
  private String password;
  private Long schedulerIntervalInMin;
}
