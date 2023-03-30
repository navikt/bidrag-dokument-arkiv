package no.nav.bidrag.dokument.arkiv.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.nav.bidrag.commons.util.KildesystemIdenfikator
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequestInternal
import no.nav.bidrag.dokument.arkiv.service.DistribuerJournalpostService
import no.nav.bidrag.dokument.dto.DistribuerJournalpostRequest
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse
import no.nav.bidrag.dokument.dto.DistribusjonInfoDto
import no.nav.bidrag.dokument.dto.JournalpostId
import no.nav.security.token.support.core.api.Protected
import org.apache.logging.log4j.util.Strings
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class DistribuerController(private val distribuerJournalpostService: DistribuerJournalpostService) : BaseController() {
    @PostMapping("$ROOT_JOURNAL/distribuer/{joarkJournalpostId}")
    @Operation(description = "Bestill distribusjon av journalpost")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Distribusjon av journalpost er bestilt"
            ), ApiResponse(responseCode = "400", description = "Journalpost mangler mottakerid eller adresse er ikke oppgitt i kallet")
        ]
    )
    @ResponseBody
    fun distribuerJournalpost(
        @RequestBody(required = false) distribuerJournalpostRequest: DistribuerJournalpostRequest?,
        @PathVariable joarkJournalpostId: String,
        @RequestParam(required = false, name = "batchId") batchIdHeader: String?
    ): ResponseEntity<DistribuerJournalpostResponse> {
        val batchId = if (Strings.isEmpty(batchIdHeader)) null else batchIdHeader
        LOGGER.info(
            "Distribuerer journalpost {}{}",
            joarkJournalpostId,
            if (Strings.isNotEmpty(batchId)) String.format(" og batchId %s", batchId) else ""
        )
        val kildesystemIdenfikator = KildesystemIdenfikator(joarkJournalpostId!!)
        if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
            val msgBadRequest = String.format("Id har ikke riktig prefix: %s", joarkJournalpostId)
            LOGGER.warn(msgBadRequest)
            return ResponseEntity
                .badRequest()
                .header(HttpHeaders.WARNING, msgBadRequest)
                .build()
        }
        val journalpostId = kildesystemIdenfikator.hentJournalpostIdLong()
        return ResponseEntity.ok(
            distribuerJournalpostService.distribuerJournalpost(
                journalpostId!!,
                batchId,
                DistribuerJournalpostRequestInternal(distribuerJournalpostRequest)
            )
        )
    }

    @GetMapping("$ROOT_JOURNAL/distribuer/{journalpostId}/enabled")
    @Operation(description = "Sjekk om distribusjon av journalpost kan bestilles")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Distribusjon av journalpost kan bestilles"
            ), ApiResponse(responseCode = "406", description = "Distribusjon av journalpost kan ikke bestilles")
        ]
    )
    @ResponseBody
    fun kanDistribuerJournalpost(@PathVariable journalpostId: String): ResponseEntity<Void> {
        LOGGER.info("Sjekker om journalpost {} kan distribueres", journalpostId)
        val kildesystemIdenfikator = KildesystemIdenfikator(journalpostId)
        if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
            val msgBadRequest = String.format("Id har ikke riktig prefix: %s", journalpostId)
            LOGGER.warn(msgBadRequest)
            return ResponseEntity
                .badRequest()
                .header(HttpHeaders.WARNING, msgBadRequest)
                .build()
        }
        return try {
            distribuerJournalpostService.kanDistribuereJournalpost(kildesystemIdenfikator.hentJournalpostIdLong()!!)
            ResponseEntity.ok().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .header(HttpHeaders.WARNING, e.message)
                .build()
        }
    }

    @GetMapping("$ROOT_JOURNAL/distribuer/info/{journalpostId}")
    @Operation(description = "Hent informasjon om distribusjon av journalpost")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Hentet informasjon om distribusjon av journalpost"
            ),
            ApiResponse(responseCode = "202", description = "Journalpost er ikke distribuert eller er av type NOTAT eller INNGÅENDE"),
            ApiResponse(responseCode = "404", description = "Fant ikke journalpost")
        ]
    )
    @ResponseBody
    fun hentDistribusjonsInfo(@PathVariable journalpostId: String): ResponseEntity<DistribusjonInfoDto> {
        LOGGER.info("Henter distribusjonsinfo for journalpost {}", journalpostId)
        val kildesystemIdenfikator = JournalpostId(journalpostId)
        if (!kildesystemIdenfikator.erSystemJoark) {
            val msgBadRequest = String.format("Id har ikke riktig prefix: %s", journalpostId)
            LOGGER.warn(msgBadRequest)
            return ResponseEntity
                .badRequest()
                .header(HttpHeaders.WARNING, msgBadRequest)
                .build()
        }

        return distribuerJournalpostService.hentDistribusjonsInfo(kildesystemIdenfikator.idNumerisk!!)?.let {
            SECURE_LOGGER.info("Hentet distribusjonsinfo $it for journalpost $journalpostId")
            ResponseEntity.ok(it)
        } ?: ResponseEntity.noContent().build()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DistribuerController::class.java)
    }
}
