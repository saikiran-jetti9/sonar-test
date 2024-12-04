package com.bmg.trigon.service;

import com.bmg.trigon.common.util.FileUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.stereotype.Service;

@Service
public class SftpService {

  private static final Logger logger = LoggerFactory.getLogger(SftpService.class);

  @Autowired private SftpRemoteFileTemplate remoteFileTemplate;

  public List<String> listAllFile(String path) {
    return remoteFileTemplate.execute(
        session -> {
          Stream<String> names = Arrays.stream(session.listNames(path));
          return names.collect(Collectors.toList());
        });
  }

  public File downloadFile(String fileName, String savePath) {
    return remoteFileTemplate.execute(
        session -> {
          boolean existFile = session.exists(fileName);
          if (existFile) {
            InputStream is = session.readRaw(fileName);
            return FileUtils.convertInputStreamToFile(is, savePath);
          } else {
            logger.info("file : {} not exist", fileName);
            return null;
          }
        });
  }

  public InputStream downloadFileAsStream(String fileName, String savePath) {
    return remoteFileTemplate.execute(
        session -> {
          boolean existFile = session.exists(fileName);
          if (existFile) {
            return session.readRaw(fileName);
          } else {
            logger.info("file : {} not exist", fileName);
            return null;
          }
        });
  }

  public boolean existFile(String filePath) {
    return remoteFileTemplate.execute(session -> session.exists(filePath));
  }

  public boolean deleteFile(String fileName) {
    return remoteFileTemplate.execute(
        session -> {
          boolean existFile = session.exists(fileName);
          if (existFile) {
            return session.remove(fileName);
          } else {
            logger.info("file : {} not exist", fileName);
            return false;
          }
        });
  }

  public boolean move(String pathFrom, String pathTo) {
    return remoteFileTemplate.execute(
        session -> {
          boolean existFile = session.exists(pathFrom);
          if (existFile) {
            session.rename(pathFrom, pathTo);
            return true;
          } else {
            logger.info("file : {} not exist", pathFrom);
            return false;
          }
        });
  }

  public boolean uploadFile(File localFile, String remoteFilePath) {
    return remoteFileTemplate.execute(
        session -> {
          try (InputStream inputStream = new FileInputStream(localFile)) {
            session.write(inputStream, remoteFilePath);
            logger.info("Successfully uploaded file to path: {}", remoteFilePath);
            return true;
          } catch (IOException e) {
            logger.error("Error uploading file to path: {}", remoteFilePath, e);
            return false;
          }
        });
  }
}
