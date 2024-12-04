package com.bmg.deliver.model.product;

import com.bmg.deliver.model.ddex.Deal;
import com.bmg.deliver.model.ddex.TrackDeal;
import com.bmg.deliver.model.interfaces.StoreAsset;
import com.bmg.deliver.model.interfaces.TriggerData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Murari Sabavath
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Product implements TriggerData {
	private ReleaseProduct releaseProduct;
	private CaptureProduct captureProduct;
	private List<StoreAsset> preparedAssets = new ArrayList<>();
	private List<Deal> productDeals = new ArrayList<>();
	private List<TrackDeal> trackDeals = new ArrayList<>();
}
