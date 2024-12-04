package com.bmg.deliver.model;

import jakarta.persistence.*;
import java.util.Date;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@Table(name = "workflow")
@EntityListeners(AuditingEntityListener.class)
public class Workflow {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name")
	private String name;

	@Column(name = "description")
	private String description;

	@Column(name = "enabled")
	private boolean enabled;

	@Column(name = "task_chain_is_valid")
	private boolean taskChainIsValid;

	@Column(name = "throttle_limit")
	private Integer throttleLimit;

	@Column(name = "alias")
	private String alias;

	@Column(name = "asset_ingestion_wait_time")
	private String assetIngestionTime;

	@Column(name = "data_ingestion_wait_time")
	private String dataIngestionTime;

	@Column(name = "paused")
	private boolean paused;

	@CreatedDate
	@Column(name = "created")
	private Date created;

	@LastModifiedDate
	@Column(name = "modified")
	private Date modified;
}
