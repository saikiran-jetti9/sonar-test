package com.bmg.trigon.dto;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDetails {
  private String batchId;
  private String batchName;
  private Integer noOfLines;
  private String totalAmount;
  private String amountFromSAP;
  private String lastModified;
  private Integer toPost;

  private Integer toPostModified;

  private Integer postingInProgress;

  private Integer postingError;

  private Integer complete;

  private Integer declined;
  private Integer pendingApproval;
  private Integer approved;
  private String posted;
  private Boolean isDefaultBatch;
  private String workbenchCriteriaId;
  private ZonedDateTime created;
}
