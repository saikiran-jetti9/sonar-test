package com.bmg.deliver.model.product;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Distributor {
	private String distributorName;
	private String distributorId;
	private List<DistributionTerritory> distributorTerritories = new ArrayList<>();
	private Retailers retailers;

	@Data
	public static class Retailers {
		private List<RetailerWrapper> exclusives = new ArrayList<>();
		private List<RetailerWrapper> exclusions = new ArrayList<>();
	}

	@Data
	public static class RetailerWrapper {
		private Retailer retailer;
	}

	@Data
	public static class Retailer {
		private String retailerId;
		private String retailerName;
	}
}
