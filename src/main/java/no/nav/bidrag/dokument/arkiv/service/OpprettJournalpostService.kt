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
import no.nav.bidrag.dokument.arkiv.dto.OpprettJournalpost
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.dto.hentGjelderIdent
import no.nav.bidrag.dokument.arkiv.dto.hentGjelderType
import no.nav.bidrag.dokument.arkiv.dto.hentJournalførendeEnhet
import no.nav.bidrag.dokument.arkiv.dto.opprettDokumentVariant
import no.nav.bidrag.dokument.arkiv.dto.validerKanOppretteJournalpost
import no.nav.bidrag.dokument.arkiv.dto.validerKanOppretteJournalpost2
import no.nav.bidrag.dokument.arkiv.dto.validerUtgaaendeJournalpostKanDupliseres
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
import java.util.function.Consumer

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
        validerKanOppretteJournalpost2(request)
        val opprettJournalpostRequest = mapTilJoarkOpprettJournalpostRequest(request)

        val response = dokarkivConsumer.opprett(opprettJournalpostRequest, request.skalJournalføres)
        LOGGER.info("Opprettet ny journalpost {}", response.journalpostId)
        SECURE_LOGGER.info("Opprettet ny journalpost {}", response)

        validerOpprettJournalpostResponse(request, response)
        knyttSakerOgLagreSaksbehandlerForJournalførtJournalpost(response, request.tilknyttSaker)

        return OpprettJournalpostResponse(
            journalpostId = response.journalpostId.toString(),
            dokumenter = response.dokumenter?.map {
                OpprettDokumentDto(dokumentreferanse = it.dokumentInfoId)
            } ?: emptyList()
        )
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

    fun validerOpprettJournalpostResponse(request: OpprettJournalpostRequest, response: JoarkOpprettJournalpostResponse){
        if (request.skalJournalføres && !response.journalpostferdigstilt) {
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

    fun opprettOgJournalforJournalpost(request: OpprettJournalpost, knyttTilSaker: List<String>): JoarkOpprettJournalpostResponse {
        val tilknyttetSak = knyttTilSaker[0]
        request.medSak(tilknyttetSak)
        populerMedDokumenterByteData(request)
        validerKanOppretteJournalpost(request)
        val opprettJournalpostResponse = dokarkivConsumer.opprett(request, true)
        LOGGER.info("Opprettet ny journalpost {}", opprettJournalpostResponse.journalpostId)
        SECURE_LOGGER.info("Opprettet ny journalpost {}", opprettJournalpostResponse)
        try {
            if (!opprettJournalpostResponse.journalpostferdigstilt) {
                val message = String.format(
                    "Kunne ikke journalføre journalpost %s med feilmelding %s",
                    opprettJournalpostResponse.journalpostId,
                    opprettJournalpostResponse.melding
                )
                LOGGER.error(message)
                throw KunneIkkeJournalforeOpprettetJournalpost(message)
            }
            val opprettetJournalpost = hentJournalpost(opprettJournalpostResponse.journalpostId)
            endreJournalpostService.lagreSaksbehandlerIdentForJournalfortJournalpost(opprettetJournalpost)
            knyttSakerTilOpprettetJournalpost(opprettetJournalpost, knyttTilSaker)
        } catch (e: Exception) {
            LOGGER.error(
                "Etterbehandling av opprettet journalpost feilet (knytt til flere saker eller lagre saksbehandler ident). Fortsetter behandling da feilen må behandles manuelt.",
                e
            )
        }
        return opprettJournalpostResponse
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

    fun populerMedDokumenterByteData(request: OpprettJournalpost) {
        if (request.originalJournalpostId != null) {
            request.dokumenter.forEach(Consumer { dok: JoarkOpprettJournalpostRequest.Dokument ->
                if (dok.dokumentvarianter.isEmpty() && Strings.isNotEmpty(dok.dokumentInfoId)) {
                    val dokument = hentDokument(request.originalJournalpostId!!, dok.dokumentInfoId!!)
                    dok.dokumentvarianter = listOf(opprettDokumentVariant(null, dokument))
                }
            })
        }
    }

    fun dupliserUtgaaendeJournalpost(journalpost: Journalpost, removeDistribusjonMetadata: Boolean): JoarkOpprettJournalpostResponse {
        validerUtgaaendeJournalpostKanDupliseres(journalpost)
        val dokumenter = hentDokumenter(journalpost)
        val opprettJournalpostRequest = createOpprettJournalpostRequest(journalpost, dokumenter, removeDistribusjonMetadata)
        opprettJournalpostRequest.eksternReferanseId = String.format("BID_duplikat_%s", journalpost.journalpostId)
        val opprettJournalpostResponse = dokarkivConsumer.opprett(opprettJournalpostRequest, true)
        LOGGER.info("Duplisert journalpost {}, ny journalpostId {}", journalpost.journalpostId, opprettJournalpostResponse.journalpostId)
        return opprettJournalpostResponse
    }

    private fun createOpprettJournalpostRequest(
        journalpost: Journalpost,
        dokumenter: Map<String, ByteArray>,
        removeDistribusjonMetadata: Boolean
    ): JoarkOpprettJournalpostRequest {
        val opprettJournalpostBuilderRequest = OpprettJournalpost()
            .dupliser(journalpost, dokumenter)
            .medKanal(null)
        if (removeDistribusjonMetadata) {
            val tillegssopplysninger = TilleggsOpplysninger()
            tillegssopplysninger.addAll(journalpost.tilleggsopplysninger)
            tillegssopplysninger.removeDistribusjonMetadata()
            tillegssopplysninger.lockAllReturDetaljerLog()
            opprettJournalpostBuilderRequest.tilleggsopplysninger = tillegssopplysninger
        }
        return opprettJournalpostBuilderRequest
    }

    private fun hentDokumenter(journalpost: Journalpost): Map<String, ByteArray> {
        return journalpost.dokumenter
            .associate { it.dokumentInfoId!! to hentDokument(journalpost.hentJournalpostIdLong()!!, it.dokumentInfoId!!) }
    }

    private fun hentDokument(journalpostId: Long, dokumentId: String): ByteArray {
        return dokumentService.hentDokument(journalpostId, dokumentId).body!!
    }
}