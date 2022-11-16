package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.arkiv.model.OppgaveHendelse
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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
open class OppgaveData(
    var id: Long? = null,
    var versjon: Int = -1,
    var tildeltEnhetsnr: String? = null,
    open var endretAvEnhetsnr: String? = null,
    var opprettetAvEnhetsnr: String? = null,
    var journalpostId: String? = null,
    var journalpostkilde: String? = null,
    var behandlesAvApplikasjon: String? = null,
    open var saksreferanse: String? = null,
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
)

data class OppdaterSakRequest(private var oppgaveHendelse: OppgaveHendelse, override var saksreferanse: String?): OppgaveData(id = oppgaveHendelse.id, versjon = oppgaveHendelse.versjon)

data class OverforOppgaveTilFagpost(private var oppgaveData: OppgaveData, private var _endretAvEnhetsnr: String, private val saksbehandlersInfo: String, private val kommentar: String):
    OppgaveData(
        id = oppgaveData.id,
        versjon = oppgaveData.versjon,
        endretAvEnhetsnr = _endretAvEnhetsnr,
        tildeltEnhetsnr = OppgaveEnhet.FAGPOST,
        beskrivelse = beskrivelseHeader(saksbehandlersInfo) +
                "\u00B7 $kommentar\r\n" +
                "\u00B7 Oppgave overført fra enhet ${oppgaveData.tildeltEnhetsnr} til ${OppgaveEnhet.FAGPOST}\r\n\r\n" +
                "${oppgaveData.beskrivelse}"
    )

data class FerdigstillOppgaveRequest(private var oppgaveData: OppgaveData, private var _endretAvEnhetsnr: String):
    OppgaveData(
        id = oppgaveData.id,
        versjon = oppgaveData.versjon,
        status = "FERDIGSTILT",
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

):
    OpprettOppgaveFagpostRequest(
        journalpostId = journalpost.journalpostId!!,
        oppgavetype = OppgaveType.BEST_RESCAN,
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer,
        saksreferanse = journalpost.hentSaksnummer(),
        gjelderId = journalpost.hentGjelderId()
    ){
    init {
        beskrivelse =  """
            ${beskrivelseHeader(saksbehandlerMedEnhet.hentSaksbehandlerInfo())} 
            ${bestillSplittingKommentar(beskrivSplitting)}
            """.trimIndent()
    }
}

@Suppress("unused") // påkrevd felt som brukes av jackson men som ikke brukes aktivt
data class BestillReskanningOppgaveRequest(
    private val journalpost: Journalpost,
    private var saksbehandlerMedEnhet: SaksbehandlerMedEnhet,
    private var kommentar: String?,
    override var fristFerdigstillelse: String? = LocalDate.now().plusDays(5).toString(),


    ):
    OpprettOppgaveFagpostRequest(
        journalpostId = journalpost.journalpostId!!,
        oppgavetype = OppgaveType.BEST_RESCAN,
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer,
        saksreferanse = journalpost.hentSaksnummer(),
        gjelderId = journalpost.hentGjelderId()
    ){
    init {
        beskrivelse =  """
            ${beskrivelseHeader(saksbehandlerMedEnhet.hentSaksbehandlerInfo())} 
            ${bestillReskanningKommentar(kommentar)}
            """.trimIndent()
    }
}

@Suppress("unused") // påkrevd felt som brukes av jackson men som ikke brukes aktivt
data class BestillOriginalOppgaveRequest(
    private val journalpost: Journalpost,
    private val enhet: String,
    private var saksbehandlerMedEnhet: SaksbehandlerMedEnhet,
    private var kommentar: String?

):
    OpprettOppgaveFagpostRequest(
        journalpostId = journalpost.journalpostId!!,
        oppgavetype = OppgaveType.BEST_ORGINAL,
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer,
        saksreferanse = journalpost.hentSaksnummer(),
        gjelderId = journalpost.hentGjelderId()
    ){
        init {
            beskrivelse =  """
            ${beskrivelseHeader(saksbehandlerMedEnhet.hentSaksbehandlerInfo())} 
            Originalbestilling: Vi ber om å få tilsendt papiroriginalen av vedlagte dokumenter. 
                
            Dokumentet skal sendes til ${enhet}, og merkes med ${saksbehandlerMedEnhet.saksbehandler.hentIdentMedNavn()}
            """.trimIndent()
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

    ):
    OpprettOppgaveRequest(
        journalpostId = journalpostId,
        oppgavetype = oppgavetype,
        prioritet = Prioritet.NORM.name,
        fristFerdigstillelse = LocalDate.now().plusDays(1).toString(),
        tildeltEnhetsnr = OppgaveEnhet.FAGPOST,
        tema = "GEN"
    ){

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
):
    OpprettOppgaveRequest(
        journalpostId = journalpostId,
        oppgavetype = OppgaveType.VUR,
        prioritet = Prioritet.NORM.name,
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer,
        fristFerdigstillelse = LocalDate.now().plusDays(1).toString(),
        beskrivelse = lagVurderDokumentOppgaveBeskrivelse(saksbehandlerMedEnhet, journalpost.dokumenter[0].brevkode, journalpost.tittel!!, kommentar, journalpost.hentDatoRegistrert() ?: LocalDate.now())
    )


@Suppress("unused") // påkrevd felt som brukes av jackson men som ikke brukes aktivt
data class OpprettBehandleDokumentOppgaveRequest(
    private var journalpost: Journalpost,
    var aktoerId: String,
    private val saksnummer: String,
    private var saksbehandlerMedEnhet: SaksbehandlerMedEnhet):
    OpprettOppgaveRequest(
        journalpostId = journalpost.journalpostId!!,
        beskrivelse = lagDokumentOppgaveTittel("Behandle dokument", journalpost.tittel!!, journalpost.hentDatoRegistrert() ?: LocalDate.now()),
        fristFerdigstillelse = LocalDate.now().plusDays(1).toString(),
        opprettetAvEnhetsnr = saksbehandlerMedEnhet.enhetsnummer!!,
        oppgavetype = OppgaveType.BEH_SAK,
        saksreferanse = saksnummer,
        tildeltEnhetsnr = saksbehandlerMedEnhet.enhetsnummer,
        tilordnetRessurs = saksbehandlerMedEnhet.saksbehandler.ident,
    )

internal fun lagDokumentOppgaveTittelForEndring(oppgaveNavn: String, dokumentbeskrivelse: String, dokumentdato: LocalDate) =
    "\u00B7 ${lagDokumentOppgaveTittel(oppgaveNavn, dokumentbeskrivelse, dokumentdato)}"

internal fun lagDokumentOppgaveTittel(oppgaveNavn: String, dokumentbeskrivelse: String, dokumentdato: LocalDate) =
    "$oppgaveNavn ($dokumentbeskrivelse) mottatt ${dokumentdato.format(NORSK_DATO_FORMAT)}"

internal fun lagVurderDokumentOppgaveBeskrivelse(saksbehandlerMedEnhet: SaksbehandlerMedEnhet, brevKode: String?, dokumentTittel: String?, kommentar: String?, regDato: LocalDate): String {
    var description = "--- ${LocalDate.now().format(NORSK_DATO_FORMAT)} ${saksbehandlerMedEnhet.hentSaksbehandlerInfo()} ---\n $brevKode $dokumentTittel"
    if (kommentar != null){
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

fun beskrivelseHeader(saksbehandlerInfo: String) = "--- ${LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)} $saksbehandlerInfo ---\r\n"
fun bestillReskanningKommentar(beskrivReskanning: String?) = """
        Bestill reskanning: Vi ber om reskanning av dokument.
            
        Beskrivelse fra saksbehandler: 
        ${beskrivReskanning ?: "Ingen"}
        """.trimIndent()

fun bestillSplittingKommentar(beskrivSplitting: String?) = """
        Bestill splitting av dokument:
        
        Saksbehandler ønsker splitting av dokument:
        "$beskrivSplitting"
        """.trimIndent()