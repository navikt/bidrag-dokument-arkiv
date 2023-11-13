package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.arkiv.model.OppgaveStatus
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val NORSK_DATO_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val NORSK_TIDSSTEMPEL_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

data class OppgaveResponse(var id: Long?)
data class OppgaveSokResponse(
    var antallTreffTotalt: Int = 0,
    var oppgaver: List<OppgaveData> = ArrayList()
)

data class OpprettOppgaveResponse(
    var id: Long? = null,
    var tildeltEnhetsnr: String? = null,
    var tema: String? = null,
    var oppgavetype: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OppgaveData(
    val id: Long,
    val versjon: Int = -1,
    val tildeltEnhetsnr: String? = null,
    val endretAvEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val journalpostId: String? = null,
    val journalpostkilde: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val saksreferanse: String? = null,
    val bnr: String? = null,
    val samhandlernr: String? = null,
    val aktoerId: String? = null,
    val orgnr: String? = null,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String? = null,
    val temagruppe: String? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val mappeId: String? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val aktivDato: String? = null,
    val opprettetTidspunkt: String? = null,
    val opprettetAv: String? = null,
    val endretAv: String? = null,
    val ferdigstiltTidspunkt: String? = null,
    val endretTidspunkt: String? = null,
    val prioritet: String? = null,
    val status: OppgaveStatus? = null,
    val metadata: Map<String, String>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
open class OppgaveRequest(
    val id: Long,
    var versjon: Int = -1,
    val endretAvEnhetsnr: String? = null,
    val journalpostId: String? = null,
    open val saksreferanse: String? = null,
    val beskrivelse: String? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val status: OppgaveStatus? = null
)

data class OppdaterSakRequest(
    private var oppgaveHendelse: OppgaveData,
    override var saksreferanse: String?,
    private val kommentar: String? = null
) : OppgaveRequest(
    id = oppgaveHendelse.id,
    versjon = oppgaveHendelse.versjon,
    beskrivelse = if (kommentar != null) kommentar else null
)

data class LeggTilKommentarPaaOppgave(
    private var oppgaveData: OppgaveData,
    private var _endretAvEnhetsnr: String,
    private val saksbehandlersInfo: String,
    private val kommentar: String
) :
    OppgaveRequest(
        id = oppgaveData.id,
        versjon = oppgaveData.versjon,
        endretAvEnhetsnr = _endretAvEnhetsnr,
        beskrivelse = beskrivelseHeader(saksbehandlersInfo) +
                "$kommentar\r\n\r\n" +
                "${oppgaveData.beskrivelse}"
    )

data class FerdigstillOppgaveRequest(
    private var oppgaveData: OppgaveData,
    private var _endretAvEnhetsnr: String
) :
    OppgaveRequest(
        id = oppgaveData.id,
        versjon = oppgaveData.versjon,
        status = OppgaveStatus.FERDIGSTILT,
        endretAvEnhetsnr = _endretAvEnhetsnr
    )

@Suppress("unused") // used by jackson...
sealed class OpprettOppgaveRequest(
    open val journalpostId: String,
    open val oppgavetype: OppgaveType,
    open val opprettetAvEnhetsnr: String = "9999",
    open val prioritet: String = Prioritet.HOY.name,
    open val tildeltEnhetsnr: String? = "4833",
    open val tema: String = "BID",
    var aktivDato: String = LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd")),
    open val saksreferanse: String? = null,
    var beskrivelse: String? = null,
    var tilordnetRessurs: String? = null,
    open var fristFerdigstillelse: String? = null
) {

    fun somHttpEntity(): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return HttpEntity(this, headers)
    }

    fun asJson(): String = ObjectMapper().findAndRegisterModules().writeValueAsString(this)
}

@Suppress("unused") // påkrevd felt som brukes av jackson men som ikke brukes aktivt
data class BestillSplittingoppgaveRequest(
    private val journalpost: Journalpost,
    private var saksbehandlerMedEnhet: SaksbehandlerMedEnhet,
    private var beskrivSplitting: String?

) :
    OpprettOppgaveFagpostRequest(
        journalpostId = journalpost.journalpostId!!,
        oppgavetype = OppgaveType.BEST_RESCAN,
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer,
        saksreferanse = journalpost.hentSaksnummer(),
        gjelderId = journalpost.hentGjelderId()
    ) {
    init {
        beskrivelse = "${beskrivelseHeader(saksbehandlerMedEnhet.hentSaksbehandlerInfo())}\n${
            bestillSplittingKommentar(beskrivSplitting)
        }"
    }
}

@Suppress("unused") // påkrevd felt som brukes av jackson men som ikke brukes aktivt
data class BestillReskanningOppgaveRequest(
    private val journalpost: Journalpost,
    private var saksbehandlerMedEnhet: SaksbehandlerMedEnhet,
    private var kommentar: String?,
    override var fristFerdigstillelse: String? = LocalDate.now().plusDays(5).toString()

) :
    OpprettOppgaveFagpostRequest(
        journalpostId = journalpost.journalpostId!!,
        oppgavetype = OppgaveType.BEST_RESCAN,
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer,
        saksreferanse = journalpost.hentSaksnummer(),
        gjelderId = journalpost.hentGjelderId()
    ) {
    init {
        beskrivelse = "${beskrivelseHeader(saksbehandlerMedEnhet.hentSaksbehandlerInfo())}\n${
            bestillReskanningKommentar(kommentar)
        }"
    }
}

@Suppress("unused") // påkrevd felt som brukes av jackson men som ikke brukes aktivt
data class BestillOriginalOppgaveRequest(
    private val journalpost: Journalpost,
    private val enhet: String,
    private var saksbehandlerMedEnhet: SaksbehandlerMedEnhet,
    private var kommentar: String?

) :
    OpprettOppgaveFagpostRequest(
        journalpostId = journalpost.journalpostId!!,
        oppgavetype = OppgaveType.BEST_ORGINAL,
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer,
        saksreferanse = journalpost.hentSaksnummer(),
        gjelderId = journalpost.hentGjelderId()
    ) {
    init {
        beskrivelse = """
            ${beskrivelseHeader(saksbehandlerMedEnhet.hentSaksbehandlerInfo())} 
            Originalbestilling: Vi ber om å få tilsendt papiroriginalen av vedlagte dokumenter. 
                
            Dokumentet skal sendes til $enhet, og merkes med ${saksbehandlerMedEnhet.saksbehandler.hentIdentMedNavn()}
        """.trimIndent().trim()
    }
}

@Suppress("unused") // påkrevd felt som brukes av jackson men som ikke brukes aktivt
sealed class OpprettOppgaveFagpostRequest(
    override var journalpostId: String,
    override var oppgavetype: OppgaveType,
    override var saksreferanse: String?,
    override var opprettetAvEnhetsnr: String,
    var aktoerId: String? = null,
    private val gjelderId: String?

) :
    OpprettOppgaveRequest(
        journalpostId = journalpostId,
        oppgavetype = oppgavetype,
        prioritet = Prioritet.NORM.name,
        fristFerdigstillelse = LocalDate.now().plusDays(5).toString(),
        tildeltEnhetsnr = OppgaveEnhet.FAGPOST,
        tema = "GEN"
    ) {

    fun hentGjelderId() = gjelderId
    fun hasGjelderId() = !gjelderId.isNullOrEmpty()
}

@Suppress("unused") // påkrevd felt som brukes av jackson men som ikke brukes aktivt
data class OpprettVurderDokumentOppgaveRequest(
    private var journalpost: Journalpost,
    override var journalpostId: String,
    override var tildeltEnhetsnr: String?,
    override var tema: String,
    var aktoerId: String,
    private var saksbehandlerMedEnhet: SaksbehandlerMedEnhet,
    private var kommentar: String?
) :
    OpprettOppgaveRequest(
        journalpostId = journalpostId,
        oppgavetype = OppgaveType.VUR,
        prioritet = Prioritet.NORM.name,
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer,
        fristFerdigstillelse = LocalDate.now().plusDays(1).toString(),
        beskrivelse = lagVurderDokumentOppgaveBeskrivelse(
            saksbehandlerMedEnhet,
            journalpost.dokumenter[0].brevkode,
            journalpost.tittel!!,
            kommentar,
            journalpost.hentDatoRegistrert() ?: LocalDate.now()
        )
    )

@Suppress("unused") // påkrevd felt som brukes av jackson men som ikke brukes aktivt
data class OpprettBehandleDokumentOppgaveRequest(
    private var journalpost: Journalpost,
    var aktoerId: String,
    private val saksnummer: String,
    private var saksbehandlerMedEnhet: SaksbehandlerMedEnhet
) :
    OpprettOppgaveRequest(
        journalpostId = journalpost.journalpostId!!,
        beskrivelse = lagDokumentOppgaveTittel(
            "Behandle dokument",
            journalpost.tittel!!,
            journalpost.hentDatoRegistrert() ?: LocalDate.now()
        ),
        fristFerdigstillelse = LocalDate.now().plusDays(1).toString(),
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer!!,
        oppgavetype = OppgaveType.BEH_SAK,
        saksreferanse = saksnummer,
        tildeltEnhetsnr = saksbehandlerMedEnhet.enhetsnummer,
        tilordnetRessurs = saksbehandlerMedEnhet.saksbehandler.ident
    )

internal fun lagDokumentOppgaveTittelForEndring(
    oppgaveNavn: String,
    dokumentbeskrivelse: String,
    dokumentdato: LocalDate
) =
    "\u00B7 ${lagDokumentOppgaveTittel(oppgaveNavn, dokumentbeskrivelse, dokumentdato)}"

internal fun lagDokumentOppgaveTittel(
    oppgaveNavn: String,
    dokumentbeskrivelse: String,
    dokumentdato: LocalDate
) =
    "$oppgaveNavn ($dokumentbeskrivelse) mottatt ${dokumentdato.format(NORSK_DATO_FORMAT)}"

internal fun lagVurderDokumentOppgaveBeskrivelse(
    saksbehandlerMedEnhet: SaksbehandlerMedEnhet,
    brevKode: String?,
    dokumentTittel: String?,
    kommentar: String?,
    regDato: LocalDate
): String {
    var description = "--- ${
        LocalDate.now().format(NORSK_DATO_FORMAT)
    } ${saksbehandlerMedEnhet.hentSaksbehandlerInfo()} ---\n $brevKode $dokumentTittel"
    if (kommentar != null) {
        description += "\n\n $kommentar"
    }
    description += "\n\n Reg.dato: ${regDato.format(NORSK_DATO_FORMAT)}"
    return description
}

internal fun lagDokumenterVedlagtBeskrivelse(journalpost: Journalpost) =
    "\u00B7 Dokumenter vedlagt: ${journalpost.dokumenter.joinToString { "JOARK-${journalpost.journalpostId}:${it.dokumentInfoId}" }}"

enum class OppgaveType {
    BEH_SAK,
    VUR,
    JFR,
    BEST_RESCAN,
    BEST_ORGINAL
}

object OppgaveEnhet {
    val FAGPOST = "2950"
}

enum class Prioritet {
    HOY, NORM, LAV
}

fun beskrivelseHeader(saksbehandlerInfo: String) =
    "--- ${LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)} $saksbehandlerInfo ---\r\n"

fun bestillReskanningKommentar(beskrivReskanning: String?) = """
        Bestill reskanning: 
        Vi ber om reskanning av dokument.
        Beskrivelse fra saksbehandler: 
        ${beskrivReskanning ?: "Ingen"}
""".trimIndent().trim()

fun bestillSplittingKommentar(beskrivSplitting: String?) = """
        Bestill splitting av dokument: 
        Saksbehandler ønsker splitting av dokument:
        $beskrivSplitting
""".trimIndent().trim()
