package no.nav.bidrag.dokument.arkiv.service

import com.google.common.base.Strings
import io.micrometer.core.instrument.MeterRegistry
import no.nav.bidrag.dokument.arkiv.consumer.DokdistFordelingConsumer
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequestInternal
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.JournalpostUtsendingKanal
import no.nav.bidrag.dokument.arkiv.dto.LagreAdresseRequest
import no.nav.bidrag.dokument.arkiv.dto.LagreReturDetaljForSisteReturRequest
import no.nav.bidrag.dokument.arkiv.dto.OppdaterFlaggNyDistribusjonBestiltRequest
import no.nav.bidrag.dokument.arkiv.dto.dupliserJournalpost
import no.nav.bidrag.dokument.arkiv.dto.fjern
import no.nav.bidrag.dokument.arkiv.dto.med
import no.nav.bidrag.dokument.arkiv.dto.validerAdresse
import no.nav.bidrag.dokument.arkiv.dto.validerKanDistribueres
import no.nav.bidrag.dokument.arkiv.dto.validerKanDistribueresUtenAdresse
import no.nav.bidrag.dokument.arkiv.dto.validerUtgaaendeJournalpostKanDupliseres
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.arkiv.model.UgyldigDistribusjonException
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Objects

@Service
class DistribuerJournalpostService(
    personConsumers: ResourceByDiscriminator<PersonConsumer?>,
    journalpostServices: ResourceByDiscriminator<JournalpostService?>,
    val endreJournalpostService: EndreJournalpostService,
    val opprettJournalpostService: OpprettJournalpostService,
    val dokdistFordelingConsumer: DokdistFordelingConsumer,
    val meterRegistry: MeterRegistry
) {
    private final val journalpostService: JournalpostService
    private final val personConsumer: PersonConsumer

    init {
        journalpostService = journalpostServices.get(Discriminator.REGULAR_USER)
        personConsumer = personConsumers.get(Discriminator.REGULAR_USER)
    }

    fun bestillNyDistribusjon(journalpost: Journalpost, distribuerTilAdresse: DistribuerTilAdresse?) {
        if (journalpost.tilleggsopplysninger.isNyDistribusjonBestilt()) {
            throw UgyldigDistribusjonException(String.format("Ny distribusjon er allerede bestilt for journalpost %s", journalpost.journalpostId))
        }
        validerAdresse(distribuerTilAdresse)
        oppdaterReturDetaljerHvisNodvendig(journalpost)

        validerUtgaaendeJournalpostKanDupliseres(journalpost)
        val request = dupliserJournalpost(journalpost){
            fjern distribusjonMetadata true
            med eksternReferanseId "BID_duplikat_${journalpost.journalpostId}"
            med dokumenter journalpost.dokumenter
        }
        val (journalpostId) = opprettJournalpostService.opprettJournalpost(request, originalJournalpostId = journalpost.hentJournalpostIdLong(), skalFerdigstilles = true)
        distribuerJournalpost(journalpostId!!.toLong(), null, DistribuerJournalpostRequestInternal(distribuerTilAdresse))
        endreJournalpostService.lagreJournalpost(OppdaterFlaggNyDistribusjonBestiltRequest(journalpost.hentJournalpostIdLong()!!, journalpost))
    }

    private fun oppdaterReturDetaljerHvisNodvendig(journalpost: Journalpost) {
        if (journalpost.manglerReturDetaljForSisteRetur()) {
            if (journalpost.hentDatoRetur() == null) {
                throw UgyldigDistribusjonException("Kan ikke bestille distribusjon når det mangler returdetalj for siste returpost")
            }
            endreJournalpostService.lagreJournalpost(LagreReturDetaljForSisteReturRequest(journalpost))
        }
    }

    fun distribuerJournalpost(
        journalpostId: Long,
        batchId: String?,
        distribuerJournalpostRequest: DistribuerJournalpostRequestInternal
    ): DistribuerJournalpostResponse {
        val journalpost = journalpostService.hentJournalpost(journalpostId)
            .orElseThrow { JournalpostIkkeFunnetException(String.format("Fant ingen journalpost med id %s", journalpostId)) }
        journalpostService.populerMedTilknyttedeSaker(journalpost)
        if (journalpost.tilleggsopplysninger.isDistribusjonBestilt() || journalpost.journalstatus == JournalStatus.EKSPEDERT) {
            LOGGER.warn(
                "Distribusjon er allerede bestillt for journalpostid {}{}. Stopper videre behandling",
                journalpostId,
                if (batchId != null) String.format(" med batchId %s", batchId) else ""
            )
            return DistribuerJournalpostResponse("JOARK-$journalpostId", null)
        }
        validerKanDistribueres(journalpost)

        if (distribuerJournalpostRequest.erLokalUtskrift()){
            LOGGER.info("Journalpost $journalpostId er distribuert via lokal utskrift. Oppdaterer journalpost status")
            oppdaterDistribusjonsInfoLokalUtskrift(journalpostId)
            return DistribuerJournalpostResponse("JOARK-$journalpostId", null)
        }

        val adresse = hentAdresse(distribuerJournalpostRequest, journalpost)
        if (adresse != null) {
            validerAdresse(adresse)
            lagreAdresse(journalpostId, adresse, journalpost)
        } else {
            validerKanDistribueresUtenAdresse(journalpost)
        }

        //TODO: Lagre bestillingsid når bd-arkiv er koblet mot database
        val distribuerResponse = dokdistFordelingConsumer.distribuerJournalpost(journalpost, batchId, adresse)
        LOGGER.info("Bestillt distribusjon av journalpost {} med bestillingsId {}", journalpostId, distribuerResponse.bestillingsId)
        endreJournalpostService.oppdaterJournalpostDistribusjonBestiltStatus(journalpostId, journalpost)
        measureDistribution(batchId)
        return distribuerResponse
    }

    private fun hentAdresse(
        distribuerJournalpostRequestInternal: DistribuerJournalpostRequestInternal,
        journalpost: Journalpost
    ): DistribuerTilAdresse? {
        if (distribuerJournalpostRequestInternal.hasAdresse()) {
            return distribuerJournalpostRequestInternal.getAdresse()
        }
        LOGGER.info("Distribusjon av journalpost bestilt uten adresse. Henter adresse for mottaker. JournalpostId {}", journalpost.journalpostId)
        val adresseResponse = personConsumer.hentAdresse(journalpost.hentAvsenderMottakerId())
        if (Objects.isNull(adresseResponse)) {
            LOGGER.warn("Mottaker i journalpost {} mangler adresse", journalpost.journalpostId)
            return null
        }
        return DistribuerTilAdresse(
            adresseResponse.adresselinje1,
            adresseResponse.adresselinje2,
            adresseResponse.adresselinje3,
            adresseResponse.land,
            adresseResponse.postnummer,
            adresseResponse.poststed
        )
    }

    fun oppdaterDistribusjonsInfoLokalUtskrift(journalpostId: Long) {
        endreJournalpostService.oppdaterDistribusjonsInfo(journalpostId, true, JournalpostUtsendingKanal.L)
    }

    fun kanDistribuereJournalpost(journalpostId: Long?) {
        LOGGER.info("Sjekker om distribuere journalpost {} kan distribueres", journalpostId)
        val journalpost = journalpostService.hentJournalpost(journalpostId)
            .orElseThrow { JournalpostIkkeFunnetException(String.format("Fant ingen journalpost med id %s", journalpostId)) }
        validerKanDistribueres(journalpost)
    }

    private fun lagreAdresse(journalpostId: Long?, distribuerTilAdresse: DistribuerTilAdresse?, journalpost: Journalpost) {
        if (distribuerTilAdresse != null) {
            endreJournalpostService.lagreJournalpost(LagreAdresseRequest(journalpostId!!, distribuerTilAdresse, journalpost))
        }
    }

    private fun measureDistribution(batchId: String?) {
        try {
            meterRegistry.counter(
                DISTRIBUSJON_COUNTER_NAME,
                "batchId", if (Strings.isNullOrEmpty(batchId)) "NONE" else batchId
            ).increment()
        } catch (e: Exception) {
            LOGGER.error("Det skjedde en feil ved oppdatering av metrikk", e)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DistribuerJournalpostService::class.java)
        private const val DISTRIBUSJON_COUNTER_NAME = "distribuer_journalpost"
    }
}