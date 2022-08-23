package no.nav.bidrag.dokument.arkiv.model

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottakerIdType
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.JournalstatusDto
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerMedEnhet
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import no.nav.bidrag.dokument.dto.Sporingsdata
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

class JournalpostHendelseIntern(var journalpost: Journalpost, var saksbehandler: SaksbehandlerMedEnhet?, var journalforingHendelse: JournalfoeringHendelseRecord?) {
    var journalpostHendelse: JournalpostHendelse = JournalpostHendelse()
    init {
        journalpostHendelse.journalpostId = journalpost.hentJournalpostIdMedPrefix()
        journalpostHendelse.journalstatus = journalpost.hentJournalStatus()
        journalpostHendelse.enhet = journalpost.journalforendeEnhet
        journalpostHendelse.fagomrade = journalforingHendelse?.temaNytt ?: journalpost.tema
        journalpostHendelse.aktorId = hentAktoerIdFraJournalpost()
        journalpostHendelse.fnr = hentFnrFraJournalpost()
        journalpostHendelse.sporing = opprettSporingsData()
    }

    fun hentFnrFraJournalpost(): String? {
        val bruker = journalpost.bruker
        val avsenderMottaker = journalpost.avsenderMottaker
        return if (bruker != null && !bruker.isAktoerId()) bruker.id
        else if (avsenderMottaker != null && avsenderMottaker.type == AvsenderMottakerIdType.FNR) avsenderMottaker.id
        else null
    }
    fun hentAktoerIdFraJournalpost(): String? {
        val bruker = journalpost.bruker
        return if (bruker?.isAktoerId() == true) bruker.id else null
    }
    private fun opprettSporingsData(): Sporingsdata = Sporingsdata(CorrelationId.fetchCorrelationIdForThread(), saksbehandler?.saksbehandler?.ident, saksbehandler?.saksbehandler?.navn, saksbehandler?.enhetsnummer)
    fun hentJournalpostHendelse()=journalpostHendelse
}
class JournalforingHendelseIntern(var journalforingHendelse: JournalfoeringHendelseRecord) {
    var saksbehandler = Saksbehandler("bidrag-dokument-arkiv", "bidrag-dokument-arkiv").tilEnhet("9999")

    fun toJournalpostHendelse(journalpost: Journalpost?): JournalpostHendelse {
        if (journalpost != null){
            val hendelse = JournalpostHendelseIntern(journalpost, saksbehandler, journalforingHendelse).hentJournalpostHendelse()
            hendelse.enhet = null
            return hendelse
        }

        return journalforingHendelseToJournalpostHendelse()
    }

    fun journalforingHendelseToJournalpostHendelse(): JournalpostHendelse {
        val journalpostHendelse = JournalpostHendelse()
        journalpostHendelse.sporing = opprettSporingsData()
        journalpostHendelse.journalpostId = "JOARK-${journalforingHendelse.journalpostId}"
        journalpostHendelse.journalstatus = when(journalforingHendelse.journalpostStatus){
            "MOTTATT"-> JournalstatusDto.MOTTAKSREGISTRERT
            "JOURNALFOERT"-> JournalstatusDto.JOURNALFORT
            "UTGAAR"-> JournalstatusDto.UTGAR
            else -> null
        }
        journalpostHendelse.enhet = null
        journalpostHendelse.fagomrade = journalforingHendelse.temaNytt ?: journalforingHendelse.temaGammelt
        return journalpostHendelse
    }

    private fun opprettSporingsData(): Sporingsdata = Sporingsdata(CorrelationId.fetchCorrelationIdForThread(), saksbehandler.saksbehandler.ident, saksbehandler.saksbehandler.navn, saksbehandler.enhetsnummer)
}