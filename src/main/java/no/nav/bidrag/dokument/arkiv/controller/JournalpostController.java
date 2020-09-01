package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.commons.KildesystemIdenfikator.PREFIX_JOARK_COMPLETE;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
  @ApiOperation("Hent en journalpost for en id på formatet '" + PREFIX_JOARK_COMPLETE + "<journalpostId>'")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Journalpost er hentet"),
      @ApiResponse(code = 204, message = "Fant ikke journalpost som skal hentes"),
      @ApiResponse(code = 401, message = "Du mangler eller har ugyldig sikkerhetstoken"),
      @ApiResponse(code = 403, message = "Du mangler eller har ugyldig sikkerhetstoken"),
      @ApiResponse(code = 404, message = "Journalpost som skal hentes er ikke koblet mot gitt saksnummer, eller det er feil prefix/id på journalposten")
  })
  public ResponseEntity<JournalpostResponse> hentJournalpost(
      @PathVariable String joarkJournalpostId,
      @RequestParam(required = false) String saksnummer
  ) {
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(joarkJournalpostId);

    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix() || erIkkePrefixetMedJoark(joarkJournalpostId)) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    var httpResponse = journalpostService.hentJournalpost(kildesystemIdenfikator.hentJournalpostId());
    var muligJournalpost = httpResponse.fetchBody();

    if (muligJournalpost.isEmpty()) {
      return new ResponseEntity<>(httpResponse.fetchHeaders(), HttpStatus.NOT_FOUND);
    } else if (muligJournalpost.filter(journalpost -> journalpost.erTilknyttetSak(saksnummer)).isEmpty()) {
      return new ResponseEntity<>(httpResponse.fetchHeaders(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(
        muligJournalpost.get().tilJournalpostResponse(),
        httpResponse.fetchHeaders(),
        httpResponse.getResponseEntity().getStatusCode()
    );
  }

  private boolean erIkkePrefixetMedJoark(String joarkJournalpostId) {
    return !joarkJournalpostId.startsWith(PREFIX_JOARK_COMPLETE);
  }

  @GetMapping("/sak/{saksnummer}/journal")
  @ApiOperation("Finn journalposter for et saksnummer og fagområde. Parameter fagomrade=BID er bidragjournal og fagomrade=FAR er farskapsjournal")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Fant journalposter for saksnummer og fagområde"),
      @ApiResponse(code = 204, message = "Ingen journalposter for saksnummer og fagområde"),
      @ApiResponse(code = 401, message = "Du mangler eller har ugyldig sikkerhetstoken"),
      @ApiResponse(code = 403, message = "Du mangler eller har ugyldig sikkerhetstoken")
  })
  public ResponseEntity<List<JournalpostDto>> hentJournal(@PathVariable String saksnummer, @RequestParam String fagomrade) {
    var journalposterResponse = journalpostService.finnJournalposter(saksnummer, fagomrade);

    if (journalposterResponse.fetchBody().isEmpty() || journalposterResponse.fetchBody().get().isEmpty()) {
      return new ResponseEntity<>(journalposterResponse.fetchHeaders(), HttpStatus.NO_CONTENT);
    }

    return new ResponseEntity<>(journalposterResponse.fetchBody().get(), journalposterResponse.fetchHeaders(), HttpStatus.OK);
  }

  @PutMapping("/journal/{joarkJournalpostId}")
  @ApiOperation("endre eksisterende journalpost med journalpostId på formatet '" + PREFIX_JOARK_COMPLETE + "<journalpostId>'")
  @ApiResponses(value = {
      @ApiResponse(code = 203, message = "Journalpost er endret"),
      @ApiResponse(code = 400, message = "Prefiks på journalpostId er ugyldig, JournalpostEndreJournalpostCommandDto.gjelder er ikke satt eller det ikke finnes en journalpost på gitt id"),
      @ApiResponse(code = 401, message = "Sikkerhetstoken er ikke gyldig"),
      @ApiResponse(code = 403, message = "Sikkerhetstoke1n er ikke gyldig, eller det er ikke gitt adgang til kode 6 og 7 (nav-ansatt)"),
      @ApiResponse(code = 404, message = "Fant ikke journalpost som skal endres, ingen 'payload' eller feil prefix/id på journalposten")
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
  @ApiOperation("Welcome Home Page")
  public String index() {
    return "Welcome to Bidrag Dokument Arkiv: Microservice for integration with JOARK for bidrag-dokument.";
  }
}
