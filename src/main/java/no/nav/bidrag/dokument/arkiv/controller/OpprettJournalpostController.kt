package no.nav.bidrag.dokument.arkiv.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.arkiv.service.EndreJournalpostService
import no.nav.bidrag.dokument.arkiv.service.JournalpostService
import no.nav.bidrag.dokument.arkiv.service.OpprettJournalpostService
import no.nav.bidrag.dokument.dto.OpprettJournalpostRequest
import no.nav.bidrag.dokument.dto.OpprettJournalpostResponse
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class OpprettJournalpostController(private val opprettJournalpostService: OpprettJournalpostService): BaseController() {

    @PostMapping("/journalpost")
    @Operation(security = [SecurityRequirement(name = "bearer-key")], description = "Opprett journalpost i Joark")
    @ApiResponses(value = [ApiResponse(responseCode = "400", description = "Opprett journalpost kalt med ugyldig data")])
    fun opprettJournalpost(@RequestBody opprettJournalpostRequest: OpprettJournalpostRequest?): ResponseEntity<OpprettJournalpostResponse> {
        SECURE_LOGGER.info("Oppretter journalpost {}", opprettJournalpostRequest)
        return ResponseEntity.ok(opprettJournalpostService.opprettJournalpost(opprettJournalpostRequest!!))
    }

}