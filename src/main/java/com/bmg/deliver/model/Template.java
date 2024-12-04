package com.bmg.deliver.model;

import jakarta.persistence.*;
import java.util.Date;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@Table(name = "Template")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Template {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name")
	private String name;

	@Column(name = "description")
	private String description;

	@Column(name = "primary_version_id")
	private Long primaryVersionId;

	@CreatedDate
	@Column(name = "created")
	private Date created;

	@LastModifiedDate
	@Column(name = "modified")
	private Date modified;
}