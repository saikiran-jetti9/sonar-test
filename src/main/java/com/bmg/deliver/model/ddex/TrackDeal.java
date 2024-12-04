package com.bmg.deliver.model.ddex;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
public class TrackDeal {
	private final List<Deal> deals;
	private final Set<String> resourceValues;

	public TrackDeal(List<Deal> deals, Set<String> resourceValues) {
		this.deals = deals;
		this.resourceValues = resourceValues;
	}

	/**
	 * If every deal in the incoming TrackDeal can be merged with a deal in this,
	 * then merge the incoming TrackDeal with this TrackDeal.
	 *
	 * @param toMerge
	 *            the TrackDeal to merge with this TrackDeal
	 * @return true if the merge was successful, false otherwise
	 */
	public boolean merge(TrackDeal toMerge) {
		List<Deal> remainingDeals = new ArrayList<>(deals);

		for (Deal deal : toMerge.deals) {
			for (Deal remainingDeal : remainingDeals) {
				if (deal.merge(remainingDeal)) {
					remainingDeals.remove(remainingDeal);
					break;
				}
			}
		}

		if (remainingDeals.isEmpty()) {
			this.resourceValues.addAll(toMerge.resourceValues);
			return true;
		}
		return false;
	}
}
