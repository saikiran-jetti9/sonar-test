package com.bmg.trigon.service;

import com.google.cloud.storage.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GCSService {

  @Value("${cloud.gcs.project-id}")
  private String projectId;

  @Value("${cloud.gcs.bucket-name}")
  private String bucketName;

  public String uploadFile(File file, String path, String fileName) throws IOException {
    String fileInfo = path + File.separator + fileName;
    log.info("uploading file to gcs :{}", fileInfo);
    Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, fileInfo).build();
    storage.create(blobInfo, Files.readAllBytes(file.toPath()));
    return fileInfo;
  }
}
