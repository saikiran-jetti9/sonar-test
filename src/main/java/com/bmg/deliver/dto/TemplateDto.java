package com.bmg.deliver.dto;

import com.bmg.deliver.model.Template;
import lombok.Data;

@Data
public class TemplateDto {
	private String name;
	private String description;
	private String templateCode;
	private Template template;
	private String templateDescription;
}
