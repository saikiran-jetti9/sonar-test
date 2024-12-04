package com.bmg.deliver.model.product;

import lombok.Data;

@Data
public class DistributionTerritory {
	private String isoCode;
	private String releaseDate;
	private String currency;
	private String preOrderDate;
	private String permanentDownloadDate;
	private String subscriptionStreamingDate;
	private String adSupportedStreamingDate;
	private String euroPpdBmgCode;
	private String gbpPpdBmgCode;
	private String usdPpdBmgCode;
	private Boolean isAvailableForSale;
	private String partnerPriceCode;
	private String resourceValue;
	// private String partnerPricing;
	// private List<Distributor> distributors = new ArrayList<>();
}
