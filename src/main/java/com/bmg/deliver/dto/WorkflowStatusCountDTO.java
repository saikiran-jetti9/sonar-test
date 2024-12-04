/**
 * @author dattatreya
 **/
package com.bmg.deliver.dto;

import lombok.Data;

import java.util.Date;

/**
 *
 * @author dattatreya
 **/
@Data
public class WorkflowStatusCountDTO {
	private TotalStatusCountDTO totalInstances;
	private String workflowName;
	private Long workflowId;
	private boolean isPaused;
	private Date lastRun;
	private Date completed;
	private Date started;
}
