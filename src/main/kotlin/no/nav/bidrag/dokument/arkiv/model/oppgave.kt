package no.nav.bidrag.dokument.arkiv.model

import no.nav.bidrag.dokument.arkiv.dto.OppgaveType

private const val PARAMETER_JOURNALPOST_ID = "journalpostId"
private const val PARAMETER_OPPGAVE_TYPE = "oppgavetype"
private const val PARAMETER_SAKSREFERANSE = "saksreferanse"
private const val PARAMETER_TEMA = "tema"
private const val PARAMETER_JOURNALPOSTID = "journalpostId"

internal data class OppgaveSokParametre(private val parametre: StringBuilder = StringBuilder()) {

    fun brukBehandlingSomOppgaveType(): OppgaveSokParametre {
        return leggTilParameter(PARAMETER_OPPGAVE_TYPE, OppgaveType.BEH_SAK)
    }

    fun brukVurderDokumentSomOppgaveType(): OppgaveSokParametre {
        return leggTilParameter(PARAMETER_OPPGAVE_TYPE, OppgaveType.VUR)
    }

    fun brukJournalforingSomOppgaveType(): OppgaveSokParametre {
        return leggTilParameter(PARAMETER_OPPGAVE_TYPE, OppgaveType.JFR)
    }


    fun leggTilJournalpostId(journalpostId: Long): OppgaveSokParametre {
        return leggTilParameter(PARAMETER_JOURNALPOSTID, journalpostId)
    }
    fun leggTilFagomrade(fagomrade: String): OppgaveSokParametre {
        return leggTilParameter(PARAMETER_TEMA, fagomrade)
    }

    fun leggTilSaksreferanse(saksnummer: String?) {
        leggTilParameter(PARAMETER_SAKSREFERANSE, saksnummer)
    }

    private fun leggTilParameter(navn: String?, verdi: Any?): OppgaveSokParametre {
        if (parametre.isEmpty()) {
            parametre.append('?')
        } else {
            parametre.append('&')
        }

        parametre.append(navn).append('=').append(verdi)

        return this
    }

    fun hentParametreForApneOppgaverSortertSynkendeEtterFrist(): String {
        return "/$parametre&statuskategori=AAPEN&sorteringsrekkefolge=ASC&sorteringsfelt=FRIST&limit=10"
    }
}