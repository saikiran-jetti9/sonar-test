package com.bmg.deliver.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class StatisticsDTO {
	private Long totalSuccessfulInstances;
	private Long totalFailedInstances;
	private Map<String, DeliveryTypeStatsDTO> deliveryTypeStats = new HashMap<>();

}
