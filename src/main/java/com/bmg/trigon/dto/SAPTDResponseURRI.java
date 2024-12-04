package com.bmg.trigon.dto;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SAPTDResponseURRI {
  // Map of URRI and corresponding SAPTDResponseData
  private Map<String, List<SAPTDResponseData>> results;

  // Constructor to initialize the map
  public SAPTDResponseURRI(List<SAPTDResponseData> saptdResponseDataList) {
    this.results = new HashMap<>(); // Initializing with default capacity
    if (saptdResponseDataList != null) {
      for (SAPTDResponseData saptdResponseData : saptdResponseDataList) {
        String urri = saptdResponseData.getAccountingDocumentHeaderText();
        // Use computeIfAbsent to minimize map lookups and initialization
        this.results.computeIfAbsent(urri, k -> new ArrayList<>()).add(saptdResponseData);
      }
    }
  }

  public List<SAPTDResponseData> getResultsBasedOnURRI(String fullURRI) {
    return this.results.get(fullURRI);
  }
}
