package no.nav.bidrag.dokument.arkiv.model

import no.nav.bidrag.dokument.arkiv.dto.OppgaveType

private const val PARAMETER_JOURNALPOST_ID = "journalpostId"
private const val PARAMETER_OPPGAVE_TYPE = "oppgavetype"
private const val PARAMETER_SAKSREFERANSE = "saksreferanse"
private const val PARAMETER_TEMA = "tema"
private const val PARAMETER_JOURNALPOSTID = "journalpostId"

data class OppgaveSokParametre(private val parametre: StringBuilder = StringBuilder()) {

    fun brukVurderDokumentSomOppgaveType(): OppgaveSokParametre = leggTilParameter(PARAMETER_OPPGAVE_TYPE, OppgaveType.VUR)

    fun brukJournalforingSomOppgaveType(): OppgaveSokParametre = leggTilParameter(PARAMETER_OPPGAVE_TYPE, OppgaveType.JFR)

    fun leggTilJournalpostId(journalpostId: Long): OppgaveSokParametre = leggTilParameter(PARAMETER_JOURNALPOSTID, journalpostId)
    fun leggTilFagomrade(fagomrade: String): OppgaveSokParametre = leggTilParameter(PARAMETER_TEMA, fagomrade)

    private fun leggTilParameter(navn: String?, verdi: Any?): OppgaveSokParametre {
        if (parametre.isEmpty()) {
            parametre.append('?')
        } else {
            parametre.append('&')
        }

        parametre.append(navn).append('=').append(verdi)

        return this
    }

    fun hentParametreForApneOppgaverSortertSynkendeEtterFrist(): String = "/$parametre&statuskategori=AAPEN&sorteringsrekkefolge=ASC&sorteringsfelt=FRIST&limit=10"
}
