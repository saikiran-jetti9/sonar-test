package com.bmg.trigon.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sap_master_data_sync")
public class SAPMasterDataSync {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false)
  private Long id;

  @Column(name = "territory", nullable = false)
  private String territory;

  @Column(name = "segment", nullable = false)
  private String segment;

  @Column(name = "last_run_number")
  private Long lastRunNumber;

  @Column(name = "last_run_date")
  private String lastRunDate;

  @Column(name = "sync_started_at")
  private String syncStartedAt;

  @Column(name = "sync_finished_at")
  private String syncFinishedAt;
}
