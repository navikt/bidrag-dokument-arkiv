package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.security.oidc.api.ProtectedWithClaims;
import no.nav.security.oidc.api.Unprotected;

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

    @Unprotected
    @GetMapping("/")
    @ApiOperation("Welcome Home Page")
    public String index() {
        return "Welcome to Bidrag Dokument Arkiv: Microservice for integration with JOARK for bidrag-dokument.";
    }

}
