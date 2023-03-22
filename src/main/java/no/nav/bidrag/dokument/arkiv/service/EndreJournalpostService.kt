package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivKnyttTilSakConsumer
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.JournalpostUtsendingKanal
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakRequest
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilSakRequest
import no.nav.bidrag.dokument.arkiv.dto.LagreJournalfortAvIdentRequest
import no.nav.bidrag.dokument.arkiv.dto.LagreJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.OppdaterDokumentdatoTilIdag
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostDistribusjonsInfoRequest
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostTilleggsopplysninger
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException
import no.nav.bidrag.dokument.arkiv.model.LagreSaksbehandlerIdentForJournalfortJournalpostFeilet
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager
import org.slf4j.LoggerFactory
import java.util.Objects
import java.util.function.Consumer
import java.util.stream.Collectors

class EndreJournalpostService(
    private val journalpostService: JournalpostService,
    private val dokarkivConsumer: DokarkivConsumer,
    private val dokarkivKnyttTilSakConsumer: DokarkivKnyttTilSakConsumer,
    private val hendelserProducer: HendelserProducer, private val saksbehandlerInfoManager: SaksbehandlerInfoManager
) {
    fun endre(journalpostId: Long, endreJournalpostCommand: EndreJournalpostCommandIntern) {
        var journalpost = hentJournalpost(journalpostId)
        endreJournalpostCommand.sjekkGyldigEndring(journalpost)
        lagreJournalpost(journalpostId, endreJournalpostCommand, journalpost)
        journalfoerJournalpostNarMottaksregistrert(endreJournalpostCommand, journalpost)
        if (journalpost.kanTilknytteSaker() || endreJournalpostCommand.skalJournalfores()) {
            journalpost = hentJournalpost(journalpostId)
            tilknyttSakerTilJournalfoertJournalpost(endreJournalpostCommand, journalpost)
        }
        publiserJournalpostEndretHendelse(journalpost, journalpostId, endreJournalpostCommand)
    }

    private fun publiserJournalpostEndretHendelse(
        journalpost: Journalpost,
        journalpostId: Long,
        endreJournalpostCommand: EndreJournalpostCommandIntern
    ) {
        if (journalpost.isInngaaendeDokument()) {
            hendelserProducer.publishJournalpostUpdated(journalpostId, endreJournalpostCommand.enhet)
        }
    }

    fun lagreJournalpost(oppdaterJournalpostRequest: OppdaterJournalpostRequest): OppdaterJournalpostResponse {
        return dokarkivConsumer.endre(oppdaterJournalpostRequest)
    }

    private fun lagreJournalpost(journalpostId: Long, endreJournalpostCommand: EndreJournalpostCommandIntern, journalpost: Journalpost) {
        val oppdaterJournalpostRequest = LagreJournalpostRequest(journalpostId, endreJournalpostCommand, journalpost)
        lagreJournalpost(oppdaterJournalpostRequest)
        if (Objects.nonNull(oppdaterJournalpostRequest.sak)) {
            journalpost.sak = Sak(oppdaterJournalpostRequest.sak!!.fagsakId)
        }
    }

    private fun journalfoerJournalpostNarMottaksregistrert(endreJournalpostCommand: EndreJournalpostCommandIntern, journalpost: Journalpost) {
        if (endreJournalpostCommand.skalJournalfores() && journalpost.isStatusMottatt()) {
            val journalpostId = journalpost.hentJournalpostIdLong()
            journalfoerJournalpost(journalpostId, endreJournalpostCommand.enhet, journalpost)
            journalpost.journalstatus = JournalStatus.JOURNALFOERT
        }
    }

    fun lagreSaksbehandlerIdentForJournalfortJournalpost(journalpost: Journalpost, saksbehandlerIdent: String?) {
        try {
            lagreJournalpost(
                LagreJournalfortAvIdentRequest(
                    journalpost.hentJournalpostIdLong()!!,
                    journalpost,
                    (saksbehandlerIdent ?: saksbehandlerInfoManager.hentSaksbehandlerBrukerId())!!
                )
            )
        } catch (e: Exception) {
            throw LagreSaksbehandlerIdentForJournalfortJournalpostFeilet(
                String.format(
                    "Lagring av saksbehandler ident for journalført journalpost %s feilet",
                    journalpost.journalpostId
                ), e
            )
        }
    }

    private fun tilknyttSakerTilJournalfoertJournalpost(endreJournalpostCommand: EndreJournalpostCommandIntern, journalpost: Journalpost) {
        if (journalpost.kanTilknytteSaker()) {
            journalpostService.populerMedTilknyttedeSaker(journalpost)
            endreJournalpostCommand.hentTilknyttetSaker().stream()
                .filter { sak: String? -> !journalpost.hentTilknyttetSaker().contains(sak) }
                .collect(Collectors.toSet())
                .forEach(Consumer { saksnummer: String? -> tilknyttTilSak(saksnummer, journalpost) })
        }
    }

    fun tilknyttTilSak(saksnummer: String?, journalpost: Journalpost) {
        tilknyttTilSak(saksnummer, if (journalpost.isBidragTema()) null else "BID", journalpost)
    }

    fun tilknyttTilSak(saksnummer: String?, tema: String?, journalpost: Journalpost) {
        val knyttTilAnnenSakRequest: KnyttTilAnnenSakRequest = KnyttTilSakRequest(saksnummer!!, journalpost, tema)
        val (nyJournalpostId) = dokarkivKnyttTilSakConsumer.knyttTilSak(journalpost.hentJournalpostIdLong(), knyttTilAnnenSakRequest)
        LOGGER.info(
            "Tilknyttet journalpost {} til sak {} med ny journalpostId {} og tema {}",
            journalpost.journalpostId,
            saksnummer,
            nyJournalpostId,
            tema
        )
        journalpost.leggTilTilknyttetSak(saksnummer)
    }

    private fun journalfoerJournalpost(journalpostId: Long?, enhet: String?, journalpost: Journalpost) {
        val journalforRequest = FerdigstillJournalpostRequest(journalpostId!!, enhet!!)
        dokarkivConsumer.ferdigstill(journalforRequest)
        LOGGER.info("Journalpost med id $journalpostId er journalført")
        lagreSaksbehandlerIdentForJournalfortJournalpost(journalpost, null)
    }

    fun oppdaterJournalpostDistribusjonBestiltStatus(journalpostId: Long, journalpost: Journalpost) {
        lagreJournalpost(OppdaterJournalpostDistribusjonsInfoRequest(journalpostId, journalpost))
    }

    fun oppdaterDokumentdatoTilIdag(journalpostId: Long, journalpost: Journalpost) {
        lagreJournalpost(OppdaterDokumentdatoTilIdag(journalpostId, journalpost))
    }

    fun oppdaterJournalpostTilleggsopplysninger(journalpostId: Long, journalpost: Journalpost) {
        lagreJournalpost(OppdaterJournalpostTilleggsopplysninger(journalpostId, journalpost))
    }

    private fun hentJournalpost(journalpostId: Long): Journalpost {
        LOGGER.info("Henter jouranlpost $journalpostId")
        return journalpostService.hentJournalpost(journalpostId).orElseThrow {
            JournalpostIkkeFunnetException("Kunne ikke finne journalpost med id: $journalpostId")
        }
    }

    fun oppdaterDistribusjonsInfo(journalpostId: Long?, settStatusEkspedert: Boolean, utsendingsKanal: JournalpostUtsendingKanal?) {
        dokarkivConsumer.oppdaterDistribusjonsInfo(journalpostId, settStatusEkspedert, utsendingsKanal)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(EndreJournalpostService::class.java)
    }
}