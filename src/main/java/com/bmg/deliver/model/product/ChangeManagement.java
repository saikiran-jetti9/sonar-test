package com.bmg.deliver.model.product;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChangeManagement {
	private String confirmedDate;
	private String confirmedUser;
	private String changeReason;
	private List<String> changeTypes = new ArrayList<>();
	private List<Long> changeTypeId = new ArrayList<>();
}
