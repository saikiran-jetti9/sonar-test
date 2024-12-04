package com.bmg.trigon.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@ConfigurationProperties(prefix = "sftp")
@Component
public class SftpProperties {
  private String sapMdHost;
  private Integer sapMdPort;
  private String sapMdUser;
  private String sapMdPwd;
  private String sapMdUnprocessedDir;
  private String sapMdProcessedDir;
  private String sapMdErrorDir;
  private String sapMdDirectFiDir;
  private Long sapMdWatchIntervalInMin;
}
