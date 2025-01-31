package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.consumer.InnsendingConsumer
import no.nav.bidrag.dokument.arkiv.consumer.dto.DokumentSoknadDto
import no.nav.bidrag.dokument.arkiv.consumer.dto.HentEtterseningsoppgaveRequest
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import org.springframework.stereotype.Service

@Service
class InnsendingService(val innsendingConsumer: InnsendingConsumer) {

    fun hentEttersending(journalpost: Journalpost): DokumentSoknadDto? {
        val gjelder = journalpost.hentGjelderId()

        val ettersendingsoppgave = journalpost.ettersendingsoppgave() ?: return null
        val ettersendingsoppgaver = innsendingConsumer.hentEttersendingsoppgave(HentEtterseningsoppgaveRequest(gjelder!!, ettersendingsoppgave.skjemaId))
        return ettersendingsoppgaver.find { it.innsendingsId == ettersendingsoppgave.innsendingsId }
    }
}
