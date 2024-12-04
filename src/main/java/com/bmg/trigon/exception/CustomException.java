package com.bmg.trigon.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomException extends RuntimeException {

  private final String apiUrl;
  private final int statusCode;
  private final String errorMessage;

  public CustomException(String apiUrl, int statusCode, String errorMessage) {
    this.apiUrl = apiUrl;
    this.statusCode = statusCode;
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return errorMessage + "- Status Code :" + statusCode;
  }
}
