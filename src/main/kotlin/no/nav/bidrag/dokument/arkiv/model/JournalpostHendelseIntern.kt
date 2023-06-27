package no.nav.bidrag.dokument.arkiv.model

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottakerIdType
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerMedEnhet
import no.nav.bidrag.transport.dokument.HendelseType
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.bidrag.transport.dokument.JournalpostStatus
import no.nav.bidrag.transport.dokument.Sporingsdata
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

class JournalpostHendelseIntern(
    var journalpost: Journalpost,
    var saksbehandler: SaksbehandlerMedEnhet?,
    var journalforingHendelse: JournalfoeringHendelseRecord?
) {
    val journalpostHendelse: JournalpostHendelse

    init {
        journalpostHendelse = JournalpostHendelse(
            journalpostId = journalpost.hentJournalpostIdMedPrefix(),
            journalstatus = journalpost.hentJournalStatus(),
            status = journalpost.hentJournalStatus(),
            enhet = journalpost.journalforendeEnhet,
            fagomrade = journalforingHendelse?.temaNytt ?: journalpost.tema,
            tema = journalforingHendelse?.temaNytt ?: journalpost.tema,
            aktorId = hentAktoerIdFraJournalpost(),
            fnr = hentFnrFraJournalpost(),
            tittel = journalpost.hentTittel(),
            sporing = opprettSporingsData(),
            sakstilknytninger = journalpost.hentTilknyttetSaker().toList(),
            dokumentDato = journalpost.hentDatoDokument() ?: journalpost.hentDatoRegistrert(),
            journalfortDato = journalpost.hentDatoJournalfort(),
            hendelseType = if (journalforingHendelse?.hendelsesType == JoarkHendelseType.ENDELIG_JOURNALFORT.hendelsesType) HendelseType.JOURNALFORING else HendelseType.ENDRING,
            journalposttype = journalpost.journalposttype?.name,
            behandlingstema = journalpost.behandlingstema
        )
    }

    fun hentFnrFraJournalpost(): String? {
        val bruker = journalpost.bruker
        val avsenderMottaker = journalpost.avsenderMottaker
        return if (bruker != null && !bruker.isAktoerId()) {
            bruker.id
        } else if (avsenderMottaker != null && avsenderMottaker.type == AvsenderMottakerIdType.FNR) {
            avsenderMottaker.id
        } else {
            null
        }
    }

    fun hentAktoerIdFraJournalpost(): String? {
        val bruker = journalpost.bruker
        return if (bruker?.isAktoerId() == true) bruker.id else null
    }

    private fun opprettSporingsData(): Sporingsdata = Sporingsdata(
        CorrelationId.fetchCorrelationIdForThread(),
        saksbehandler?.saksbehandler?.ident,
        saksbehandler?.saksbehandler?.navn,
        saksbehandler?.enhetsnummer
    )

    fun hentJournalpostHendelse() = journalpostHendelse
}

class JournalforingHendelseIntern(var journalforingHendelse: JournalfoeringHendelseRecord) {
    var saksbehandler = Saksbehandler(null, "bidrag-dokument-arkiv").tilEnhet("9999")

    fun toJournalpostHendelse(journalpost: Journalpost?): JournalpostHendelse {
        if (journalpost != null) {
            return JournalpostHendelseIntern(
                journalpost,
                hentSaksbehandler(journalpost),
                journalforingHendelse
            ).hentJournalpostHendelse()
        }

        return journalforingHendelseToJournalpostHendelse()
    }

    fun hentSaksbehandler(journalpost: Journalpost): SaksbehandlerMedEnhet {
        if (journalpost.isStatusJournalfort()) {
            return Saksbehandler(
                journalpost.hentJournalfortAvIdent(),
                journalpost.journalfortAvNavn
            ).tilEnhet(journalpost.journalforendeEnhet)
        }

        return Saksbehandler(null, "bidrag-dokument-arkiv").tilEnhet("9999")
    }

    fun journalforingHendelseToJournalpostHendelse(): JournalpostHendelse {
        return JournalpostHendelse(
            sporing = opprettSporingsData(),
            journalpostId = "JOARK-${journalforingHendelse.journalpostId}",
            status = when (journalforingHendelse.journalpostStatus) {
                "MOTTATT" -> JournalpostStatus.MOTTATT
                "JOURNALFOERT" -> JournalpostStatus.JOURNALFØRT
                "UTGAAR" -> JournalpostStatus.UTGÅR
                else -> null
            },
            enhet = null,
            fagomrade = journalforingHendelse.temaNytt ?: journalforingHendelse.temaGammelt
        )
    }

    private fun opprettSporingsData(): Sporingsdata = Sporingsdata(
        CorrelationId.fetchCorrelationIdForThread(),
        saksbehandler.saksbehandler.ident,
        saksbehandler.saksbehandler.navn,
        saksbehandler.enhetsnummer
    )
}
