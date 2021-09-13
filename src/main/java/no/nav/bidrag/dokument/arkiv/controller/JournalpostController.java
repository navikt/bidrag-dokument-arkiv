package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.commons.KildesystemIdenfikator.PREFIX_JOARK_COMPLETE;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import no.nav.bidrag.commons.KildesystemIdenfikator;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.bidrag.dokument.dto.JournalpostResponse;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ProtectedWithClaims(issuer = ISSUER)
public class JournalpostController {

  private static final Logger LOGGER = LoggerFactory.getLogger(JournalpostController.class);

  private final JournalpostService journalpostService;

  public JournalpostController(JournalpostService journalpostService) {
    this.journalpostService = journalpostService;
  }

  @GetMapping("/journal/{joarkJournalpostId}")
  @Operation(
      description = "Hent en journalpost for en id på formatet '" + PREFIX_JOARK_COMPLETE + "<journalpostId>'",
      security = {@SecurityRequirement(name = "bearer-key")}
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Journalpost er hentet"),
      @ApiResponse(responseCode = "400", description = "Journalpost som skal hentes er ikke koblet mot gitt saksnummer, eller det er feil prefix/id på journalposten", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "Du mangler eller har ugyldig sikkerhetstoken", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "Du mangler eller har ugyldig sikkerhetstoken", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som skal hentes", content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<JournalpostResponse> hentJournalpost(
      @PathVariable String joarkJournalpostId,
      @RequestParam(required = false) String saksnummer
  ) {
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(joarkJournalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix() || erIkkePrefixetMedJoark(joarkJournalpostId)) {
      return ResponseEntity.badRequest().build();
    }

    var journalpostId = kildesystemIdenfikator.hentJournalpostId();
    if (journalpostId == null) {
      return ResponseEntity.badRequest().build();
    }

    return journalpostService.hentJournalpost(journalpostId, saksnummer)
        .map(journalpost -> ResponseEntity.ok(journalpost.tilJournalpostResponse()))
        .orElse(ResponseEntity.badRequest().build());
  }

  private boolean erIkkePrefixetMedJoark(String joarkJournalpostId) {
    return !joarkJournalpostId.startsWith(PREFIX_JOARK_COMPLETE);
  }

  @GetMapping("/sak/{saksnummer}/journal")
  @Operation(description = "Finn journalposter for et saksnummer og fagområde. Parameter fagomrade=BID er bidragjournal og fagomrade=FAR er farskapsjournal")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Liste over journalposter for saksnummer og fagområde"),
      @ApiResponse(responseCode = "401", description = "Du mangler eller har ugyldig sikkerhetstoken", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "Du har ikke rettigheter til å hentede data", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalposter for oppgitt sak og fagområde", content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<List<JournalpostDto>> hentJournal(@PathVariable String saksnummer, @RequestParam String fagomrade) {
    return ResponseEntity.ok(journalpostService.finnJournalposter(saksnummer, fagomrade));
  }

  @PutMapping("/journal/{joarkJournalpostId}")
  @Operation(description = "endre eksisterende journalpost med journalpostId på formatet '" + PREFIX_JOARK_COMPLETE + "<journalpostId>'")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "203", description = "Journalpost er endret"),
      @ApiResponse(responseCode = "400", description = "Prefiks på journalpostId er ugyldig, JournalpostEndreJournalpostCommandDto.gjelder er ikke satt eller det ikke finnes en journalpost på gitt id"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken er ikke gyldig"),
      @ApiResponse(responseCode = "403", description = "Sikkerhetstoke1n er ikke gyldig, eller det er ikke gitt adgang til kode 6 og 7 (nav-ansatt)"),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som skal endres, ingen 'payload' eller feil prefix/id på journalposten")
  })
  public ResponseEntity<Void> put(
      @RequestBody EndreJournalpostCommand endreJournalpostCommand,
      @PathVariable String joarkJournalpostId,
      @RequestHeader(EnhetFilter.X_ENHET_HEADER) String enhet
  ) {
    LOGGER.info("api: put /journal/{}, body: {}", joarkJournalpostId, endreJournalpostCommand);
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(joarkJournalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix() ||
        endreJournalpostCommand == null ||
        endreJournalpostCommand.getGjelder() == null
    ) {
      LOGGER.warn("Id har ikke riktig prefix: {} eller det mangler gjelder person {}", joarkJournalpostId, endreJournalpostCommand);
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    var endreJournalpostHttpResponse = journalpostService.endre(
        kildesystemIdenfikator.hentJournalpostId(), new EndreJournalpostCommandIntern(endreJournalpostCommand, enhet)
    );
    return new ResponseEntity<>(endreJournalpostHttpResponse.getResponseEntity().getStatusCode());
  }

  @Unprotected
  @GetMapping("/")
  @Operation(description = "Welcome Home Page")
  public String index() {
    return "Welcome to Bidrag Dokument Arkiv: Microservice for integration with JOARK for bidrag-dokument.";
  }
}
