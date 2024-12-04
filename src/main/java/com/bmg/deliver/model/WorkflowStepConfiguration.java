package com.bmg.deliver.model;

import jakarta.persistence.*;
import java.util.Date;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@Table(name = "workflow_step_configuration")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowStepConfiguration {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "workflow_step_id")
	private WorkflowStep workflowStep;

	@Column(name = "key")
	private String key;

	@Column(name = "value")
	private String value;

	@CreatedDate
	@Column(name = "created")
	private Date created;

	@LastModifiedDate
	@Column(name = "modified")
	private Date modified;
}
