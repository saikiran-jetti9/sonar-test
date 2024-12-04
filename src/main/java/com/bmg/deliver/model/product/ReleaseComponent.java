package com.bmg.deliver.model.product;

import lombok.Data;

//import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReleaseComponent {
	private String captureId;
	private String format;
	private long componentNumber;
	private List<ReleaseTrack> tracks = new ArrayList<>();
	private List<ProductAsset> componentAssets = new ArrayList<>();
}
