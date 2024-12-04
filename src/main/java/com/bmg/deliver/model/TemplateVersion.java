package com.bmg.deliver.model;

import jakarta.persistence.*;
import java.util.Date;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@Table(name = "Template_Version")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TemplateVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "template_id", nullable = false)
	private Template template;

	@Column(name = "template_code")
	private String templateCode;

	@Column(name = "template_description")
	private String templateDescription;

	@CreatedDate
	@Column(name = "created")
	private Date created;

	@LastModifiedDate
	@Column(name = "modified")
	private Date modified;
}
