package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.commons.KildesystemIdenfikator.PREFIX_JOARK_COMPLETE;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;

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
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.service.EndreJournalpostService;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.bidrag.dokument.dto.JournalpostResponse;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Protected
public class JournalpostController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(JournalpostController.class);
  private final JournalpostService journalpostService;
  private final EndreJournalpostService endreJournalpostService;

  public JournalpostController(ResourceByDiscriminator<JournalpostService> journalpostService, EndreJournalpostService endreJournalpostService) {
    this.journalpostService = journalpostService.get(Discriminator.REGULAR_USER);
    this.endreJournalpostService = endreJournalpostService;
  }

  @GetMapping(ROOT_JOURNAL+"/{joarkJournalpostId}")
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

    LOGGER.info("Henter journalpost {} med saksnummer {}", joarkJournalpostId, saksnummer);
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(joarkJournalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix() || erIkkePrefixetMedJoark(joarkJournalpostId)) {
      return ResponseEntity
          .badRequest()
          .header(HttpHeaders.WARNING, "Ukjent prefix på journalpostId: " + joarkJournalpostId)
          .build();
    }

    var journalpostId = kildesystemIdenfikator.hentJournalpostId();
    if (journalpostId == null) {
      return ResponseEntity
          .badRequest()
          .header(HttpHeaders.WARNING, "Kunne ikke hente id fra prefikset journalpostId: " + joarkJournalpostId)
          .build();
    }

    return journalpostService.hentJournalpostMedFnrOgTilknyttedeSaker(Long.valueOf(journalpostId), saksnummer)
        .map(journalpost -> ResponseEntity.ok(journalpost.tilJournalpostResponse()))
        .orElse(ResponseEntity.notFound()
            .header(HttpHeaders.WARNING, String.format("Fant ingen journalpost med id %s og saksnummer %s", journalpostId, saksnummer))
            .build());
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
    LOGGER.info("Henter journal for saksnummer {} og tema {}", saksnummer, fagomrade);
    return ResponseEntity.ok(journalpostService.finnJournalposter(saksnummer, fagomrade));
  }

  @PatchMapping(ROOT_JOURNAL+"/{joarkJournalpostId}")
  @Operation(description = "endre eksisterende journalpost med journalpostId på formatet '" + PREFIX_JOARK_COMPLETE + "<journalpostId>'")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "203", description = "Journalpost er endret"),
      @ApiResponse(responseCode = "400", description = "Prefiks på journalpostId er ugyldig, JournalpostEndreJournalpostCommandDto.gjelder er ikke satt eller det ikke finnes en journalpost på gitt id"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken er ikke gyldig"),
      @ApiResponse(responseCode = "403", description = "Sikkerhetstoke1n er ikke gyldig, eller det er ikke gitt adgang til kode 6 og 7 (nav-ansatt)"),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som skal endres, ingen 'payload' eller feil prefix/id på journalposten")
  })
  public ResponseEntity<Void> patch(
      @RequestBody EndreJournalpostCommand endreJournalpostCommand,
      @PathVariable String joarkJournalpostId,
      @RequestHeader(EnhetFilter.X_ENHET_HEADER) String enhet
  ) {
    LOGGER.info("Mottatt oppdater journalpost {} kall", joarkJournalpostId);
    SECURE_LOGGER.info("Oppdater journalpost {} med body: {}", joarkJournalpostId, endreJournalpostCommand);
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(joarkJournalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix() ||
        endreJournalpostCommand == null
    ) {
      var msgBadRequest = String.format(
          "Id har ikke riktig prefix: %s eller det mangler gjelder person %s", joarkJournalpostId, endreJournalpostCommand
      );

      LOGGER.warn(msgBadRequest);

      return ResponseEntity
          .badRequest()
          .header(HttpHeaders.WARNING, msgBadRequest)
          .build();
    }

    var endreJournalpostHttpResponse = endreJournalpostService.endre(
        Long.valueOf(kildesystemIdenfikator.hentJournalpostId()), new EndreJournalpostCommandIntern(endreJournalpostCommand, enhet)
    );

    return new ResponseEntity<>(endreJournalpostHttpResponse.getResponseEntity().getStatusCode());
  }
}
