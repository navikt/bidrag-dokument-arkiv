package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.commons.KildesystemIdenfikator.PREFIX_BIDRAG_COMPLETE;
import static no.nav.bidrag.commons.KildesystemIdenfikator.PREFIX_JOARK;
import static no.nav.bidrag.commons.KildesystemIdenfikator.PREFIX_JOARK_COMPLETE;
import static no.nav.bidrag.commons.web.WebUtil.initHttpHeadersWith;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Optional;
import no.nav.bidrag.commons.KildesystemIdenfikator;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.dokument.arkiv.dto.AvvikshendelseIntern;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequestInternal;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.service.AvvikService;
import no.nav.bidrag.dokument.arkiv.service.DistribuerJournalpostService;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.bidrag.dokument.dto.AvvikType;
import no.nav.bidrag.dokument.dto.Avvikshendelse;
import no.nav.bidrag.dokument.dto.BehandleAvvikshendelseResponse;
import no.nav.bidrag.dokument.dto.DistribuerJournalpostRequest;
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse;
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.bidrag.dokument.dto.JournalpostResponse;
import no.nav.security.token.support.core.api.Protected;
import no.nav.security.token.support.core.api.Unprotected;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Protected
public class JournalpostController {

  private static final Logger LOGGER = LoggerFactory.getLogger(JournalpostController.class);
  public static final String ROOT_JOURNAL = "/journal";

  private final JournalpostService journalpostService;
  private final AvvikService avvikService;
  private final DistribuerJournalpostService distribuerJournalpostService;

  public JournalpostController(ResourceByDiscriminator<JournalpostService> journalpostService, AvvikService avvikService, DistribuerJournalpostService distribuerJournalpostService) {
    this.journalpostService = journalpostService.get(Discriminator.REGULAR_USER);
    this.avvikService = avvikService;
    this.distribuerJournalpostService = distribuerJournalpostService;
  }

  @GetMapping(ROOT_JOURNAL+"/{journalpostId}/avvik")
  @Operation(
      security = {@SecurityRequirement(name = "bearer-key")},
      summary = "Henter mulige avvik for en journalpost, id på formatet '" + PREFIX_JOARK + "<journalpostId>'"
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
  ){
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
      summary = "Behandler et avvik for en journalpost, id på formatet '" + PREFIX_BIDRAG_COMPLETE + "<journalpostId>'"
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
    LOGGER.info("opprett /journal/{}/avvik: {}", journalpostId, avvikshendelse);

    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(journalpostId);
    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      return new ResponseEntity<>(initHttpHeadersWith(HttpHeaders.WARNING, "Ugyldig prefix på journalpostId"), HttpStatus.BAD_REQUEST);
    }

    var muligAvvikstype = avvikshendelse.hent()
        .map(type -> AvvikType.valueOf(type.name()));

    if (muligAvvikstype.isEmpty() || enhet == null || enhet.isBlank()) {
      var message = String.format(
          "BAD REQUEST: avvikshendelse: %s, mulig avvik: %s, enhet: %s", avvikshendelse, muligAvvikstype, enhet
      );

      LOGGER.warn(message);

      return new ResponseEntity<>(initHttpHeadersWith(HttpHeaders.WARNING, message), HttpStatus.BAD_REQUEST);
    }
    var behandleAvvikResponse = avvikService.behandleAvvik(new AvvikshendelseIntern(avvikshendelse, enhet, Long.valueOf(kildesystemIdenfikator.hentJournalpostId())));
    return behandleAvvikResponse.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
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
    LOGGER.info("api: put /journal/{}, body: {}", joarkJournalpostId, endreJournalpostCommand);
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

    var endreJournalpostHttpResponse = journalpostService.endre(
        Long.valueOf(kildesystemIdenfikator.hentJournalpostId()), new EndreJournalpostCommandIntern(endreJournalpostCommand, enhet)
    );

    return new ResponseEntity<>(endreJournalpostHttpResponse.getResponseEntity().getStatusCode());
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
      @RequestParam(required = false) String batchIdHeader
  ) {
    var batchId = Strings.isEmpty(batchIdHeader) ? null : batchIdHeader;
    LOGGER.info("Distribuerer journalpost {}{}", joarkJournalpostId, Strings.isEmpty(batchId) ? String.format(" og batchId %s", batchId) : "");
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

  @GetMapping("/journal/distribuer/{journalpostId}/enabled")
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

  @Unprotected
  @GetMapping("/")
  @Operation(description = "Welcome Home Page")
  public String index() {
    return "Welcome to Bidrag Dokument Arkiv: Microservice for integration with JOARK for bidrag-dokument.";
  }
}
