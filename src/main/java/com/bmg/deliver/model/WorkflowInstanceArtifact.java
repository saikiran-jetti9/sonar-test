package com.bmg.deliver.model;

import jakarta.persistence.*;
import java.util.Date;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@Table(name = "workflow_instance_artifact")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowInstanceArtifact {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "workflow_instance_id")
	private WorkflowInstance workflowInstance;

	@ManyToOne
	@JoinColumn(name = "workflow_step_id")
	private WorkflowStep workflowStep;

	@Column(name = "description")
	private String description;

	@Column(name = "filename")
	private String filename;

	@Column(name = "unique_filename")
	private String uniqueFilename;

	@Column(name = "created")
	private Date created;

	@LastModifiedDate
	@Column(name = "modified")
	private Date modified;
}
