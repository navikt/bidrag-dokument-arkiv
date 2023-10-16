package no.nav.bidrag.dokument.arkiv.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.commons.util.KildesystemIdenfikator
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.arkiv.service.EndreJournalpostService
import no.nav.bidrag.dokument.arkiv.service.JournalpostService
import no.nav.bidrag.transport.dokument.EndreJournalpostCommand
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class JournalpostController(
    journalpostService: ResourceByDiscriminator<JournalpostService?>,
    private val endreJournalpostService: EndreJournalpostService
) : BaseController() {
    private val journalpostService: JournalpostService

    init {
        this.journalpostService = journalpostService.get(Discriminator.REGULAR_USER)
    }

    @GetMapping("$ROOT_JOURNAL/{joarkJournalpostId}")
    @Operation(
        description = "Hent en journalpost for en id på formatet '" + KildesystemIdenfikator.PREFIX_JOARK_COMPLETE + "<journalpostId>'",
        security = [SecurityRequirement(name = "bearer-key")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Journalpost er hentet"), ApiResponse(
                responseCode = "400",
                description = "Journalpost som skal hentes er ikke koblet mot gitt saksnummer, eller det er feil prefix/id på journalposten"
            ), ApiResponse(
                responseCode = "404",
                description = "Fant ikke journalpost som skal hentes"
            )
        ]
    )
    fun hentJournalpost(
        @PathVariable joarkJournalpostId: String,
        @RequestParam(required = false) saksnummer: String?
    ): ResponseEntity<JournalpostResponse> {
        LOGGER.info("Henter journalpost {} med saksnummer {}", joarkJournalpostId, saksnummer)
        val kildesystemIdenfikator = KildesystemIdenfikator(joarkJournalpostId)
        if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix() || erIkkePrefixetMedJoark(
                joarkJournalpostId
            )
        ) {
            return ResponseEntity
                .badRequest()
                .header(HttpHeaders.WARNING, "Ukjent prefix på journalpostId: $joarkJournalpostId")
                .build()
        }
        val journalpostId = kildesystemIdenfikator.hentJournalpostId()
            ?: return ResponseEntity
                .badRequest()
                .header(
                    HttpHeaders.WARNING,
                    "Kunne ikke hente id fra prefikset journalpostId: $joarkJournalpostId"
                )
                .build()
        return journalpostService.hentJournalpostMedFnrOgTilknyttedeSaker(
            java.lang.Long.valueOf(
                journalpostId.toLong()
            ),
            saksnummer
        )
            .map { journalpost: Journalpost -> ResponseEntity.ok(journalpost.tilJournalpostResponse()) }
            .orElse(
                ResponseEntity.notFound()
                    .header(
                        HttpHeaders.WARNING,
                        String.format(
                            "Fant ingen journalpost med id %s og saksnummer %s",
                            journalpostId,
                            saksnummer
                        )
                    )
                    .build()
            )
    }

    private fun erIkkePrefixetMedJoark(joarkJournalpostId: String): Boolean {
        return !joarkJournalpostId.startsWith(KildesystemIdenfikator.PREFIX_JOARK_COMPLETE)
    }

    @GetMapping("/sak/{saksnummer}/journal")
    @Operation(description = "Finn journalposter for et saksnummer og fagområde. Parameter fagomrade=BID er bidragjournal og fagomrade=FAR er farskapsjournal")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste over journalposter for saksnummer og fagområde"
            ), ApiResponse(
                responseCode = "404",
                description = "Fant ikke journalposter for oppgitt sak og fagområde"
            )
        ]
    )
    fun hentJournal(
        @PathVariable saksnummer: String,
        @RequestParam fagomrade: List<String> = emptyList()
    ): ResponseEntity<List<JournalpostDto>> {
        LOGGER.info("Henter journal for saksnummer {} og tema {}", saksnummer, fagomrade)
        return ResponseEntity.ok(journalpostService.finnJournalposter(saksnummer, fagomrade))
    }

    @PatchMapping("$ROOT_JOURNAL/{joarkJournalpostId}")
    @Operation(description = "endre eksisterende journalpost med journalpostId på formatet '" + KildesystemIdenfikator.PREFIX_JOARK_COMPLETE + "<journalpostId>'")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "203", description = "Journalpost er endret"), ApiResponse(
                responseCode = "400",
                description = "Prefiks på journalpostId er ugyldig, JournalpostEndreJournalpostCommandDto.gjelder er ikke satt eller det ikke finnes en journalpost på gitt id"
            ), ApiResponse(
                responseCode = "404",
                description = "Fant ikke journalpost som skal endres, ingen 'payload' eller feil prefix/id på journalposten"
            )
        ]
    )
    fun patch(
        @RequestBody endreJournalpostCommand: EndreJournalpostCommand,
        @PathVariable joarkJournalpostId: String,
        @RequestHeader(EnhetFilter.X_ENHET_HEADER) enhet: String?
    ): ResponseEntity<Void> {
        LOGGER.info("Mottatt oppdater journalpost {} kall", joarkJournalpostId)
        SECURE_LOGGER.info(
            "Oppdater journalpost {} med body: {}",
            joarkJournalpostId,
            endreJournalpostCommand
        )
        val kildesystemIdenfikator = KildesystemIdenfikator(joarkJournalpostId)
        if (kildesystemIdenfikator.erUkjentPrefixEllerHarIkkeTallEtterPrefix()) {
            val msgBadRequest = String.format(
                "Id har ikke riktig prefix: %s eller det mangler gjelder person %s",
                joarkJournalpostId,
                endreJournalpostCommand
            )
            LOGGER.warn(msgBadRequest)
            return ResponseEntity
                .badRequest()
                .header(HttpHeaders.WARNING, msgBadRequest)
                .build()
        }
        endreJournalpostService.endre(
            java.lang.Long.valueOf(
                kildesystemIdenfikator.hentJournalpostId()!!.toLong()
            ),
            EndreJournalpostCommandIntern(endreJournalpostCommand, enhet!!)
        )
        return ResponseEntity(HttpStatus.OK)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(JournalpostController::class.java)
    }
}
