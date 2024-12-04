package com.bmg.deliver.model;

import com.bmg.deliver.enums.EmailStatus;
import jakarta.persistence.*;
import java.util.Date;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@Table(name = "workflow_email")
@EntityListeners(AuditingEntityListener.class)
public class WorkflowEmail {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "workflow_id")
	private Workflow workflow;

	@Column(name = "email")
	private String email;

	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	private EmailStatus status;

	@Column(name = "name")
	private String name;

	@CreatedDate
	@Column(name = "created")
	private Date created;

	@LastModifiedDate
	@Column(name = "modified")
	private Date modified;
}
