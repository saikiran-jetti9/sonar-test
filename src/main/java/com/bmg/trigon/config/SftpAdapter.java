package com.bmg.trigon.config;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

@Configuration
public class SftpAdapter {

  @Autowired private SftpProperties sftpProperties;

  @Bean
  public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {
    DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
    factory.setHost(sftpProperties.getSapMdHost());
    factory.setPort(sftpProperties.getSapMdPort());
    factory.setUser(sftpProperties.getSapMdUser());
    factory.setPassword(sftpProperties.getSapMdPwd());
    factory.setAllowUnknownKeys(true);
    return new CachingSessionFactory<>(factory);
  }

  @Bean
  public SftpRemoteFileTemplate template() {
    return new SftpRemoteFileTemplate(sftpSessionFactory());
  }
}
