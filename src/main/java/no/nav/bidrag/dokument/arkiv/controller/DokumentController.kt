package no.nav.bidrag.dokument.arkiv.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import mu.KotlinLogging
import no.nav.bidrag.commons.util.KildesystemIdenfikator
import no.nav.bidrag.commons.web.WebUtil
import no.nav.bidrag.dokument.arkiv.service.DokumentService
import no.nav.bidrag.transport.dokument.DokumentMetadata
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

private val LOGGER = KotlinLogging.logger {}

@RestController
@Protected
class DokumentController(private val dokumentService: DokumentService) {
    @GetMapping(value = ["/dokument/{journalpostId}/{dokumentreferanse}"])
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Henter dokument fra Joark for journalpostid og dokumentreferanse. ",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK - dokument returneres i form av base64 encoded string.",
            ),
            ApiResponse(
                responseCode = "404",
                description = "Fant ikke journalpost med oppgitt dokumentreferanse",
            ),
        ],
    )
    fun hentDokument(@PathVariable journalpostId: String, @PathVariable dokumentreferanse: String): ResponseEntity<ByteArray> {
        val kildesystemIdenfikator = KildesystemIdenfikator(journalpostId)
        return if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
            ResponseEntity(
                WebUtil.initHttpHeadersWith(
                    HttpHeaders.WARNING,
                    "Ugyldig prefix på journalpostId",
                ),
                HttpStatus.BAD_REQUEST,
            )
        } else {
            dokumentService.hentDokument(
                kildesystemIdenfikator.hentJournalpostIdLong()!!,
                dokumentreferanse,
            )
        }
    }

    @RequestMapping(
        value = ["/dokument/{journalpostId}/{dokumentreferanse}", "/dokument/{journalpostId}", "/dokumentreferanse/{dokumentreferanse}"],
        method = [RequestMethod.OPTIONS],
    )
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Henter dokument for journalpostid og dokumentreferanse. ",
    )
    fun hentDokumentMetadata(
        @PathVariable(required = false) journalpostId: String?,
        @PathVariable(required = false) dokumentreferanse: String?,
    ): ResponseEntity<List<DokumentMetadata>> {
        LOGGER.info("Henter dokument for journalpost $journalpostId og dokumentId $dokumentreferanse")
        if (journalpostId.isNullOrEmpty() && dokumentreferanse.isNullOrEmpty()) {
            return ResponseEntity
                .badRequest()
                .header(
                    HttpHeaders.WARNING,
                    "Kan ikke hente dokument uten journalpostId eller dokumentereferanse",
                )
                .build()
        }
        if (journalpostId.isNullOrEmpty()) {
            return ResponseEntity.ok(dokumentService.hentDokumentMetadata(dokumentReferanse = dokumentreferanse))
        }
        val kildesystemIdenfikator = KildesystemIdenfikator(journalpostId)
        return if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
            ResponseEntity
                .badRequest()
                .header(HttpHeaders.WARNING, "Ugyldig prefix på journalpostId $journalpostId")
                .build()
        } else {
            ResponseEntity.ok(
                dokumentService.hentDokumentMetadata(
                    kildesystemIdenfikator.hentJournalpostIdLong(),
                    dokumentreferanse,
                ),
            )
        }
    }
}
