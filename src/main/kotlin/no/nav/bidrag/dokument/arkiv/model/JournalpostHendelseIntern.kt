package no.nav.bidrag.dokument.arkiv.model

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.dokument.arkiv.dto.BrukerType
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerMedEnhet
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import no.nav.bidrag.dokument.dto.Sporingsdata

class JournalpostHendelseIntern(var journalpost: Journalpost, var saksbehandler: SaksbehandlerMedEnhet?) {
    var journalpostHendelse: JournalpostHendelse = JournalpostHendelse()
    init {
        journalpostHendelse.journalpostId = journalpost.hentJournalpostIdMedPrefix()
        journalpostHendelse.journalstatus = journalpost.hentJournalStatus()
        journalpostHendelse.enhet = journalpost.journalforendeEnhet
        journalpostHendelse.fagomrade = journalpost.tema
        val bruker = journalpost.bruker
        if (bruker?.id != null && bruker.type == BrukerType.AKTOERID.name) {
          journalpostHendelse.aktorId = bruker.id
        }
        journalpostHendelse.sporing = opprettSporingsData()
    }
    private fun opprettSporingsData(): Sporingsdata = Sporingsdata(CorrelationId.fetchCorrelationIdForThread(), saksbehandler?.saksbehandler?.ident, saksbehandler?.saksbehandler?.navn, saksbehandler?.enhetsnummer)
    fun hentJournalpostHendelse()=journalpostHendelse
}