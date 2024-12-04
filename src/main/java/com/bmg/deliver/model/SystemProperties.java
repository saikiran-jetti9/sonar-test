package com.bmg.deliver.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@Entity
@Data
@Table(name = "system_properties")
@EntityListeners(AuditingEntityListener.class)
public class SystemProperties {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "key")
	private String key;

	@Column(name = "value")
	private String value;

	@Column(name = "description")
	private String description;

	@CreatedDate
	@Column(name = "created")
	private Date created;

	@LastModifiedDate
	@Column(name = "modified")
	private Date modified;
}
