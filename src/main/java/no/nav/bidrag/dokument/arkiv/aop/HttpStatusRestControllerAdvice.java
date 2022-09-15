package no.nav.bidrag.dokument.arkiv.aop;

import no.nav.bidrag.dokument.arkiv.model.HttpStatusException;
import no.nav.bidrag.dokument.arkiv.model.JournalIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.KnyttTilSakManglerTemaException;
import no.nav.bidrag.dokument.arkiv.model.KunneIkkeFerdigstilleOpprettetJournalpost;
import no.nav.bidrag.dokument.arkiv.model.OppdaterJournalpostFeiletFunksjoneltException;
import no.nav.bidrag.dokument.arkiv.model.PersonException;
import no.nav.bidrag.dokument.arkiv.model.UgyldigAvvikException;
import no.nav.bidrag.dokument.arkiv.model.ViolationException;
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException;
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
  @ExceptionHandler({PersonException.class})
  public ResponseEntity<?> handleTechnicalException(Exception exception) {
    LOGGER.warn(exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .header(HttpHeaders.WARNING, exception.getMessage())
        .build();
  }


  @ResponseBody
  @ExceptionHandler
  public ResponseEntity<?> handleViolationException(ViolationException exception) {
    LOGGER.warn(exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .header(HttpHeaders.WARNING, exception.getMessage())
        .build();
  }

  @ResponseBody
  @ExceptionHandler({KunneIkkeFerdigstilleOpprettetJournalpost.class})
  public ResponseEntity<?> handleBadRequest(Exception exception) {
    LOGGER.warn(exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .header(HttpHeaders.WARNING, exception.getMessage())
        .build();
  }

  @ResponseBody
  @ExceptionHandler
  public ResponseEntity<?> handleOtherExceptions(Exception exception) {
    LOGGER.error(exception.getMessage(), exception);

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .header(HttpHeaders.WARNING, exception.getMessage())
        .build();
  }

  @ResponseBody
  @ExceptionHandler(JwtTokenUnauthorizedException.class)
  public ResponseEntity<?> handleUnauthorizedException(Exception exception) {
    LOGGER.warn(exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
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
  @ExceptionHandler({KnyttTilSakManglerTemaException.class, OppdaterJournalpostFeiletFunksjoneltException.class, UgyldigAvvikException.class})
  public ResponseEntity<?> ugyldigInput(Exception exception) {
    LOGGER.warn(exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .header(HttpHeaders.WARNING, exception.getMessage())
        .build();
  }

  @ResponseBody
  @ExceptionHandler({JournalIkkeFunnetException.class, JournalpostIkkeFunnetException.class})
  public ResponseEntity<?> journalpostIkkeFunnet(Exception exception) {
    LOGGER.warn(exception.getMessage());

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .header(HttpHeaders.WARNING, exception.getMessage())
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
