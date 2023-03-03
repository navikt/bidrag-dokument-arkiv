package no.nav.bidrag.dokument.arkiv.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import no.nav.bidrag.commons.util.KildesystemIdenfikator;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequestInternal;
import no.nav.bidrag.dokument.arkiv.service.DistribuerJournalpostService;
import no.nav.bidrag.dokument.dto.DistribuerJournalpostRequest;
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse;
import no.nav.security.token.support.core.api.Protected;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Protected
public class DistribuerController extends BaseController {
  private static final Logger LOGGER = LoggerFactory.getLogger(DistribuerController.class);

  private final DistribuerJournalpostService distribuerJournalpostService;

  public DistribuerController(DistribuerJournalpostService distribuerJournalpostService) {
    this.distribuerJournalpostService = distribuerJournalpostService;
  }

  @PostMapping(ROOT_JOURNAL+"/distribuer/{joarkJournalpostId}")
  @Operation(description = "Bestill distribusjon av journalpost")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Distribusjon av journalpost er bestilt"),
      @ApiResponse(responseCode = "400", description = "Journalpost mangler mottakerid eller adresse er ikke oppgitt i kallet"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken er ikke gyldig"),
      @ApiResponse(responseCode = "403", description = "Sikkerhetstoke1n er ikke gyldig, eller det er ikke gitt adgang til kode 6 og 7 (nav-ansatt)"),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som skal distribueres")
  })
  @ResponseBody
  public ResponseEntity<DistribuerJournalpostResponse> distribuerJournalpost(
      @RequestBody(required = false) DistribuerJournalpostRequest distribuerJournalpostRequest,
      @PathVariable String joarkJournalpostId,
      @RequestParam(required = false, name = "batchId") String batchIdHeader
  ) {
    var batchId = Strings.isEmpty(batchIdHeader) ? null : batchIdHeader;
    LOGGER.info("Distribuerer journalpost {}{}", joarkJournalpostId, Strings.isNotEmpty(batchId) ? String.format(" og batchId %s", batchId) : "");
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(joarkJournalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      var msgBadRequest = String.format("Id har ikke riktig prefix: %s", joarkJournalpostId);

      LOGGER.warn(msgBadRequest);

      return ResponseEntity
          .badRequest()
          .header(HttpHeaders.WARNING, msgBadRequest)
          .build();
    }

    var journalpostId = kildesystemIdenfikator.hentJournalpostIdLong();
    return ResponseEntity.ok(distribuerJournalpostService.distribuerJournalpost(journalpostId, batchId, new DistribuerJournalpostRequestInternal(distribuerJournalpostRequest)));
  }

  @GetMapping(ROOT_JOURNAL+"/distribuer/{journalpostId}/enabled")
  @Operation(description = "Sjekk om distribusjon av journalpost kan bestilles")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Distribusjon av journalpost kan bestilles"),
      @ApiResponse(responseCode = "406", description = "Distribusjon av journalpost kan ikke bestilles"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken er ikke gyldig"),
      @ApiResponse(responseCode = "403", description = "Sikkerhetstoke1n er ikke gyldig, eller det er ikke gitt adgang til kode 6 og 7 (nav-ansatt)"),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som skal distribueres")
  })
  @ResponseBody
  public ResponseEntity<Void> kanDistribuerJournalpost(@PathVariable String journalpostId) {
    LOGGER.info("Sjekker om journalpost {} kan distribueres", journalpostId);
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(journalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      var msgBadRequest = String.format("Id har ikke riktig prefix: %s", journalpostId);

      LOGGER.warn(msgBadRequest);

      return ResponseEntity
          .badRequest()
          .header(HttpHeaders.WARNING, msgBadRequest)
          .build();
    }

    try {
      distribuerJournalpostService.kanDistribuereJournalpost(kildesystemIdenfikator.hentJournalpostIdLong());
      return ResponseEntity.ok().build();
    } catch (IllegalArgumentException e){
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
          .header(HttpHeaders.WARNING, e.getMessage())
          .build();
    }
  }
}
