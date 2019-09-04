package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.commons.KildesystemIdenfikator.PREFIX_JOARK_COMPLETE;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import no.nav.bidrag.commons.KildesystemIdenfikator;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.security.oidc.api.ProtectedWithClaims;
import no.nav.security.oidc.api.Unprotected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JournalpostController {

  private final JournalpostService journalpostService;
  private static final String PATH_SAKSJOURNAL = "/sak/{saksnummer}/journal";

  public JournalpostController(JournalpostService journalpostService) {
    this.journalpostService = journalpostService;
  }

  @ProtectedWithClaims(issuer = ISSUER)
  @GetMapping(PATH_SAKSJOURNAL + "/{joarkJournalpostId}")
  @ApiOperation("Hent en journalpost for en id på formatet '" + PREFIX_JOARK_COMPLETE + "<journalpostId>'")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Journalpost er hentet"),
      @ApiResponse(code = 204, message = "Fant ikke journalpost som skal hentes"),
      @ApiResponse(code = 401, message = "Du mangler eller har ugyldig sikkerhetstoken"),
      @ApiResponse(code = 403, message = "Du mangler eller har ugyldig sikkerhetstoken"),
      @ApiResponse(code = 404, message = "Journalpost som skal hentes er ikke koblet mot gitt saksnummer, eller det er feil prefix/id på journalposten")
  })
  public ResponseEntity<JournalpostDto> hentJournalpost(@PathVariable String saksnummer, @PathVariable String joarkJournalpostId) {
    if (erUkjentPrefixEllerHarIkkeTallEtterPrefix(joarkJournalpostId) || erIkkePrefixetMedJoark(joarkJournalpostId)) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    var journalpostDtoHttpStatusResponse = journalpostService.hentJournalpost(saksnummer, KildesystemIdenfikator.hentJournalpostId());

    return new ResponseEntity<>(journalpostDtoHttpStatusResponse.getBody(), journalpostDtoHttpStatusResponse.getHttpStatus());
  }

  private boolean erUkjentPrefixEllerHarIkkeTallEtterPrefix(@PathVariable String joarkJournalpostId) {
    return KildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix(joarkJournalpostId);
  }

  private boolean erIkkePrefixetMedJoark(@PathVariable String joarkJournalpostId) {
    return !joarkJournalpostId.startsWith(PREFIX_JOARK_COMPLETE);
  }

  @ProtectedWithClaims(issuer = ISSUER)
  @GetMapping("/sakjournal/{saksnummer}")
  @ApiOperation("Finn journalposter for et saksnummer og fagområde. Parameter fagomrade=BID er bidragjournal og fagomrade=FAR er farskapsjournal")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Fant journalposter for saksnummer og fagområde"),
      @ApiResponse(code = 204, message = "Ingen journalposter for saksnummer og fagområde"),
      @ApiResponse(code = 401, message = "Du mangler eller har ugyldig sikkerhetstoken"),
      @ApiResponse(code = 403, message = "Du mangler eller har ugyldig sikkerhetstoken")
  })
  public ResponseEntity<List<JournalpostDto>> get(@PathVariable String saksnummer, @RequestParam String fagomrade) {
    var journalposter = journalpostService.finnJournalposter(saksnummer, fagomrade);

    if (journalposter.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    return new ResponseEntity<>(journalposter, HttpStatus.OK);
  }

  @Unprotected
  @GetMapping("/")
  @ApiOperation("Welcome Home Page")
  public String index() {
    return "Welcome to Bidrag Dokument Arkiv: Microservice for integration with JOARK for bidrag-dokument.";
  }
}
