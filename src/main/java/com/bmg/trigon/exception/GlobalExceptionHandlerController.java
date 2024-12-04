package com.bmg.trigon.exception;

import com.bmg.trigon.common.dto.TrigonResponse;
import com.bmg.trigon.util.ApplicationConstants;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandlerController extends ResponseEntityExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<TrigonResponse> handleIllegalArgumentException(Exception e) {
    log.error("Exception occurred", e);
    Map<String, Object> meta = new HashMap<>();
    meta.put(ApplicationConstants.MSG, e.getMessage());
    return new ResponseEntity<>(
        TrigonResponse.builder().meta(meta).build(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<TrigonResponse> handleException(Exception e) throws IOException {
    log.error("Exception occurred", e);
    Map<String, Object> meta = new HashMap<>();
    meta.put(ApplicationConstants.MSG, e.getMessage());
    return new ResponseEntity<>(
        TrigonResponse.builder().meta(meta).build(), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
