package no.nav.bidrag.dokument.arkiv.service.utvidelser

import no.nav.bidrag.dokument.arkiv.dto.EttersendingsoppgaveDo
import no.nav.bidrag.transport.dokument.JournalpostType
import no.nav.bidrag.transport.dokument.OpprettEttersendingsppgaveDto
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest

val OpprettJournalpostRequest.erNotat get() = journalposttype == JournalpostType.NOTAT
fun OpprettEttersendingsppgaveDto.toTilleggsopplysning() = EttersendingsoppgaveDo(
    tittel = tittel,
    skjemaId = skjemaId,
    språk = språk.name,
    innsendingsFristDager = innsendingsFristDager,
    vedleggsliste = vedleggsliste.map {
        EttersendingsoppgaveDo.EttersendingsoppgaveVedleggDo(
            vedleggsnr = it.vedleggsnr,
            tittel = it.tittel,
        )
    },
)
