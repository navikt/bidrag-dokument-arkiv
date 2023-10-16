package no.nav.bidrag.dokument.arkiv.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import mu.KotlinLogging
import no.nav.bidrag.commons.util.KildesystemIdenfikator
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv
import no.nav.bidrag.dokument.arkiv.dto.AvvikshendelseIntern
import no.nav.bidrag.dokument.arkiv.service.AvvikService
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.BehandleAvvikshendelseResponse
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

private val LOGGER = KotlinLogging.logger {}

@RestController
@Protected
class AvvikController(private val avvikService: AvvikService) : BaseController() {
    @GetMapping(ROOT_JOURNAL + "/{journalpostId}/avvik")
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Henter mulige avvik for en journalpost, id på formatet '" +
            KildesystemIdenfikator.PREFIX_JOARK +
            "<journalpostId>'"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Tilgjengelig avvik for journalpost er hentet"
            ), ApiResponse(
                responseCode = "404",
                description = "Fant ikke journalpost som det skal hentes avvik på"
            )
        ]
    )
    fun hentAvvik(
        @PathVariable journalpostId: String?,
        @Parameter(name = "saksnummer", description = "journalposten tilhører sak")
        @RequestParam(
            required = false
        )
        saksnummer: String?
    ): ResponseEntity<List<AvvikType>> {
        val muligSak = Optional.ofNullable(saksnummer)
        if (muligSak.isPresent) {
            LOGGER.info("GET: journal/{}/avvik?saksnummer={}", journalpostId, saksnummer)
        } else {
            LOGGER.info("GET: /journal/{}/avvik", journalpostId)
        }
        val kildesystemIdenfikator = KildesystemIdenfikator(journalpostId!!)
        return if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
            ResponseEntity(
                initHttpHeadersWith(
                    HttpHeaders.WARNING,
                    "Ugyldig prefix på journalpostId"
                ),
                HttpStatus.BAD_REQUEST
            )
        } else {
            ResponseEntity.ok(
                avvikService.hentAvvik(
                    java.lang.Long.valueOf(
                        kildesystemIdenfikator.hentJournalpostId()!!.toLong()
                    )
                )
            )
        }
    }

    @PostMapping(
        value = [ROOT_JOURNAL + "/{journalpostId}/avvik"],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        security = [SecurityRequirement(name = "bearer-key")],
        summary = "Behandler et avvik for en journalpost, id på formatet '" +
            KildesystemIdenfikator.PREFIX_JOARK_COMPLETE +
            "<journalpostId>'"
    )
    @Transactional
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Avvik på journalpost er behandlet"
            ), ApiResponse(
                responseCode = "400",
                description = """En av følgende:
          - prefiks på journalpostId er ugyldig
          - avvikstypen mangler i avvikshendelsen
          - enhetsnummer i header (X_ENHET) mangler
          - ugyldig behandling av avvikshendelse (som bla. inkluderer):
            - oppretting av oppgave feiler
            - BESTILL_SPLITTING: beskrivelse må være i avvikshendelsen
            - OVERFOR_TIL_ANNEN_ENHET: nyttEnhetsnummer og gammeltEnhetsnummer må være i detaljer map
          """
            ), ApiResponse(
                responseCode = "503",
                description = "Oppretting av oppgave for avviket feilet"
            )
        ]
    )
    fun behandleAvvik(
        @PathVariable journalpostId: String?,
        @RequestBody avvikshendelse: Avvikshendelse,
        @RequestHeader(EnhetFilter.X_ENHET_HEADER) enhet: String?
    ): ResponseEntity<BehandleAvvikshendelseResponse> {
        LOGGER.info(
            "Behandle avvik {} for journalpost {}",
            avvikshendelse.avvikType,
            journalpostId
        )
        BidragDokumentArkiv.SECURE_LOGGER.info(
            "Behandle avvik {} for journalpost {}: {}",
            avvikshendelse.avvikType,
            journalpostId,
            avvikshendelse
        )
        val kildesystemIdenfikator = KildesystemIdenfikator(journalpostId!!)
        if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
            return ResponseEntity(
                initHttpHeadersWith(HttpHeaders.WARNING, "Ugyldig prefix på journalpostId"),
                HttpStatus.BAD_REQUEST
            )
        }
        val muligAvvikstype = avvikshendelse.hent()
        if (muligAvvikstype == null || enhet.isNullOrBlank()) {
            val message = String.format(
                "BAD REQUEST: avvikshendelse: %s, mulig avvik: %s, enhet: %s",
                avvikshendelse,
                muligAvvikstype,
                enhet
            )
            LOGGER.warn(message)
            return ResponseEntity(
                initHttpHeadersWith(HttpHeaders.WARNING, message),
                HttpStatus.BAD_REQUEST
            )
        }
        val behandleAvvikResponse = avvikService.behandleAvvik(
            AvvikshendelseIntern(
                avvikshendelse,
                enhet,
                kildesystemIdenfikator.hentJournalpostId()!!.toLong()
            )
        )
        return ResponseEntity.ok(behandleAvvikResponse)
    }

    private fun initHttpHeadersWith(httpHeader: String, message: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.add(httpHeader, message)
        return headers
    }
}
