package com.bmg.deliver.dto;

import java.util.Date;

import lombok.Data;

@Data
public class WorkflowDTO {
	private Long id;
	private String name;
	private String status;
	private String description;
	private Boolean enabled;
	private Boolean paused;
	private Integer throttleLimit;
	private String alias;
	private Date created;
	private Date modified;
	private Boolean isTaskChainIsValid;
	private Date completed;
	private String assetIngestionTime;
	private String dataIngestionTime;
}
