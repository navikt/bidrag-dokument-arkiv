package no.nav.bidrag.dokument.arkiv.controller;


import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Optional;
import no.nav.bidrag.commons.util.KildesystemIdenfikator;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.dokument.arkiv.dto.AvvikshendelseIntern;
import no.nav.bidrag.dokument.arkiv.service.AvvikService;
import no.nav.bidrag.transport.dokument.AvvikType;
import no.nav.bidrag.transport.dokument.Avvikshendelse;
import no.nav.bidrag.transport.dokument.BehandleAvvikshendelseResponse;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Protected
public class AvvikController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(AvvikController.class);

  private final AvvikService avvikService;

  public AvvikController(AvvikService avvikService) {
    this.avvikService = avvikService;
  }

  @GetMapping(ROOT_JOURNAL + "/{journalpostId}/avvik")
  @Operation(
      security = {@SecurityRequirement(name = "bearer-key")},
      summary = "Henter mulige avvik for en journalpost, id på formatet '" + KildesystemIdenfikator.PREFIX_JOARK + "<journalpostId>'"
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Tilgjengelig avvik for journalpost er hentet"),
      @ApiResponse(responseCode = "401", description = "Du mangler sikkerhetstoken", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "Sikkerhetstoken er ikke gyldig", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som det skal hentes avvik på", content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<List<AvvikType>> hentAvvik(
      @PathVariable String journalpostId,
      @Parameter(name = "saksnummer", description = "journalposten tilhører sak") @RequestParam(required = false) String saksnummer
  ) {
    var muligSak = Optional.ofNullable(saksnummer);

    if (muligSak.isPresent()) {
      LOGGER.info("GET: journal/{}/avvik?saksnummer={}", journalpostId, saksnummer);
    } else {
      LOGGER.info("GET: /journal/{}/avvik", journalpostId);
    }

    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(journalpostId);
    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      return new ResponseEntity<>(initHttpHeadersWith(HttpHeaders.WARNING, "Ugyldig prefix på journalpostId"), HttpStatus.BAD_REQUEST);
    }

    return ResponseEntity.ok(avvikService.hentAvvik(Long.valueOf(kildesystemIdenfikator.hentJournalpostId())));
  }

  @PostMapping(value = ROOT_JOURNAL + "/{journalpostId}/avvik", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      security = {@SecurityRequirement(name = "bearer-key")},
      summary = "Behandler et avvik for en journalpost, id på formatet '" + KildesystemIdenfikator.PREFIX_JOARK_COMPLETE + "<journalpostId>'"
  )
  @Transactional
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Avvik på journalpost er behandlet"),
      @ApiResponse(responseCode = "400", description = """
          En av følgende:
          - prefiks på journalpostId er ugyldig
          - avvikstypen mangler i avvikshendelsen
          - enhetsnummer i header (X_ENHET) mangler
          - ugyldig behandling av avvikshendelse (som bla. inkluderer):
            - oppretting av oppgave feiler
            - BESTILL_SPLITTING: beskrivelse må være i avvikshendelsen
            - OVERFOR_TIL_ANNEN_ENHET: nyttEnhetsnummer og gammeltEnhetsnummer må være i detaljer map
          """),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost som det skal lages avvik på eller feil prefix/id på journalposten"),
      @ApiResponse(responseCode = "503", description = "Oppretting av oppgave for avviket feilet")
  })
  public ResponseEntity<BehandleAvvikshendelseResponse> behandleAvvik(
      @PathVariable String journalpostId,
      @RequestBody Avvikshendelse avvikshendelse,
      @RequestHeader(EnhetFilter.X_ENHET_HEADER) String enhet
  ) {
    LOGGER.info("Behandle avvik {} for journalpost {}", avvikshendelse.getAvvikType(), journalpostId);
    SECURE_LOGGER.info("Behandle avvik {} for journalpost {}: {}", avvikshendelse.getAvvikType(), journalpostId, avvikshendelse);

    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(journalpostId);
    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      return new ResponseEntity<>(initHttpHeadersWith(HttpHeaders.WARNING, "Ugyldig prefix på journalpostId"), HttpStatus.BAD_REQUEST);
    }

    var muligAvvikstype = avvikshendelse.hent();

    if (muligAvvikstype == null || enhet == null || enhet.isBlank()) {
      var message = String.format(
          "BAD REQUEST: avvikshendelse: %s, mulig avvik: %s, enhet: %s", avvikshendelse, muligAvvikstype, enhet
      );

      LOGGER.warn(message);

      return new ResponseEntity<>(initHttpHeadersWith(HttpHeaders.WARNING, message), HttpStatus.BAD_REQUEST);
    }
    var behandleAvvikResponse = avvikService.behandleAvvik(new AvvikshendelseIntern(avvikshendelse,
        enhet,
        Long.valueOf(kildesystemIdenfikator.hentJournalpostId())));
    return behandleAvvikResponse.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
  }

  private HttpHeaders initHttpHeadersWith(String httpHeader, String message) {
    var headers = new HttpHeaders();
    headers.add(httpHeader, message);
    return headers;

  }
}
