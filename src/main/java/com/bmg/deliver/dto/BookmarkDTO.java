package com.bmg.deliver.dto;

import lombok.Data;

import java.util.Date;

@Data
public class BookmarkDTO {
	private Long workflowId;
	private String userName;
	private Date created;
	private Date modified;
}
