package no.nav.bidrag.dokument.arkiv.dto

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val NORSK_DATO_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val NORSK_TIDSSTEMPEL_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

data class OppgaveResponse(var id: Long?)
data class OppgaveSokResponse(var antallTreffTotalt: Int = 0, var oppgaver: List<OppgaveData> = ArrayList())

data class OpprettOppgaveResponse(
    var id: Long? = null,
    var tildeltEnhetsnr: String? = null,
    var tema: String? = null,
    var oppgavetype: String? = null
)

sealed class OppgaveData(
    var id: Long,
    var versjon: Int,
    var tildeltEnhetsnr: String? = null,
    var endretAvEnhetsnr: String? = null,
    var opprettetAvEnhetsnr: String? = null,
    var journalpostId: String? = null,
    var journalpostkilde: String? = null,
    var behandlesAvApplikasjon: String? = null,
    var saksreferanse: String? = null,
    var bnr: String? = null,
    var samhandlernr: String? = null,
    var aktoerId: String? = null,
    var orgnr: String? = null,
    var tilordnetRessurs: String? = null,
    var beskrivelse: String? = null,
    var temagruppe: String? = null,
    open var tema: String? = null,
    var behandlingstema: String? = null,
    var oppgavetype: String? = null,
    var behandlingstype: String? = null,
    var mappeId: String? = null,
    var fristFerdigstillelse: String? = null,
    var aktivDato: String? = null,
    var opprettetTidspunkt: String? = null,
    var opprettetAv: String? = null,
    var endretAv: String? = null,
    var ferdigstiltTidspunkt: String? = null,
    var endretTidspunkt: String? = null,
    var prioritet: String? = null,
    var status: String? = null,
    var metadata: Map<String, String>? = null
) {

    fun endreForNyttDokument(
        saksbehandlersInfo: String,
        dokumentbeskrivelse: String,
        dokumentdato: LocalDate
    ) {
        beskrivelse = "--- ${LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)} $saksbehandlersInfo ---\r\n" +
                "${lagDokumentOppgaveTittelForEndring("Nytt dokument", dokumentbeskrivelse, dokumentdato)}\r\n\r\n" +
                "$beskrivelse"
    }
}

data class EndreForNyttDokumentRequest(private var oppgaveData: OppgaveData,
                                       private var saksbehandlersInfo: String,
                                       private var journalpost: Journalpost):
    OppgaveData(
        id = oppgaveData.id,
        tema = journalpost.tema,
        versjon = oppgaveData.versjon,
        beskrivelse = "--- ${LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)} $saksbehandlersInfo ---\r\n" +
                "${lagDokumentOppgaveTittelForEndring("Nytt dokument", journalpost.tittel!!, journalpost.hentDatoRegistrert()!!)}\r\n\r\n" +
                "$oppgaveData.beskrivelse"
)

@Suppress("unused") // used by jackson...
sealed class OpprettOppgaveRequest(
    var journalpostId: String,
    var oppgavetype: OppgaveType,
    var opprettetAvEnhetsnr: String = "9999",
    var prioritet: String = Prioritet.HOY.name,
    var tildeltEnhetsnr: String? = "4833",
    open var tema: String = "BID",
    var aktivDato: String = LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd")),
    var saksreferanse: String? = null,
    var beskrivelse: String? = null,
    var tilordnetRessurs: String? = null,
    var fristFerdigstillelse: String? = null
) {

    fun somHttpEntity(): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return HttpEntity(this, headers)
    }
}

@Suppress("unused") // påkrevd felt som brukes av jackson men som ikke brukes aktivt
data class OpprettVurderKonsekvensYtelseOppgaveRequest(
    private var journalpost: Journalpost,
    override var tema: String,
    var aktoerId: String,
    var saksbehandlerMedEnhet: SaksbehandlerMedEnhet, private var kommentar: String):
    OpprettOppgaveRequest(
        journalpostId = journalpost.journalpostId!!,
        oppgavetype = OppgaveType.VUR_KONS_YTE,
        tildeltEnhetsnr = journalpost.journalforendeEnhet,
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer!!,
        beskrivelse = lagVurderKonskevensYtelseOppgaveTittel(journalpost.tittel!!, kommentar)
    )


@Suppress("unused") // påkrevd felt som brukes av jackson men som ikke brukes aktivt
data class OpprettBehandleDokumentOppgaveRequest(
    private var journalpost: Journalpost,
    var aktoerId: String,
    var saksbehandlerMedEnhet: SaksbehandlerMedEnhet):
    OpprettOppgaveRequest(
        journalpostId = journalpost.journalpostId!!,
        beskrivelse = lagDokumentOppgaveTittel("Behandle dokument", journalpost.tittel!!, journalpost.hentDatoRegistrert() ?: LocalDate.now()),
        fristFerdigstillelse = LocalDate.now().plusDays(1).toString(),
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer!!,
        oppgavetype = OppgaveType.BEH_SAK,
        saksreferanse = journalpost.sak?.fagsakId,
        tildeltEnhetsnr = saksbehandlerMedEnhet.enhetsnummer,
        tilordnetRessurs = saksbehandlerMedEnhet.saksbehandler.ident,
    )

internal fun lagDokumentOppgaveTittelForEndring(oppgaveNavn: String, dokumentbeskrivelse: String, dokumentdato: LocalDate) =
    "\u00B7 ${lagDokumentOppgaveTittel(oppgaveNavn, dokumentbeskrivelse, dokumentdato)}"

internal fun lagDokumentOppgaveTittel(oppgaveNavn: String, dokumentbeskrivelse: String, dokumentdato: LocalDate) =
    "$oppgaveNavn ($dokumentbeskrivelse) mottatt ${dokumentdato.format(NORSK_DATO_FORMAT)}"

internal fun lagVurderKonskevensYtelseOppgaveTittel(dokumentbeskrivelse: String, kommentar: String): String {
    return "--- ${LocalDate.now().format(NORSK_DATO_FORMAT)} - Opprettet av Bidrag ---\n Journalpost ($dokumentbeskrivelse) - $kommentar"
}


enum class OppgaveType {
    BEH_SAK,
    VUR_KONS_YTE
}

enum class Prioritet {
    HOY //, NORM, LAV
}