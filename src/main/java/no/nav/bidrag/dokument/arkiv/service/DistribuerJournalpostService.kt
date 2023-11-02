package no.nav.bidrag.dokument.arkiv.service

import com.google.common.base.Strings
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER
import no.nav.bidrag.dokument.arkiv.consumer.BestemKanalResponse
import no.nav.bidrag.dokument.arkiv.consumer.DistribusjonsKanal
import no.nav.bidrag.dokument.arkiv.consumer.DokdistFordelingConsumer
import no.nav.bidrag.dokument.arkiv.consumer.DokdistKanalConsumer
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer
import no.nav.bidrag.dokument.arkiv.dto.BestemDistribusjonKanalRequest
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequestInternal
import no.nav.bidrag.dokument.arkiv.dto.DistribuertTilAdresseDo
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.JournalpostUtsendingKanal
import no.nav.bidrag.dokument.arkiv.dto.LagreReturDetaljForSisteReturRequest
import no.nav.bidrag.dokument.arkiv.dto.LeggTilBeskjedPåTittel
import no.nav.bidrag.dokument.arkiv.dto.OppdaterFlaggNyDistribusjonBestiltRequest
import no.nav.bidrag.dokument.arkiv.dto.dupliserJournalpost
import no.nav.bidrag.dokument.arkiv.dto.fjern
import no.nav.bidrag.dokument.arkiv.dto.med
import no.nav.bidrag.dokument.arkiv.dto.validerAdresse
import no.nav.bidrag.dokument.arkiv.dto.validerKanDistribueres
import no.nav.bidrag.dokument.arkiv.dto.validerKanDistribueresUtenAdresse
import no.nav.bidrag.dokument.arkiv.dto.validerUtgaaendeJournalpostKanDupliseres
import no.nav.bidrag.dokument.arkiv.mapper.tilVarselTypeDto
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.arkiv.model.UgyldigDistribusjonException
import no.nav.bidrag.dokument.arkiv.model.ifFalse
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager
import no.nav.bidrag.transport.dokument.DistribuerJournalpostResponse
import no.nav.bidrag.transport.dokument.DistribuerTilAdresse
import no.nav.bidrag.transport.dokument.DistribusjonInfoDto
import no.nav.bidrag.transport.dokument.UtsendingsInfoDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

private val LOGGER = KotlinLogging.logger {}

@Service
class DistribuerJournalpostService(
    personConsumers: ResourceByDiscriminator<PersonConsumer?>,
    journalpostServices: ResourceByDiscriminator<JournalpostService?>,
    val endreJournalpostService: EndreJournalpostService,
    val opprettJournalpostService: OpprettJournalpostService,
    val dokdistFordelingConsumer: DokdistFordelingConsumer,
    val saksbehandlerInfoManager: SaksbehandlerInfoManager,
    val dokdistKanalConsumer: DokdistKanalConsumer,
    final val meterRegistry: MeterRegistry
) {
    private final val journalpostService: JournalpostService
    private final val personConsumer: PersonConsumer
    private final val distributionAntallDokumenter =
        DistributionSummary.builder(DISTRIBUSJON_DOKUMENTER_GAUGE_NAME)
            .publishPercentileHistogram()
            .publishPercentiles(0.1, 0.3, 0.5, 0.95, 0.99)
            .description("Antall dokumenter journalpost blir distribuert med")
            .register(meterRegistry)

    companion object {
        private const val DISTRIBUSJON_COUNTER_NAME = "distribuer_journalpost"
        private const val DISTRIBUSJON_DOKUMENTER_GAUGE_NAME =
            "distribuer_journalpost_antall_dokumenter"
    }

    init {
        journalpostService = journalpostServices.get(Discriminator.REGULAR_USER)
        personConsumer = personConsumers.get(Discriminator.REGULAR_USER)
    }

    fun hentDistribusjonKanal(request: BestemDistribusjonKanalRequest): BestemKanalResponse {
        val kanal = dokdistKanalConsumer.bestimDistribusjonsKanal(
            request.gjelderId,
            request.mottakerId,
            request.tema,
            request.forsendelseStoerrelse
        )
        SECURE_LOGGER.info("Hentet kanal ${kanal.distribusjonskanal} for forespørsel $request")
        return kanal
    }

    fun hentDistribusjonKanal(journalpost: Journalpost): BestemKanalResponse {
        return hentDistribusjonKanal(
            BestemDistribusjonKanalRequest(
                journalpost.hentAvsenderMottakerId(),
                journalpost.hentGjelderId()!!,
                journalpost.tema ?: "BID"
            )
        )
    }

    fun hentDistribusjonsInfo(journalpostId: Long): DistribusjonInfoDto? {
        return journalpostService.hentDistribusjonsInfo(journalpostId)
            .takeIf { it.isUtgaaendeDokument() }
            ?.let {
                SECURE_LOGGER.info("Hentet utsendinginfo $it for journalpost $journalpostId")
                val utsendingsinfo = it.utsendingsinfo
                DistribusjonInfoDto(
                    journalstatus = it.hentJournalStatus(),
                    kanal = it.kanal?.name ?: JournalpostUtsendingKanal.UKJENT.name,
                    utsendingsinfo = UtsendingsInfoDto(
                        varseltype = utsendingsinfo?.tilVarselTypeDto(),
                        adresse = utsendingsinfo?.sisteVarselSendt?.adresse
                            ?: utsendingsinfo?.smsVarselSendt?.adresse
                            ?: utsendingsinfo?.epostVarselSendt?.adresse
                            ?: utsendingsinfo?.digitalpostSendt?.adresse
                            ?: utsendingsinfo?.fysiskpostSendt?.adressetekstKonvolutt,
                        varslingstekst = utsendingsinfo?.sisteVarselSendt?.varslingstekst
                            ?: utsendingsinfo?.smsVarselSendt?.varslingstekst
                            ?: utsendingsinfo?.epostVarselSendt?.varslingstekst,
                        tittel = utsendingsinfo?.sisteVarselSendt?.tittel
                            ?: utsendingsinfo?.epostVarselSendt?.tittel
                    ),
                    distribuertAvIdent = it.hentDistribuertAvIdent(),
                    distribuertDato = it.hentDatoEkspedert() ?: it.hentDatoDokument(),
                    bestillingId = it.hentBestillingId()
                )
            }
    }

    fun bestillNyDistribusjon(
        journalpost: Journalpost,
        distribuerTilAdresse: DistribuerTilAdresse?
    ) {
        if (journalpost.tilleggsopplysninger.isNyDistribusjonBestilt()) {
            throw UgyldigDistribusjonException(
                String.format(
                    "Ny distribusjon er allerede bestilt for journalpost %s",
                    journalpost.journalpostId
                )
            )
        }
        validerAdresse(distribuerTilAdresse)
        oppdaterReturDetaljerHvisNodvendig(journalpost)

        validerUtgaaendeJournalpostKanDupliseres(journalpost)
        val request = dupliserJournalpost(journalpost) {
            fjern distribusjonMetadata true
            med eksternReferanseId "BID_duplikat_${journalpost.journalpostId}"
            med dokumenter journalpost.dokumenter
        }
        val (journalpostId) = opprettJournalpostService.opprettJournalpost(
            request,
            originalJournalpostId = journalpost.hentJournalpostIdLong(),
            skalFerdigstilles = true
        )
        distribuerJournalpost(
            journalpostId!!.toLong(),
            null,
            DistribuerJournalpostRequestInternal(distribuerTilAdresse)
        )
        val journalpostEtter = hentJournalpost(journalpost.hentJournalpostIdLong()!!)
        endreJournalpostService.lagreJournalpost(
            OppdaterFlaggNyDistribusjonBestiltRequest(
                journalpost.hentJournalpostIdLong()!!,
                journalpostEtter
            )
        )
    }

    private fun oppdaterReturDetaljerHvisNodvendig(journalpost: Journalpost) {
        if (journalpost.manglerReturDetaljForSisteRetur()) {
            if (journalpost.hentDatoRetur() == null) {
                throw UgyldigDistribusjonException("Kan ikke bestille distribusjon når det mangler returdetalj for siste returpost")
            }
            endreJournalpostService.lagreJournalpost(
                LagreReturDetaljForSisteReturRequest(
                    journalpost
                )
            )
        }
    }

    fun distribuerJournalpost(
        journalpostId: Long,
        batchId: String?,
        distribuerJournalpostRequest: DistribuerJournalpostRequestInternal
    ): DistribuerJournalpostResponse {
        val journalpost = hentJournalpost(journalpostId)
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

        if (distribuerJournalpostRequest.erLokalUtskrift()) {
            LOGGER.info("Journalpost $journalpostId er distribuert via lokal utskrift. Oppdaterer journalpost status")
            oppdaterDistribusjonsInfoLokalUtskrift(journalpostId)
            oppdaterTilleggsopplysninger(journalpostId, journalpost, erLokalUtskrift = true)
            oppdaterDokumentdatoTilIdag(journalpostId, journalpost)
            leggTilBeskrivelsePåTittelAtDokumentetErSendtPerPost(journalpostId)
            measureDistribution(journalpost, batchId, true)
            return DistribuerJournalpostResponse("JOARK-$journalpostId", null)
        }

        val distribusjonKanal = hentDistribusjonKanal(journalpost)

        val adresse =
            if (distribusjonKanal.distribusjonskanal == DistribusjonsKanal.PRINT) {
                hentOgValiderAdresse(
                    distribuerJournalpostRequest,
                    journalpost
                )
            } else {
                null
            }

        // TODO: Lagre bestillingsid når bd-arkiv er koblet mot database
        val distribuerResponse =
            dokdistFordelingConsumer.distribuerJournalpost(journalpost, batchId, adresse)
        LOGGER.info(
            "Bestillte distribusjon av journalpost $journalpostId med bestillingsId ${distribuerResponse.bestillingsId}, antall dokumenter ${journalpost.dokumenter.size} og kanal ${distribusjonKanal.distribusjonskanal}(${distribusjonKanal.regel}-${distribusjonKanal.regelBegrunnelse})."
        )

        // Distribusjonsløpet oppdaterer journalpost og overskriver alt av tilleggsopplysninger. Hent journalpost på nytt for å unngå overskrive noe som distribusjon har lagret
        oppdaterTilleggsopplysninger(journalpostId, journalpost, adresse)
        oppdaterDokumentdatoTilIdag(journalpostId, journalpost)
        measureDistribution(journalpost, batchId)
        return distribuerResponse
    }

    private fun oppdaterDokumentdatoTilIdag(journalpostId: Long, journalpostFør: Journalpost) {
        if (journalpostFør.hentDatoDokument() != LocalDate.now()) {
            val journalpostEtter = hentJournalpost(journalpostId)
            val datoDokument = journalpostFør.hentDatoDokument()?.toString()
            LOGGER.info(
                "Dokumentdato ($datoDokument) til journalpost $journalpostId er ikke samme som dato distribusjon ble bestilt. Oppdaterer dokumentdato til i dag"
            )
            endreJournalpostService.oppdaterDokumentdatoTilIdag(
                journalpostEtter.hentJournalpostIdLong()!!,
                journalpostEtter
            )
        }
    }

    private fun leggTilBeskrivelsePåTittelAtDokumentetErSendtPerPost(journalpostId: Long) {
        val beskrivelseJournalpostSendtPerPost = "dokumentet er sendt per post med vedlegg"
        val journalpostEtter = hentJournalpost(journalpostId)
        val harJournalpostTittelBeskrivelse = journalpostEtter.hentTittel()
            ?.contains(beskrivelseJournalpostSendtPerPost, true) == true
        val harHoveddokumentTittelBeskrivelse = journalpostEtter.hentHoveddokument()?.tittel
            ?.contains(beskrivelseJournalpostSendtPerPost, true) == true
        val harBeskrivelse = harJournalpostTittelBeskrivelse || harHoveddokumentTittelBeskrivelse
        if (!harBeskrivelse && !journalpostEtter.isFarskap()) {
            endreJournalpostService.lagreJournalpost(
                LeggTilBeskjedPåTittel(
                    journalpostEtter.hentJournalpostIdLong()!!,
                    journalpostEtter,
                    "dokumentet er sendt per post med vedlegg"
                )
            )
        }
    }

    private fun oppdaterTilleggsopplysninger(
        journalpostId: Long,
        journalpostFør: Journalpost,
        adresse: DistribuerTilAdresse? = null,
        erLokalUtskrift: Boolean = false
    ) {
        val journalpostEtter = hentJournalpost(journalpostId)
        leggTilEksisterendeTilleggsopplysninger(journalpostEtter, journalpostFør)
        erLokalUtskrift.ifFalse { adresse?.run { lagreAdresse(adresse, journalpostEtter) } }
        erLokalUtskrift.ifFalse { journalpostEtter.tilleggsopplysninger.setDistribusjonBestillt() }
        val saksbehandlerId = saksbehandlerInfoManager.hentSaksbehandlerBrukerId()
        saksbehandlerId?.run {
            journalpostEtter.tilleggsopplysninger.setDistribuertAvIdent(
                saksbehandlerId
            )
        }
        endreJournalpostService.oppdaterJournalpostTilleggsopplysninger(
            journalpostId,
            journalpostEtter
        )
    }

    private fun leggTilEksisterendeTilleggsopplysninger(
        journalpostEtter: Journalpost,
        journalpostFør: Journalpost
    ) {
        journalpostEtter.tilleggsopplysninger
            .addAll(
                journalpostFør.tilleggsopplysninger.filter {
                    !journalpostEtter.tilleggsopplysninger.contains(
                        it
                    )
                }
            )
    }

    private fun hentJournalpost(journalpostId: Long): Journalpost {
        return journalpostService.hentJournalpostMedFnr(journalpostId, null)
            ?: run {
                throw JournalpostIkkeFunnetException(
                    String.format(
                        "Fant ingen journalpost med id %s",
                        journalpostId
                    )
                )
            }
    }

    private fun hentOgValiderAdresse(
        distribuerJournalpostRequestInternal: DistribuerJournalpostRequestInternal,
        journalpost: Journalpost
    ): DistribuerTilAdresse? {
        val adresse = hentAdresse(distribuerJournalpostRequestInternal, journalpost)
        if (adresse != null) {
            validerAdresse(adresse)
        } else {
            validerKanDistribueresUtenAdresse(journalpost)
        }
        return adresse
    }

    private fun hentAdresse(
        distribuerJournalpostRequestInternal: DistribuerJournalpostRequestInternal,
        journalpost: Journalpost
    ): DistribuerTilAdresse? {
        if (distribuerJournalpostRequestInternal.hasAdresse()) {
            return distribuerJournalpostRequestInternal.getAdresse()
        }
        LOGGER.info(
            "Distribusjon av journalpost bestilt uten adresse. Henter adresse for mottaker. JournalpostId {}",
            journalpost.journalpostId
        )
        val adresseResponse = personConsumer.hentAdresse(journalpost.hentAvsenderMottakerId())
        if (Objects.isNull(adresseResponse)) {
            LOGGER.warn("Mottaker i journalpost {} mangler adresse", journalpost.journalpostId)
            return null
        }
        return DistribuerTilAdresse(
            adresseResponse.adresselinje1?.verdi,
            adresseResponse.adresselinje2?.verdi,
            adresseResponse.adresselinje3?.verdi,
            adresseResponse.land.verdi,
            adresseResponse.postnummer?.verdi,
            adresseResponse.poststed?.verdi
        )
    }

    fun oppdaterDistribusjonsInfoLokalUtskrift(journalpostId: Long) {
        endreJournalpostService.oppdaterDistribusjonsInfo(
            journalpostId,
            true,
            JournalpostUtsendingKanal.L
        )
    }

    fun kanDistribuereJournalpost(journalpostId: Long) {
        LOGGER.info("Sjekker om distribuere journalpost {} kan distribueres", journalpostId)
        val journalpost = journalpostService.hentJournalpost(journalpostId)
            ?: throw JournalpostIkkeFunnetException(
                String.format(
                    "Fant ingen journalpost med id %s",
                    journalpostId
                )
            )
        validerKanDistribueres(journalpost)
    }

    private fun lagreAdresse(
        distribuerTilAdresse: DistribuerTilAdresse?,
        journalpost: Journalpost
    ) {
        if (distribuerTilAdresse != null) {
            val mottakerAdresseDO = mapToAdresseDO(distribuerTilAdresse)
            if (journalpost.isUtgaaendeDokument() && mottakerAdresseDO != null) {
                journalpost.tilleggsopplysninger.addMottakerAdresse(mottakerAdresseDO)
            }
        }
    }

    private fun mapToAdresseDO(adresse: DistribuerTilAdresse?): DistribuertTilAdresseDo? {
        return if (adresse != null) {
            DistribuertTilAdresseDo(
                adresselinje1 = adresse.adresselinje1,
                adresselinje2 = adresse.adresselinje2,
                adresselinje3 = adresse.adresselinje3,
                land = adresse.land!!,
                poststed = adresse.poststed,
                postnummer = adresse.postnummer
            )
        } else {
            null
        }
    }

    private fun measureDistribution(
        journalpost: Journalpost,
        batchId: String?,
        lokalUtskrift: Boolean = false
    ) {
        try {
            val kanal =
                if (lokalUtskrift) "LOKAL_UTSKRIFT" else hentDistribusjonKanal(journalpost).distribusjonskanal.name
            meterRegistry.counter(
                DISTRIBUSJON_COUNTER_NAME,
                "batchId",
                if (Strings.isNullOrEmpty(batchId)) "NONE" else batchId,
                "enhet", journalpost.journalforendeEnhet,
                "tema", journalpost.tema,
                "kanal", kanal
            ).increment()

            distributionAntallDokumenter.record(journalpost.dokumenter.size.toDouble())
        } catch (e: Exception) {
            LOGGER.error("Det skjedde en feil ved oppdatering av metrikk", e)
        }
    }
}
