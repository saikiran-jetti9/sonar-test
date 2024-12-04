package com.bmg.deliver.model.product;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Contributor {
	private long contributorId;
	private String entityStatus;
	private String lockedStatus;
	private String businessUnit;
	private String groupType;
	private String firstName;
	private String lastName;
	private String fullName;
	// private String repertoireOwners;
	private List<ExternalCode> externalCodes = new ArrayList<>();
	private String partyReference;
	private String ppiCategory;

	@Data
	public static class ExternalCode {
		private String externalCodeText;
		private String externalCodeType;
	}

	@Data
	public static class ArtistRole {
		private boolean isPrimary;
		private String name;
		private boolean isArtist;
		private boolean ppiPayable;
		private boolean isDisplay;
	}
}
