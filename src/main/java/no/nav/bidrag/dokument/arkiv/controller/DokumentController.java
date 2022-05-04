package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.commons.KildesystemIdenfikator.PREFIX_JOARK;
import static no.nav.bidrag.commons.web.WebUtil.initHttpHeadersWith;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import no.nav.bidrag.commons.KildesystemIdenfikator;
import no.nav.bidrag.dokument.arkiv.service.DokumentService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Protected
public class DokumentController {

  private final DokumentService dokumentService;

  public DokumentController(DokumentService dokumentService) {
    this.dokumentService = dokumentService;
  }

  @GetMapping(value = {"/dokument/{journalpostId}/{dokumentreferanse}", "/dokument/{journalpostId}"})
  @Operation(
      security = {@SecurityRequirement(name = "bearer-key")},
      summary = "Henter dokument fra Joark for journalpostid og dokumentreferanse. Hvis bare journalpostId er oppgitt så returneres alle dokumentene (hoveddokument og vedlegg) som ett dokument. "
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK - dokument returneres i form av base64 encoded string."),
      @ApiResponse(responseCode = "401", description = "Du mangler sikkerhetstoken", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "Sikkerhetstoken er ikke gyldig", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "404", description = "Fant ikke journalpost emed oppgitt dokumentreferanse", content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<byte[]> hentDokument(
      @PathVariable String journalpostId,
      @PathVariable(required = false) String dokumentreferanse
  ){
    KildesystemIdenfikator kildesystemIdenfikator = new KildesystemIdenfikator(journalpostId);
    if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
      return new ResponseEntity<>(initHttpHeadersWith(HttpHeaders.WARNING, "Ugyldig prefix på journalpostId"), HttpStatus.BAD_REQUEST);
    }

    return dokumentService.hentDokument(kildesystemIdenfikator.hentJournalpostIdLong(), dokumentreferanse);
  }
}
