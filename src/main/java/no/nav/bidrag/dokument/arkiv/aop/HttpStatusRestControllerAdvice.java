package no.nav.bidrag.dokument.arkiv.aop;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class HttpStatusRestControllerAdvice {

  @ResponseBody
  @ExceptionHandler
  public ResponseEntity<?> handleHttClientErrorException(HttpClientErrorException httpClientErrorException) {
    return ResponseEntity
        .status(httpClientErrorException.getStatusCode())
        .header(HttpHeaders.WARNING, "Http client says: " + httpClientErrorException.getMessage())
        .build();
  }
}
