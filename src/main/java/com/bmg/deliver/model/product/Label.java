package com.bmg.deliver.model.product;

import lombok.Data;

@Data
public class Label {
	private long id;
	private String name;
	private String gvlLabelCode;
	private String labelCode;
	private String partyReference;
}
