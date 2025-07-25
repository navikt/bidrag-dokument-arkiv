package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.SECURE_LOGGER
import no.nav.bidrag.dokument.arkiv.consumer.OppgaveConsumer
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillOppgaveRequest
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.LeggTilKommentarPaaOppgave
import no.nav.bidrag.dokument.arkiv.dto.OppgaveData
import no.nav.bidrag.dokument.arkiv.dto.OppgaveEnhet
import no.nav.bidrag.dokument.arkiv.dto.OpprettOppgaveFagpostRequest
import no.nav.bidrag.dokument.arkiv.dto.OpprettOppgaveRequest
import no.nav.bidrag.dokument.arkiv.dto.OpprettVurderDokumentOppgaveRequest
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerMedEnhet
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.OppgaveSokParametre
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonDto
import org.slf4j.LoggerFactory
import java.util.function.Consumer

class OppgaveService(
    private val personConsumers: ResourceByDiscriminator<PersonConsumer>,
    private val oppgaveConsumers: ResourceByDiscriminator<OppgaveConsumer>,
    private val saksbehandlerInfoManager: SaksbehandlerInfoManager,
) {

    fun leggTilKommentarPaaJournalforingsoppgave(journalpost: Journalpost, saksbehandlerMedEnhet: SaksbehandlerMedEnhet, kommentar: String) {
        val oppgaver = finnJournalforingOppgaverForJournalpost(journalpost.hentJournalpostIdLong())
        oppgaver.filter { it.tildeltEnhetsnr != OppgaveEnhet.FAGPOST }.forEach(
            Consumer { oppgave: OppgaveData ->
                oppgaveConsumers.get(Discriminator.SERVICE_USER)
                    .patchOppgave(
                        LeggTilKommentarPaaOppgave(
                            oppgave,
                            saksbehandlerMedEnhet.enhetsnummer,
                            saksbehandlerMedEnhet.hentSaksbehandlerInfo(),
                            kommentar,
                        ),
                    )
                LOGGER.info("Journalføringsoppgave ${oppgave.id} for journalpost ${journalpost.journalpostId} ble overført til fagpost")
            },
        )
    }

    fun opprettOppgaveTilFagpost(opprettOppgaveFagpostRequest: OpprettOppgaveFagpostRequest) {
        if (opprettOppgaveFagpostRequest.hasGjelderId()) {
            val aktorId = hentAktorId(opprettOppgaveFagpostRequest.hentGjelderId())
            opprettOppgaveFagpostRequest.aktoerId = aktorId
        }
        opprettOppgave(opprettOppgaveFagpostRequest)
    }

    fun opprettVurderDokumentOppgave(journalpost: Journalpost, journalpostId: String, tildeltEnhetsnr: String, tema: String, kommentar: String?) {
        val aktorId = hentAktorId(journalpost.hentGjelderId())
        opprettOppgave(
            OpprettVurderDokumentOppgaveRequest(
                journalpost,
                journalpostId,
                tildeltEnhetsnr,
                tema,
                aktorId!!,
                hentSaksbehandlerMedEnhet(journalpost.journalforendeEnhet),
                kommentar,
            ),
        )
    }

    fun ferdigstillVurderDokumentOppgaver(journalpostId: Long, enhetsnr: String) {
        val oppgaver = finnVurderDokumentOppgaverForJournalpost(journalpostId)
        oppgaver.forEach(Consumer { oppgave: OppgaveData -> ferdigstillOppgave(oppgave, enhetsnr) })
    }

    private fun ferdigstillOppgave(oppgaveData: OppgaveData, enhetsnr: String) {
        LOGGER.info(
            "Ferdigstiller oppgave {} med oppgavetype {}",
            oppgaveData.id,
            oppgaveData.oppgavetype,
        )
        oppgaveConsumers.get(Discriminator.SERVICE_USER)
            .patchOppgave(FerdigstillOppgaveRequest(oppgaveData, enhetsnr))
    }

    private fun hentSaksbehandlerMedEnhet(journalforendeEnhet: String?): SaksbehandlerMedEnhet = saksbehandlerInfoManager.hentSaksbehandler()
        .map { saksbehandler: Saksbehandler -> saksbehandler.tilEnhet(journalforendeEnhet) }
        .orElseGet { SaksbehandlerMedEnhet(Saksbehandler(), journalforendeEnhet!!) }

    private fun opprettOppgave(request: OpprettOppgaveRequest) {
        val oppgaveId = oppgaveConsumers.get(Discriminator.REGULAR_USER).opprett(request)
        SECURE_LOGGER.info("Oppgave opprettet med id=$oppgaveId og request=${request.asJson()}")
    }

    private fun hentAktorId(gjelder: String?): String? {
        if (gjelder == null) return null
        return personConsumers.get(Discriminator.SERVICE_USER).hentPerson(gjelder)
            .orElseGet {
                PersonDto(
                    ident = Personident(gjelder),
                    aktørId = gjelder,
                )
            }.aktørId
    }

    private fun finnVurderDokumentOppgaverForJournalpost(journalpostId: Long): List<OppgaveData> {
        val parametre = OppgaveSokParametre()
            .leggTilFagomrade("BID")
            .leggTilJournalpostId(journalpostId)
            .brukVurderDokumentSomOppgaveType()
        return oppgaveConsumers.get(Discriminator.SERVICE_USER).finnOppgaver(parametre)?.oppgaver
            ?: emptyList()
    }

    private fun finnJournalforingOppgaverForJournalpost(journalpostId: Long?): List<OppgaveData> {
        val parametre = OppgaveSokParametre()
            .leggTilFagomrade("BID")
            .leggTilJournalpostId(journalpostId!!)
            .brukJournalforingSomOppgaveType()
        return oppgaveConsumers.get(Discriminator.SERVICE_USER).finnOppgaver(parametre)?.oppgaver
            ?: emptyList()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OppgaveService::class.java)
    }
}
