package no.nav.bidrag.dokument.arkiv.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.ZonedDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppgaveHendelse(
    val id: Long,
    val endretAvEnhetsnr: String? = null,
    val tildeltEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val journalpostId: String? = null,
    val tilordnetRessurs: String? = null,
    val saksreferanse: String? = null,
    val beskrivelse: String? = null,
    val temagruppe: String? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val versjon: Int? = null,
    val status: OppgaveStatus? = null,
    val statuskategori: Oppgavestatuskategori? = null,
    val endretAv: String? = null,
    val opprettetAv: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val ident: Ident? = null,
    val metadata: Map<String, String>? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val aktivDato: LocalDate? = null,
    val opprettetTidspunkt: ZonedDateTime? = null,
    val ferdigstiltTidspunkt: ZonedDateTime? = null,
    val endretTidspunkt: ZonedDateTime? = null
){

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Ident(
        var identType: OppgaveIdentType? = null,
        val verdi: String? = null,
        val folkeregisterident: String? = null
    )

    fun erTemaBIDEllerFAR(): Boolean = tema == "BID" || tema == "FAR"
    fun erReturOppgave(): Boolean = oppgavetype == "RETUR";
    fun erJournalforingOppgave(): Boolean = oppgavetype == "JFR";
    fun erStatusOpprettet(): Boolean = status == OppgaveStatus.OPPRETTET

    fun erJoarkJournalpost(): Boolean = journalpostId != null && !journalpostId.contains("BID")

}

enum class Prioritet {
    HOY //, NORM, LAV
}

enum class OppgaveIdentType {
    AKTOERID,
    ORGNR,
    SAMHANDLERNR,
    BNR
}
enum class OppgaveStatus {
    FERDIGSTILT,
    AAPNET,
    OPPRETTET,
    FEILREGISTRERT,
    UNDER_BEHANDLING
}

enum class Oppgavestatuskategori {
    AAPEN, AVSLUTTET
}