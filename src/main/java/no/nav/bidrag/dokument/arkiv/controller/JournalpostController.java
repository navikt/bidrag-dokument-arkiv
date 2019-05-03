package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER;

import io.swagger.annotations.ApiOperation;
import java.util.List;
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

  public JournalpostController(JournalpostService journalpostService) {
    this.journalpostService = journalpostService;
  }

  @ProtectedWithClaims(issuer = ISSUER)
  @GetMapping("/journalpost/{journalpostId}")
  @ApiOperation("Hent journalpost for en id")
  public ResponseEntity<JournalpostDto> get(@PathVariable Integer journalpostId) {
    return journalpostService.hentJournalpost(journalpostId)
        .map(journalpostDto -> new ResponseEntity<>(journalpostDto, HttpStatus.OK))
        .orElse(new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @ProtectedWithClaims(issuer = ISSUER)
  @GetMapping("/sakjournal/{saksnummer}")
  @ApiOperation("Hent journalposter for en bidragssak")
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
