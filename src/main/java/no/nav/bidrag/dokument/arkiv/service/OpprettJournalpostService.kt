package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.SECURE_LOGGER
import no.nav.bidrag.dokument.arkiv.consumer.BidragDokumentConsumer
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottakerIdType
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.JoarkJournalpostType
import no.nav.bidrag.dokument.arkiv.dto.JoarkMottakUtsendingKanal
import no.nav.bidrag.dokument.arkiv.dto.JoarkOpprettJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.JoarkOpprettJournalpostResponse
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.dto.erSamhandler
import no.nav.bidrag.dokument.arkiv.dto.hentGjelderIdent
import no.nav.bidrag.dokument.arkiv.dto.hentGjelderType
import no.nav.bidrag.dokument.arkiv.dto.hentJournalførendeEnhet
import no.nav.bidrag.dokument.arkiv.dto.opprettDokumentVariant
import no.nav.bidrag.dokument.arkiv.dto.validerKanOppretteJournalpost
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.HentJournalpostFeilet
import no.nav.bidrag.dokument.arkiv.model.KunneIkkeJournalforeOpprettetJournalpost
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager
import no.nav.bidrag.dokument.arkiv.service.utvidelser.erNotat
import no.nav.bidrag.transport.dokument.AvsenderMottakerDtoIdType
import no.nav.bidrag.transport.dokument.JournalpostType
import no.nav.bidrag.transport.dokument.MottakUtsendingKanal
import no.nav.bidrag.transport.dokument.OpprettDokumentDto
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest
import no.nav.bidrag.transport.dokument.OpprettJournalpostResponse
import org.apache.logging.log4j.util.Strings
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDateTime
import java.util.Base64

@Service
class OpprettJournalpostService(
    dokarkivConsumers: ResourceByDiscriminator<DokarkivConsumer?>,
    safConsumers: ResourceByDiscriminator<SafConsumer?>,
    private val saksbehandlerInfoManager: SaksbehandlerInfoManager,
    private val dokumentService: DokumentService,
    private val endreJournalpostService: EndreJournalpostService,
    private val bidragDokumentConsumer: BidragDokumentConsumer,
) {
    private val dokarkivConsumer: DokarkivConsumer
    private val safConsumer: SafConsumer

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OpprettJournalpostService::class.java)
    }

    init {
        dokarkivConsumer = dokarkivConsumers.get(Discriminator.REGULAR_USER)
        safConsumer = safConsumers.get(Discriminator.REGULAR_USER)
    }

    fun opprettJournalpost(request: OpprettJournalpostRequest): OpprettJournalpostResponse {
        val opprettJournalpostRequest = mapTilJoarkOpprettJournalpostRequest(request)
        if (request.skalFerdigstilles && request.erNotat && !request.saksbehandlerIdent.isNullOrEmpty()) {
            val respons = opprettJournalpost(
                opprettJournalpostRequest,
                request.tilknyttSaker,
                skalFerdigstilles = false,
            )
            val journalpostId = respons.journalpostId!!.toLong()
            ferdigstillJournalpost(
                journalpostId,
                request.hentJournalførendeEnhet()!!,
                request.saksbehandlerIdent,
            )
            lagreSaksbehandlerIdentOgKnyttFerdigstiltJournalpostTilSaker(
                journalpostId,
                request.tilknyttSaker,
                request.saksbehandlerIdent,
            )
            return respons
        }
        return opprettJournalpost(
            opprettJournalpostRequest,
            request.tilknyttSaker,
            skalFerdigstilles = request.skalFerdigstilles,
        )
    }

    private fun ferdigstillJournalpost(journalpostId: Long, journalfoerendeEnhet: String, saksbehandlerIdent: String?) {
        val saksbehandlerNavn =
            saksbehandlerIdent?.let { saksbehandlerInfoManager.hentSaksbehandler(it) }
                ?.orElse(null)?.navn
        dokarkivConsumer.ferdigstill(
            FerdigstillJournalpostRequest(
                journalpostId = journalpostId,
                journalfoerendeEnhet = journalfoerendeEnhet,
                opprettetAvNavn = saksbehandlerNavn,
                journalfortAvNavn = saksbehandlerNavn,
            ),
        )
    }

    fun opprettJournalpost(
        _request: JoarkOpprettJournalpostRequest,
        knyttTilSaker: List<String> = emptyList(),
        originalJournalpostId: Long?,
        skalFerdigstilles: Boolean = false,
    ): OpprettJournalpostResponse {
        val tilknyttetSak =
            if (knyttTilSaker.isNotEmpty()) knyttTilSaker[0] else _request.sak?.fagsakId
        var request =
            _request.copy(
                sak = if (tilknyttetSak.isNullOrEmpty()) {
                    null
                } else {
                    JoarkOpprettJournalpostRequest.OpprettJournalpostSak(
                        tilknyttetSak,
                    )
                },
            )
        request = populerMedDokumenterByteData(request, originalJournalpostId)
        return opprettJournalpost(request, knyttTilSaker, skalFerdigstilles)
    }

    private fun opprettJournalpost(
        request: JoarkOpprettJournalpostRequest,
        knyttTilSaker: List<String> = emptyList(),
        skalFerdigstilles: Boolean = false,
    ): OpprettJournalpostResponse {
        validerKanOppretteJournalpost(request, skalFerdigstilles)

        val response = dokarkivConsumer.opprett(request, skalFerdigstilles)
        LOGGER.info(
            "Opprettet ny journalpost ${response.journalpostId} med type=${request.journalpostType} kanal=${request.kanal}, tema=${request.tema}, referanseId=${request.eksternReferanseId} og enhet=${request.journalfoerendeEnhet}",
        )
        SECURE_LOGGER.info("Opprettet ny journalpost {}", response)

        validerOpprettJournalpostResponse(skalFerdigstilles, response)

        if (response.journalpostferdigstilt) {
            lagreSaksbehandlerIdentOgKnyttFerdigstiltJournalpostTilSaker(
                response.journalpostId!!,
                knyttTilSaker,
            )
        }

        return OpprettJournalpostResponse(
            journalpostId = response.journalpostId.toString(),
            dokumenter = response.dokumenter?.map {
                OpprettDokumentDto(dokumentreferanse = it.dokumentInfoId)
            } ?: emptyList(),
        )
    }

    private fun lagreSaksbehandlerIdentOgKnyttFerdigstiltJournalpostTilSaker(
        journalpostId: Long,
        knyttTilSaker: List<String>,
        saksbehandlerIdent: String? = null,
    ) {
        try {
            val opprettetJournalpost = hentJournalpost(journalpostId)
            endreJournalpostService.lagreSaksbehandlerIdentForJournalfortJournalpost(
                opprettetJournalpost,
                saksbehandlerIdent,
            )
            knyttSakerTilOpprettetJournalpost(opprettetJournalpost, knyttTilSaker)
        } catch (e: Exception) {
            LOGGER.error(
                "Etterbehandling av opprettet journalpost feilet (knytt til flere saker eller lagre saksbehandler ident). Fortsetter behandling da feilen må behandles manuelt.",
                e,
            )
        }
    }

    private fun validerOpprettJournalpostResponse(skalFerdigstilles: Boolean, response: JoarkOpprettJournalpostResponse) {
        if (skalFerdigstilles && !response.journalpostferdigstilt) {
            val message = String.format(
                "Kunne ikke journalføre journalpost %s med feilmelding %s",
                response.journalpostId,
                response.melding,
            )
            LOGGER.error(message)
            throw KunneIkkeJournalforeOpprettetJournalpost(message)
        }
    }

    private fun hentJournalpost(journalpostId: Long): Journalpost {
        return try {
            safConsumer.hentJournalpost(journalpostId)
        } catch (e: Exception) {
            throw HentJournalpostFeilet("Det skjedde en feil ved henting av journalpost", e)
        }
    }

    private fun knyttSakerTilOpprettetJournalpost(opprettetJournalpost: Journalpost, knyttTilSaker: List<String>) {
        knyttTilSaker
            .stream()
            .filter { saksnummer: String -> saksnummer != opprettetJournalpost.sak!!.fagsakId }
            .forEach { saksnummer: String? ->
                endreJournalpostService.tilknyttTilSak(
                    saksnummer,
                    opprettetJournalpost,
                )
            }
    }

    private fun populerMedDokumenterByteData(request: JoarkOpprettJournalpostRequest, originalJournalpostId: Long?): JoarkOpprettJournalpostRequest {
        if (originalJournalpostId != null) {
            return request.copy(
                dokumenter = request.dokumenter.map {
                    if (it.dokumentvarianter.isEmpty() && Strings.isNotEmpty(it.dokumentInfoId)) {
                        val dokument = hentDokument(originalJournalpostId, it.dokumentInfoId!!)
                        it.copy(dokumentvarianter = listOf(opprettDokumentVariant(null, dokument)))
                    } else {
                        it
                    }
                },
            )
        }
        return request
    }

    private fun mapTilJoarkOpprettJournalpostRequest(request: OpprettJournalpostRequest): JoarkOpprettJournalpostRequest {
        val hoveddokument = request.dokumenter[0]
        val tilknyttSaker = request.tilknyttSaker
        val hovedTittel = request.tittel.takeIf { !it.isNullOrEmpty() } ?: hoveddokument.tittel
        val erInngaendeOgSkalIkkeJournalfores =
            request.journalposttype == JournalpostType.INNGÅENDE && !request.skalFerdigstilles
        val erInngaende = request.journalposttype == JournalpostType.INNGÅENDE
        val erNotat = request.journalposttype == JournalpostType.NOTAT
        val samhandlerId =
            request.avsenderMottaker?.takeIf { it.type == AvsenderMottakerDtoIdType.SAMHANDLER }?.ident
        val tilleggsOpplysninger = TilleggsOpplysninger()

        samhandlerId?.let {
            tilleggsOpplysninger.leggTilSamhandlerId(it)
        }

        return JoarkOpprettJournalpostRequest(
            tittel = hovedTittel,
            tema = request.tema ?: "BID",
            journalpostType = when (request.journalposttype) {
                JournalpostType.UTGÅENDE, JournalpostType.UTGAAENDE -> JoarkJournalpostType.UTGAAENDE // DEPRECATED
                JournalpostType.INNGÅENDE -> JoarkJournalpostType.INNGAAENDE
                JournalpostType.NOTAT -> JoarkJournalpostType.NOTAT
                else -> JoarkJournalpostType.UTGAAENDE
            },
            eksternReferanseId = request.referanseId,
            behandlingstema = request.behandlingstema,
            journalfoerendeEnhet = if (erInngaendeOgSkalIkkeJournalfores) null else request.hentJournalførendeEnhet(),
            kanal = when (request.journalposttype) {
                JournalpostType.UTGÅENDE, JournalpostType.UTGAAENDE ->
                    when (request.kanal) {
                        MottakUtsendingKanal.LOKAL_UTSKRIFT -> JoarkMottakUtsendingKanal.L
                        MottakUtsendingKanal.INGEN_DISTRIBUSJON -> JoarkMottakUtsendingKanal.INGEN_DISTRIBUSJON
                        else -> null // Settes av distribusjonsløpet
                    }

                JournalpostType.INNGÅENDE -> // TODO: Joark få en ny mottakskanal for skanning fra Bidrag
                    if (request.kanal == MottakUtsendingKanal.SKANNING_BIDRAG) {
                        JoarkMottakUtsendingKanal.SKAN_BID
                    } else {
                        JoarkMottakUtsendingKanal.NAV_NO
                    }

                else -> null
            }?.name,
            datoDokument = if (erNotat) request.datoDokument?.toString() else null,
            datoMottatt = if (erInngaende) {
                request.datoMottatt?.toString() ?: LocalDateTime.now()
                    .toString()
            } else {
                null
            },
            bruker = JoarkOpprettJournalpostRequest.OpprettJournalpostBruker(
                id = request.hentGjelderIdent(),
                idType = request.hentGjelderType()?.name,
            ),
            avsenderMottaker = if (request.journalposttype != JournalpostType.NOTAT) {
                mapMottaker(
                    request,
                )
            } else {
                null
            },
            sak = if (erInngaendeOgSkalIkkeJournalfores || tilknyttSaker.isEmpty()) {
                null
            } else {
                JoarkOpprettJournalpostRequest.OpprettJournalpostSak(tilknyttSaker[0])
            },
            dokumenter = request.dokumenter.mapIndexed { i, it ->
                JoarkOpprettJournalpostRequest.Dokument(
                    brevkode = it.brevkode,
                    tittel = it.tittel,
                    dokumentvarianter = listOf(
                        opprettDokumentVariant(
                            null,
                            hentDokument(it),
                        ),
                    ),
                )
            },
            tilleggsopplysninger = tilleggsOpplysninger,
        )
    }

    private fun hentDokument(dokumentDto: OpprettDokumentDto): ByteArray {
        return dokumentDto.fysiskDokument ?: dokumentDto.dokument?.let {
            Base64.getDecoder().decode(it)
        } ?: dokumentDto.dokumentreferanse?.let {
            LOGGER.info(
                "Henter dokument bytedata for dokument med tittel ${dokumentDto.tittel} og dokumentreferanse ${dokumentDto.dokumentreferanse}",
            )
            bidragDokumentConsumer.hentDokument(
                it,
            )
        } ?: throw HttpClientErrorException(
            HttpStatus.BAD_REQUEST,
            "Fant ikke referanse eller data for dokument med tittel ${dokumentDto.tittel}",
        )
    }

    private fun mapMottaker(request: OpprettJournalpostRequest): JoarkOpprettJournalpostRequest.OpprettJournalpostAvsenderMottaker =
        if (request.avsenderMottaker?.erSamhandler() == true) {
            JoarkOpprettJournalpostRequest.OpprettJournalpostAvsenderMottaker(
                navn = request.avsenderMottaker?.navn,
            )
        } else {
            JoarkOpprettJournalpostRequest.OpprettJournalpostAvsenderMottaker(
                navn = request.avsenderMottaker?.navn,
                id = request.avsenderMottaker?.ident,
                idType = request.avsenderMottaker?.ident?.let {
                    when (request.avsenderMottaker?.type) {
                        AvsenderMottakerDtoIdType.FNR -> AvsenderMottakerIdType.FNR
                        AvsenderMottakerDtoIdType.ORGNR -> AvsenderMottakerIdType.ORGNR
                        AvsenderMottakerDtoIdType.UTENLANDSK_ORGNR -> AvsenderMottakerIdType.UTL_ORG
                        else -> AvsenderMottakerIdType.FNR
                    }
                },
            )
        }

    private fun hentDokument(journalpostId: Long, dokumentId: String): ByteArray {
        return dokumentService.hentDokument(journalpostId, dokumentId).body!!
    }
}
