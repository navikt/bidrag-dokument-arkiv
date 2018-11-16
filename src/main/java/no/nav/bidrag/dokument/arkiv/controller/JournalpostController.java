package no.nav.bidrag.dokument.arkiv.controller;

import io.swagger.annotations.ApiOperation;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JournalpostController {

    private final JournalpostService journalpostService;

    public JournalpostController(JournalpostService journalpostService) {
        this.journalpostService = journalpostService;
    }

    @GetMapping("/journalpost/{journalpostId}")
    @ApiOperation("Finn journalpost for en id")
    public ResponseEntity<JournalpostDto> get(@PathVariable Integer journalpostId) {
        return journalpostService.hentJournalpost(journalpostId)
                .map(journalpostDto -> new ResponseEntity<>(journalpostDto, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    @GetMapping("/")
    @ApiOperation("Welcome Home Page")
    public String index(){
        return "Welcome to Bidrag Dokument Arkiv: Microservice for integration with JOARK for bidrag-dokument.";
    }

}
