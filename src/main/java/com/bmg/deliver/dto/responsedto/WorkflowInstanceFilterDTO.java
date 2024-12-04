package com.bmg.deliver.dto.responsedto;

import com.bmg.deliver.enums.DeliveryType;
import com.bmg.deliver.enums.Priority;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.utils.AppConstants;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
public class WorkflowInstanceFilterDTO {
	@DateTimeFormat(pattern = AppConstants.DATE_FORMAT_FULL_TIMESTAMP)
	private Date startDate;

	@DateTimeFormat(pattern = AppConstants.DATE_FORMAT_FULL_TIMESTAMP)
	private Date endDate;

	@DateTimeFormat(pattern = AppConstants.DATE_FORMAT_FULL_TIMESTAMP)
	private Date completedStart;

	@DateTimeFormat(pattern = AppConstants.DATE_FORMAT_FULL_TIMESTAMP)
	private Date completedEnd;

	private List<DeliveryType> deliveryType;
	private List<WorkflowInstanceStatus> status;
	private List<Priority> priority;
	private Long duration;
	private List<String> identifier;

}
