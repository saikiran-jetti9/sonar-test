package com.bmg.deliver.model.ddex;

import com.bmg.deliver.model.product.DistributionTerritory;
import com.bmg.deliver.model.product.ReleaseTrack;
import com.bmg.deliver.utils.AppConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Deal {
	private String groupName;
	private boolean productDeal = true;
	private boolean takeDownDeal = false;
	private boolean adStream = false;
	private boolean stream = false;
	private boolean download = false;
	private boolean preOrder = false;
	private boolean instantGrat = false;
	private String takeDownDate;
	private String takeDownDateTime;
	private String dealCurrency;
	private String releaseDate;
	private String releaseDateTime;
	private String priceCode;
	private String shortsStartDate;
	private String shortsStartDateTime;
	private String distributorName;
	private String captureTrackId;
	private String preOrderDate;
	private String preOrderDateTime;
	private String instantGratOverrideDate;
	private String instantGratOverrideDateTime;
	private String instantGratPeriodStartDate;
	private String instantGratPeriodStartDateTime;
	private String instantGratPeriodEndDate;
	private String instantGratPeriodEndDateTime;
	private List<String> exclusives;
	private List<String> exclusions;
	private Set<String> includedCountries = new LinkedHashSet<>();
	private Set<String> excludedCountries = new LinkedHashSet<>();
	private List<Long> captureTrackIdsForInstantGratTracks = new ArrayList<>();
	private List<String> instantGratTrackRefs = new ArrayList<>();

	public static Deal createDefaultProductDeal(DistributionTerritory territory, String distributorName,
			List<String> exclusives, List<String> exclusions) {
		String preOrderDate = territory.getPreOrderDate();
		String priceCode = territory.getPartnerPriceCode();
		String currency = territory.getCurrency();
		String releaseDate = territory.getReleaseDate();
		String isoCode = territory.getIsoCode();
		boolean isAvailableForSale = null != territory.getIsAvailableForSale() && territory.getIsAvailableForSale();

		Deal deal = new Deal();
		deal.setProductDeal(true);
		deal.setDistributorName(distributorName);
		deal.setPriceCode(priceCode);
		deal.setDealCurrency(currency);
		deal.setExclusives(exclusives);
		deal.setExclusions(exclusions);
		deal.includedCountries.add(isoCode);
		deal.setExcludedCountries(AppConstants.getAllCountries());

		if (null != preOrderDate && !preOrderDate.isEmpty()) {
			deal.setPreOrderDate(preOrderDate);
			deal.setPreOrderDateTime(deal.convertDateTime(preOrderDate));
		}

		if (isAvailableForSale) {
			deal.setReleaseDate(releaseDate);
			String releaseDateTime = deal.convertDateTime(releaseDate);
			deal.setReleaseDateTime(releaseDateTime);
		} else {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT_YYYY_MM_DD);
			String takeDownDate = LocalDate.now().format(formatter);
			DateTimeFormatter takeDowndateTimeformatter = DateTimeFormatter
					.ofPattern(AppConstants.DATE_FORMAT_FULL_TIMESTAMP);
			String takeDownDateTime = LocalDateTime.now().format(takeDowndateTimeformatter);
			deal.setReleaseDate(takeDownDate);
			deal.setReleaseDateTime(takeDownDateTime);
			deal.setTakeDownDate(takeDownDate);
			deal.setTakeDownDateTime(takeDownDateTime);
			deal.setTakeDownDeal(true);
			deal.setPriceCode(null);
			deal.setDealCurrency(null);
		}
		return deal;
	}

	/**
	 * Creates a default track deal for a given track and territory with common
	 * parameters
	 */
	public static Deal createDefaultTrackDeal(Deal dealGroup, ReleaseTrack track,
			DistributionTerritory trackTerritory) {
		Deal deal = clone(dealGroup);
		String shortsStartDate = null;
		if (null != trackTerritory.getPartnerPriceCode()) {
			deal.priceCode = trackTerritory.getPartnerPriceCode();
		}

		if (null != track.getShorts()
				&& (null != track.getShorts().getStartDate() || null != track.getShorts().getStartDateTimeUTC())) {
			if (null != track.getShorts().getStartTime()) {
				shortsStartDate = track.getShorts().getStartDate();
			} else {
				SimpleDateFormat isoFormat = new SimpleDateFormat(AppConstants.DATE_FORMAT_ISO_8601);
				isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

				try {
					Date date = isoFormat.parse(track.getShorts().getStartDateTimeUTC());
					SimpleDateFormat dateFormat = new SimpleDateFormat(AppConstants.DATE_FORMAT_YYYY_MM_DD);
					shortsStartDate = dateFormat.format(date);
				} catch (Exception e) {
					log.error("Exception while formatting shorts start date {}", ExceptionUtils.getStackTrace(e));
				}
			}
		}

		if (track.isInstantGrat() && null != track.getInstantGratOverride()) {
			deal.setInstantGrat(true);
			deal.setInstantGratOverrideDate(track.getInstantGratOverride());
		}

		deal.setProductDeal(false);
		deal.setShortsStartDate(shortsStartDate);
		String shortsStartDateTime = deal.convertDateTime(shortsStartDate);
		deal.setShortsStartDateTime(shortsStartDateTime);

		if (deal.isTakeDownDeal()) {
			deal.setPreOrder(false);
			deal.setInstantGrat(false);
			deal.setPreOrderDate(null);
			deal.setPriceCode(null);
			deal.setPreOrderDateTime(null);
			deal.setInstantGratOverrideDate(null);
			deal.setInstantGratOverrideDateTime(null);
			deal.setInstantGratPeriodStartDate(null);
			deal.setInstantGratPeriodStartDateTime(null);
			deal.setInstantGratPeriodEndDate(null);
			deal.setInstantGratPeriodEndDateTime(null);
			deal.setShortsStartDate(null);
			deal.setShortsStartDateTime(null);
		}
		return deal;
	}

	public static Deal clone(Deal deal) {
		Deal clone = new Deal();
		clone.setProductDeal(deal.isProductDeal());
		clone.setInstantGrat(deal.isInstantGrat());
		clone.setTakeDownDeal(deal.isTakeDownDeal());
		clone.setStream(deal.isStream());
		clone.setAdStream(deal.isAdStream());
		clone.setDistributorName(deal.getDistributorName());
		clone.setPriceCode(deal.getPriceCode());
		clone.setReleaseDate(deal.getReleaseDate());
		clone.setReleaseDateTime(deal.getReleaseDateTime());
		clone.setPreOrderDate(deal.getPreOrderDate());
		clone.setPreOrderDateTime(deal.getPreOrderDateTime());
		clone.setDealCurrency(deal.getDealCurrency());
		clone.setTakeDownDate(deal.getTakeDownDate());
		clone.setTakeDownDateTime(deal.getTakeDownDateTime());
		clone.setShortsStartDate(deal.getShortsStartDate());
		clone.setShortsStartDateTime(deal.getShortsStartDateTime());
		clone.setInstantGratOverrideDate(deal.getInstantGratOverrideDate());
		clone.setInstantGratOverrideDateTime(deal.getInstantGratOverrideDateTime());
		clone.setInstantGratPeriodStartDate(deal.getInstantGratPeriodStartDate());
		clone.setInstantGratPeriodStartDateTime(deal.getInstantGratPeriodStartDateTime());
		clone.setInstantGratPeriodEndDate(deal.getInstantGratPeriodEndDate());
		clone.setInstantGratPeriodEndDateTime(deal.getInstantGratPeriodEndDateTime());

		List<String> exclusives = new ArrayList<>(deal.getExclusives());
		clone.setExclusives(exclusives);
		List<String> exclusions = new ArrayList<>(deal.getExclusions());
		clone.setExclusions(exclusions);
		Set<String> includedCountries = new LinkedHashSet<>(deal.getIncludedCountries());
		clone.setIncludedCountries(includedCountries);
		Set<String> excludedCountries = new LinkedHashSet<>(deal.getExcludedCountries());
		clone.setExcludedCountries(excludedCountries);
		return clone;
	}

	/**
	 * Creates a download deal for a given deal group with common parameters
	 */
	public static Deal createDownloadDeal(Deal defaultDeal, String groupName) {
		Deal deal = clone(defaultDeal);
		deal.setGroupName(groupName);
		deal.setDownload(true);
		return deal;
	}

	public static Deal createStreamDeal(Deal defaultDeal, String groupName, boolean isStream, boolean isAdStream) {
		Deal deal = clone(defaultDeal);
		deal.setGroupName(groupName);
		deal.setStream(isStream);
		deal.setAdStream(isAdStream);
		deal.setPriceCode(null);
		return deal;
	}

	public static Deal createDownloadPreorderDeal(Deal defaultDeal, String groupName, String instantGratPeriodStartDate,
			String instantGratPeriodEndDate) {
		Deal deal = clone(defaultDeal);
		deal.setGroupName(groupName);
		deal.setDownload(true);
		deal.setPreOrder(true);
		deal.setInstantGrat(true);
		deal.setInstantGratPeriodStartDate(instantGratPeriodStartDate);
		deal.setInstantGratPeriodStartDateTime(deal.convertDateTime(instantGratPeriodStartDate));
		deal.setInstantGratPeriodEndDate(instantGratPeriodEndDate);
		deal.setInstantGratPeriodEndDateTime(deal.convertDateTime(instantGratPeriodEndDate));
		return deal;
	}

	public static Deal createDownloadInstantGratDeal(Deal defaultDeal, String groupName, String igStartDate) {
		Deal dealGroup = clone(defaultDeal);
		dealGroup.setGroupName(groupName);
		dealGroup.setProductDeal(true);
		dealGroup.setDownload(true);
		dealGroup.setPreOrder(true);
		dealGroup.setInstantGrat(true);
		dealGroup.setInstantGratPeriodStartDate(igStartDate);
		dealGroup.setInstantGratPeriodStartDateTime(dealGroup.convertDateTime(igStartDate));
		return dealGroup;
	}

	/**
	 * Checks if the deal can be merged with another deal
	 */
	public boolean canMergeWith(Deal deal) {
		boolean priceCodesAndReleaseDatesMatch = isPriceCodesAndReleaseDatesMatch(deal);
		if (this.productDeal) {
			return priceCodesAndReleaseDatesMatch;
		}

		// Track deals needs additional instantGrat and shorts start date checks
		boolean instantGratMatch = this.instantGrat == deal.isInstantGrat()
				&& Objects.equals(this.instantGratOverrideDate, deal.getInstantGratOverrideDate());

		boolean shortsStartDateMatch = Objects.equals(this.shortsStartDate, deal.getShortsStartDate());

		return priceCodesAndReleaseDatesMatch && instantGratMatch && shortsStartDateMatch;
	}

	/**
	 * Download deals can be merged only if price code and release date matches
	 * Stream deals can be merged only if release date matches Take down deals are
	 * created for both download and stream deals only
	 *
	 * @param deal
	 *            The deal to be merged
	 * @return boolean
	 */
	private boolean isPriceCodesAndReleaseDatesMatch(Deal deal) {
		boolean takeDownsMatched = true;

		// takeDownDeal check is only for download and stream deals
		if (deal.isProductDeal()
				&& (AppConstants.DOWNLOAD.equals(this.groupName) || AppConstants.STREAM.equals(this.groupName))) {
			takeDownsMatched = this.takeDownDeal == deal.isTakeDownDeal();
		}
		boolean releaseDatesMatch = Objects.equals(this.releaseDate, deal.getReleaseDate()) && takeDownsMatched;

		// Price code check is only for download deals
		boolean priceCodesMatch = !this.download
				|| (null != this.priceCode && !this.priceCode.isEmpty() && this.priceCode.equals(deal.priceCode));

		// Even though Take down deals are download deals, they can be merged with other
		// deals if the release date matches
		return this.takeDownDeal ? releaseDatesMatch : releaseDatesMatch && priceCodesMatch;
	}

	/**
	 * Check if incoming deal can be merged with the current deal Then, if they can
	 * be merged, add the included and excluded countries from the incoming deal
	 *
	 * @param deal
	 *            The deal to be merged
	 * @return boolean if the deals are merged
	 */
	public boolean merge(Deal deal) {
		if (canMergeWith(deal)) {
			this.includedCountries.addAll(deal.getIncludedCountries());
			this.excludedCountries.removeAll(deal.getIncludedCountries());
			return true;
		}
		return false;
	}

	public String convertDateTime(String onlyDate) {
		if (null == onlyDate || onlyDate.isEmpty()) {
			return null;
		}
		LocalDate date = LocalDate.parse(onlyDate, DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT_YYYY_MM_DD));
		LocalDateTime dateTime = date.atStartOfDay();
		DateTimeFormatter dateTimeformatter = DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT_ISO_LOCAL_DATE_TIME);
		return dateTime.format(dateTimeformatter);
	}
}
