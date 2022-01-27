package no.nav.bidrag.dokument.arkiv.aop;

import no.nav.bidrag.dokument.arkiv.model.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class HttpStatusRestControllerAdvice {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpStatusRestControllerAdvice.class);


  @ResponseBody
  @ExceptionHandler
  public ResponseEntity<?> handleOtherExceptions(Exception exception) {
    LOGGER.error(exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .header(HttpHeaders.WARNING, exception.getMessage())
        .build();
  }

  @ResponseBody
  @ExceptionHandler
  public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException illegalArgumentException) {
    LOGGER.warn(illegalArgumentException.getMessage());

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .header(HttpHeaders.WARNING, illegalArgumentException.getMessage())
        .build();
  }

  @ResponseBody
  @ExceptionHandler
  public ResponseEntity<?> handleHttpStatusException(HttpStatusException httpStatusException) {
    LOGGER.warn(httpStatusException.getMessage());

    return ResponseEntity
        .status(httpStatusException.getStatus())
        .header(HttpHeaders.WARNING, httpStatusException.getMessage())
        .build();
  }

  @ResponseBody
  @ExceptionHandler
  public ResponseEntity<?> handleHttClientErrorException(HttpClientErrorException httpClientErrorException) {
    return ResponseEntity
        .status(httpClientErrorException.getStatusCode())
        .header(HttpHeaders.WARNING, "Http client says: " + httpClientErrorException.getMessage())
        .build();
  }
}
