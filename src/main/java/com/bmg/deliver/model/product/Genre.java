package com.bmg.deliver.model.product;

import lombok.Data;

@Data
public class Genre {
	private String name;
	private String subGenreName;
	private boolean hasPartnerGenre;
	private boolean hasPartnerSubGenre;
	private boolean isClassical;
}
