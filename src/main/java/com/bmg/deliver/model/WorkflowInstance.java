package com.bmg.deliver.model;

import com.bmg.deliver.enums.DeliveryType;
import com.bmg.deliver.enums.Priority;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import jakarta.persistence.*;
import java.util.Date;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@Table(name = "workflow_instance")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowInstance {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "workflow_id")
	private Workflow workflow;

	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	private WorkflowInstanceStatus status;

	@Column(name = "completed")
	private Date completed;

	@Column(name = "duration")
	private Long duration;

	@Column(name = "trigger_data")
	private String triggerData;

	@Column(name = "log")
	private String log;

	@Column(name = "identifier")
	private String identifier;

	@Column(name = "error_message")
	private String errorMessage;

	@Column(name = "priority")
	@Enumerated(EnumType.STRING)
	private Priority priority;

	@Column(name = "delivery_type")
	@Enumerated(EnumType.STRING)
	private DeliveryType deliveryType;

	@Column(name = "started")
	private Date started;

	@CreatedDate
	@Column(name = "created", updatable = false)
	private Date created;

	@LastModifiedDate
	@Column(name = "modified", updatable = false)
	private Date modified;
}
