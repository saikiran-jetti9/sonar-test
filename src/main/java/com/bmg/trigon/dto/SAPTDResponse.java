package com.bmg.trigon.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SAPTDResponse {
  private String __count;
  private List<SAPTDResponseData> results =
      new ArrayList<>(); // Initialize to avoid NullPointerException

  public void merge(SAPTDResponse other, List<String> urriValues) {
    if (other == null || urriValues == null || urriValues.isEmpty()) {
      return; // No merging is needed if the other response or URRI values are null or empty
    }

    if (other.results != null && !other.results.isEmpty()) {
      Set<String> urriSet = new HashSet<>(urriValues); // Convert to set for efficient lookups

      // Filter and add results efficiently
      for (SAPTDResponseData saptdResponseData : other.results) {
        String urri = saptdResponseData.getAccountingDocumentHeaderText();
        if (urri != null && urriSet.contains(urri)) {
          this.results.add(saptdResponseData);
        }
      }
    }

    // Optimize count update using StringBuilder and parse checks
    updateCount(other.__count);
  }

  private void updateCount(String otherCount) {
    try {
      int currentCount = this.__count != null ? Integer.parseInt(this.__count) : 0;
      int additionalCount = otherCount != null ? Integer.parseInt(otherCount) : 0;
      this.__count = String.valueOf(currentCount + additionalCount);
    } catch (NumberFormatException e) {
      // Log or handle exception if counts are not parsable as integers
      // Consider logging here to handle the error properly
    }
  }
}
