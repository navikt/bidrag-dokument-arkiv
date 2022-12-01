package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottakerIdType
import no.nav.bidrag.dokument.arkiv.dto.JoarkJournalpostType
import no.nav.bidrag.dokument.arkiv.dto.JoarkMottakUtsendingKanal
import no.nav.bidrag.dokument.arkiv.dto.JoarkOpprettJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.JoarkOpprettJournalpostResponse
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.hentGjelderIdent
import no.nav.bidrag.dokument.arkiv.dto.hentGjelderType
import no.nav.bidrag.dokument.arkiv.dto.hentJournalførendeEnhet
import no.nav.bidrag.dokument.arkiv.dto.opprettDokumentVariant
import no.nav.bidrag.dokument.arkiv.dto.validerKanOppretteJournalpost
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.HentJournalpostFeilet
import no.nav.bidrag.dokument.arkiv.model.KunneIkkeJournalforeOpprettetJournalpost
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.dto.JournalpostType
import no.nav.bidrag.dokument.dto.MottakUtsendingKanal
import no.nav.bidrag.dokument.dto.OpprettDokumentDto
import no.nav.bidrag.dokument.dto.OpprettJournalpostRequest
import no.nav.bidrag.dokument.dto.OpprettJournalpostResponse
import org.apache.logging.log4j.util.Strings
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Base64

@Service
class OpprettJournalpostService(
    dokarkivConsumers: ResourceByDiscriminator<DokarkivConsumer?>,
    safConsumers: ResourceByDiscriminator<SafConsumer?>,
    private val dokumentService: DokumentService,
    private val endreJournalpostService: EndreJournalpostService
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
        validerKanOppretteJournalpost(request)
        val opprettJournalpostRequest = mapTilJoarkOpprettJournalpostRequest(request)
        return opprettJournalpost(opprettJournalpostRequest, request.tilknyttSaker, skalJournalføres = request.skalJournalføres)
    }

    fun opprettOgJournalforJournalpost(_request: JoarkOpprettJournalpostRequest, knyttTilSaker: List<String> = emptyList(), originalJournalpostId: Long?): OpprettJournalpostResponse {
        val tilknyttetSak = if(knyttTilSaker.isNotEmpty()) knyttTilSaker[0] else _request.sak?.fagsakId
        var request = _request.copy(sak = JoarkOpprettJournalpostRequest.OpprettJournalpostSak(tilknyttetSak))
        request = populerMedDokumenterByteData(request, originalJournalpostId)
        validerKanOppretteJournalpost(request)
        return opprettJournalpost(request, knyttTilSaker, true)
    }

    private fun opprettJournalpost(request: JoarkOpprettJournalpostRequest, knyttTilSaker: List<String> = emptyList(), skalJournalføres: Boolean = false): OpprettJournalpostResponse {
        val response = dokarkivConsumer.opprett(request, skalJournalføres)
        LOGGER.info("Opprettet ny journalpost {}", response.journalpostId)
        SECURE_LOGGER.info("Opprettet ny journalpost {}", response)

        validerOpprettJournalpostResponse(skalJournalføres, response)

        try {
            knyttSakerOgLagreSaksbehandlerForJournalførtJournalpost(response, knyttTilSaker)
        } catch (e: Exception) {
            LOGGER.error(
                "Etterbehandling av opprettet journalpost feilet (knytt til flere saker eller lagre saksbehandler ident). Fortsetter behandling da feilen må behandles manuelt.",
                e
            )
        }
        return OpprettJournalpostResponse(
            journalpostId = response.journalpostId.toString(),
            dokumenter = response.dokumenter?.map {
                OpprettDokumentDto(dokumentreferanse = it.dokumentInfoId)
            } ?: emptyList()
        )
    }

    private fun validerOpprettJournalpostResponse(skalJournalføres: Boolean, response: JoarkOpprettJournalpostResponse){
        if (skalJournalføres && !response.journalpostferdigstilt) {
            val message = String.format(
                "Kunne ikke journalføre journalpost %s med feilmelding %s",
                response.journalpostId,
                response.melding
            )
            LOGGER.error(message)
            throw KunneIkkeJournalforeOpprettetJournalpost(message)
        }

    }

    private fun knyttSakerOgLagreSaksbehandlerForJournalførtJournalpost(opprettJournalpostResponse: JoarkOpprettJournalpostResponse, tilknyttSaker: List<String>){
        if (opprettJournalpostResponse.journalpostferdigstilt){
            val opprettetJournalpost = hentJournalpost(opprettJournalpostResponse.journalpostId)
            endreJournalpostService.lagreSaksbehandlerIdentForJournalfortJournalpost(opprettetJournalpost)
            knyttSakerTilOpprettetJournalpost(opprettetJournalpost, tilknyttSaker)
        }
    }

    private fun hentJournalpost(journalpostId: Long?): Journalpost {
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
            .forEach { saksnummer: String? -> endreJournalpostService.tilknyttTilSak(saksnummer, opprettetJournalpost) }
    }

    private fun populerMedDokumenterByteData(request: JoarkOpprettJournalpostRequest, originalJournalpostId: Long?): JoarkOpprettJournalpostRequest {
        if (originalJournalpostId != null) {
            return request.copy(
                dokumenter = request.dokumenter.map {
                    if (it.dokumentvarianter.isEmpty() && Strings.isNotEmpty(it.dokumentInfoId)) {
                        val dokument = hentDokument(originalJournalpostId, it.dokumentInfoId!!)
                        it.copy(dokumentvarianter = listOf(opprettDokumentVariant(null, dokument)))
                    } else it
                }
            )
        }
        return request
    }

    private fun mapTilJoarkOpprettJournalpostRequest(request: OpprettJournalpostRequest): JoarkOpprettJournalpostRequest{
        val hoveddokument = request.dokumenter[0]
        val tilknyttSaker = request.tilknyttSaker
        val hovedTittel = hoveddokument.tittel
        val erInngaendeOgSkalIkkeJournalfores = request.journalposttype == JournalpostType.INNGÅENDE && !request.skalJournalføres
        val erInngaende = request.journalposttype == JournalpostType.INNGÅENDE
        return JoarkOpprettJournalpostRequest(
            tittel = hovedTittel,
            tema = request.tema ?: "BID",
            journalpostType = when (request.journalposttype) {
                JournalpostType.UTGÅENDE, JournalpostType.UTGAAENDE -> JoarkJournalpostType.UTGAAENDE //DEPRECATED
                JournalpostType.INNGÅENDE -> JoarkJournalpostType.INNGAAENDE
                JournalpostType.NOTAT -> JoarkJournalpostType.NOTAT
                else -> JoarkJournalpostType.UTGAAENDE
            },
            eksternReferanseId = request.referanseId,
            behandlingstema = request.behandlingstema,
            journalfoerendeEnhet = if (erInngaendeOgSkalIkkeJournalfores) null else request.hentJournalførendeEnhet(),
            kanal = when (request.journalposttype) {
                JournalpostType.UTGÅENDE, JournalpostType.UTGAAENDE ->
                    if (request.kanal == MottakUtsendingKanal.LOKAL_UTSKRIFT) JoarkMottakUtsendingKanal.L
                    else null // Settes av distribusjonsløpet
                JournalpostType.INNGÅENDE -> // TODO: Joark få en ny mottakskanal for skanning fra Bidrag
                    if (request.kanal == MottakUtsendingKanal.SKANNING_BIDRAG) JoarkMottakUtsendingKanal.SKAN_BID
                    else JoarkMottakUtsendingKanal.NAV_NO
                else -> null
            }?.name,
            datoMottatt = if (erInngaende) request.datoMottatt?.toString() ?: LocalDateTime.now().toString() else null,
            bruker = JoarkOpprettJournalpostRequest.OpprettJournalpostBruker(
                id = request.hentGjelderIdent(),
                idType = request.hentGjelderType()?.name
            ),
            avsenderMottaker = if (request.journalposttype != JournalpostType.NOTAT) JoarkOpprettJournalpostRequest.OpprettJournalpostAvsenderMottaker(
                request.avsenderMottaker?.navn,
                request.avsenderMottaker?.ident,
                request.avsenderMottaker?.type?.let { AvsenderMottakerIdType.valueOf(it.name)  } ?: AvsenderMottakerIdType.FNR
            ) else null,
            sak = if (erInngaendeOgSkalIkkeJournalfores || tilknyttSaker.isEmpty()) null
            else JoarkOpprettJournalpostRequest.OpprettJournalpostSak(tilknyttSaker[0]),
            dokumenter = request.dokumenter.mapIndexed { i, it ->
                JoarkOpprettJournalpostRequest.Dokument(
                    brevkode = it.brevkode,
                    tittel = it.tittel,
                    dokumentvarianter = listOf(opprettDokumentVariant(null, it.fysiskDokument ?: Base64.getDecoder().decode(it.dokument)))
                )
            }.toMutableList()
        )

    }

    private fun hentDokument(journalpostId: Long, dokumentId: String): ByteArray {
        return dokumentService.hentDokument(journalpostId, dokumentId).body!!
    }
}