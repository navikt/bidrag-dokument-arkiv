package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.bidrag.dokument.arkiv.model.JournalpostDataException
import no.nav.bidrag.dokument.arkiv.model.ViolationException
import no.nav.bidrag.dokument.arkiv.utils.DateUtils
import no.nav.bidrag.dokument.arkiv.utils.JsonMapper.fromJsonString
import no.nav.bidrag.dokument.arkiv.utils.JsonMapper.toJsonString
import no.nav.bidrag.dokument.dto.AktorDto
import no.nav.bidrag.dokument.dto.AvsenderMottakerDto
import no.nav.bidrag.dokument.dto.AvsenderMottakerDtoIdType
import no.nav.bidrag.dokument.dto.AvvikType
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse
import no.nav.bidrag.dokument.dto.DokumentDto
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand
import no.nav.bidrag.dokument.dto.JournalpostDto
import no.nav.bidrag.dokument.dto.JournalpostResponse
import no.nav.bidrag.dokument.dto.Kanal
import no.nav.bidrag.dokument.dto.KodeDto
import no.nav.bidrag.dokument.dto.ReturDetaljer
import no.nav.bidrag.dokument.dto.ReturDetaljerLog
import org.apache.logging.log4j.util.Strings
import java.time.LocalDate
import java.util.stream.Collectors.toList


// Max key length is 20
const val RETUR_DETALJER_KEY = "retur"
const val DISTRIBUERT_ADRESSE_KEY = "distAdresse"
const val DISTRIBUSJON_BESTILT_KEY = "distribusjonBestilt"
const val AVVIK_ENDRET_TEMA_KEY = "avvikEndretTema"
const val AVVIK_NY_DISTRIBUSJON_BESTILT_KEY = "avvikNyDistribusjon"
const val JOURNALFORT_AV_KEY = "journalfortAv"
private const val DATO_DOKUMENT = "DATO_DOKUMENT"
private const val DATO_EKSPEDERT = "DATO_EKSPEDERT"
private const val DATO_JOURNALFORT = "DATO_JOURNALFOERT"
private const val DATO_REGISTRERT = "DATO_REGISTRERT"
private const val DATO_RETUR = "DATO_AVS_RETUR"

object JournalstatusDto {
    const val EKSPEDERT = "E"
    const val AVBRUTT = "A"
    const val KLAR_TIL_PRINT = "KP"
    const val RETUR = "RE"
    const val JOURNALFORT = "J"
    const val FEILREGISTRERT = "F"
    const val MOTTAKSREGISTRERT = "M"
    const val RESERVERT = "R"
    const val UTGAR = "U"
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class Journalpost(
    var avsenderMottaker: AvsenderMottaker? = null,
    var bruker: Bruker? = null,
    var dokumenter: List<Dokument> = emptyList(),
    var journalforendeEnhet: String? = null,
    var journalfortAvNavn: String? = null,
    var journalpostId: String? = null,
    var journalposttype: JournalpostType? = null,
    var kanal: JournalpostKanal? = null,
    var journalstatus: JournalStatus? = null,
    var relevanteDatoer: List<DatoType> = emptyList(),
    var sak: Sak? = null,
    var tema: String? = null,
    var antallRetur: Int? = null,
    var tittel: String? = null,
    var behandlingstema: String? = null,
    var opprettetAvNavn: String? = null,
    var eksternReferanseId: String? = null,
    var tilknyttedeSaker: List<String> = emptyList(),
    var tilleggsopplysninger: TilleggsOpplysninger = TilleggsOpplysninger()
) {

    fun hentGjelderId(): String? = bruker?.id
    fun hentAvsenderMottakerId(): String? = avsenderMottaker?.id
    fun hentJournalStatus(): String? {
        if (isDistribusjonKommetIRetur()){
            return JournalstatusDto.RETUR
        }
        return when(journalstatus){
                JournalStatus.MOTTATT -> JournalstatusDto.MOTTAKSREGISTRERT
                JournalStatus.JOURNALFOERT -> JournalstatusDto.JOURNALFORT
                JournalStatus.FEILREGISTRERT -> JournalstatusDto.FEILREGISTRERT
                JournalStatus.EKSPEDERT -> JournalstatusDto.EKSPEDERT
                JournalStatus.FERDIGSTILT -> if (isUtgaaendeDokument()) if(isDistribusjonBestilt()) JournalstatusDto.EKSPEDERT else JournalstatusDto.KLAR_TIL_PRINT else JournalstatusDto.JOURNALFORT
                JournalStatus.RESERVERT -> JournalstatusDto.RESERVERT
                JournalStatus.UTGAAR -> JournalstatusDto.UTGAR
                JournalStatus.AVBRUTT -> JournalstatusDto.AVBRUTT
                else -> journalstatus?.name
            }
    }

    fun isDistribusjonKommetIRetur() = (isDistribusjonBestilt()) && antallRetur != null && antallRetur!! > 0

    fun hentBrevkode(): String? = hentHoveddokument()?.brevkode

    fun isDistribusjonBestilt(): Boolean = tilleggsopplysninger.isDistribusjonBestilt() || isStatusEkspedert()

    fun isFeilregistrert() = journalstatus == JournalStatus.FEILREGISTRERT

    fun hentKanal(): Kanal? {
        return when(kanal){
            JournalpostKanal.NAV_NO ->Kanal.NAV_NO
            JournalpostKanal.NAV_NO_CHAT ->Kanal.NAV_NO
            JournalpostKanal.NAV_NO_UINNLOGGET->Kanal.NAV_NO
            JournalpostKanal.SKAN_NETS->Kanal.SKAN_NETS
            JournalpostKanal.LOKAL_UTSKRIFT->Kanal.LOKAL_UTSKRIFT
            JournalpostKanal.SENTRAL_UTSKRIFT->Kanal.SENTRAL_UTSKRIFT
            JournalpostKanal.SDP->Kanal.SDP
            else -> null
        }
    }
    fun isSentralPrint() = hentKanal() == Kanal.SENTRAL_UTSKRIFT
    fun harJournalforendeEnhetLik(enhet: String) = journalforendeEnhet == enhet
    fun isTemaEqualTo(likTema: String) = tema == likTema
    fun hentJournalpostIdLong() = journalpostId?.toLong()
    fun hentJournalpostIdMedPrefix() = "JOARK-$journalpostId"
    fun hentJournalpostType() = if (journalposttype == JournalpostType.N) "X" else journalposttype?.name
    fun hentDatoJournalfort(): LocalDate? {
        val journalfort = relevanteDatoer
            .find { it.datotype == DATO_JOURNALFORT }

        return journalfort?.somDato()
    }

    fun hasReturDetaljerWithDate(date: LocalDate) = !tilleggsopplysninger.hentReturDetaljerLogDO().stream().filter { it.dato == date }.findAny().isEmpty

    fun hentJournalfortAv(): String? {
       return tilleggsopplysninger.hentJournalfortAv() ?: journalfortAvNavn
    }

    fun hentReturDetaljer(): ReturDetaljer? {
        val returDetaljerLog = hentReturDetaljerLog()
        if (isDistribusjonKommetIRetur() || returDetaljerLog.isNotEmpty()){
            val senestReturDato = returDetaljerLog.filter{it.dato != null && it.dato!!.isAfter(hentDatoDokument())}.maxOfOrNull { it.dato!! }
            return ReturDetaljer(
                dato = hentDatoRetur() ?: senestReturDato,
                logg = returDetaljerLog,
                antall = returDetaljerLog.size
            )
        }

        return null;
    }

    fun manglerReturDetaljForSisteRetur(): Boolean{
        if(!isDistribusjonKommetIRetur()){
            return false
        }
        val returDetaljerLog = tilleggsopplysninger.hentReturDetaljerLogDO()
        return returDetaljerLog.none { it.dato.isAfter(hentDatoDokument()) }
    }

    fun hentReturDetaljerLog(): List<ReturDetaljerLog> {
        val returDetaljerLog = tilleggsopplysninger.hentReturDetaljerLogDO()
        val logg = returDetaljerLog.map { ReturDetaljerLog(
            dato = it.dato,
            beskrivelse = it.beskrivelse
        ) }.toMutableList()


        if (manglerReturDetaljForSisteRetur()){
            logg.add(0, ReturDetaljerLog(dato = hentDatoRetur(), beskrivelse = "Returpost"))
        }

        return logg
    }

    fun hentDatoRegistrert(): LocalDate? {
        val registrert = relevanteDatoer
            .find { it.datotype == DATO_REGISTRERT }

        return registrert?.somDato()
    }

    fun hentDatoDokument(): LocalDate? {
        val registrert = relevanteDatoer
            .find { it.datotype == DATO_DOKUMENT }

        return registrert?.somDato()
    }

    fun hentDatoRetur(): LocalDate? {
        val returDato = relevanteDatoer.find { it.datotype == DATO_RETUR && it.dato != "2022-01-01T01:00" }
        return returDato?.somDato()
    }

    fun hentTilknyttetSaker(): Set<String> {
        val saksnummer = sak?.fagsakId
        val saksnummerList = if (saksnummer != null) mutableSetOf(saksnummer) else mutableSetOf()
        saksnummerList.addAll(tilknyttedeSaker)
        return saksnummerList
    }

    fun leggTilTilknyttetSak(tilknyttetSak: String) {
        tilknyttedeSaker = tilknyttedeSaker + listOf(tilknyttetSak)
    }
    fun hentBrevkodeDto(): KodeDto? = if (hentBrevkode()!=null) KodeDto(kode = hentBrevkode()) else null

    fun hentHoveddokument(): Dokument? = if (dokumenter.isNotEmpty()) dokumenter[0] else null
    fun hentTittel(): String? = hentHoveddokument()?.tittel ?: tittel
    fun tilJournalpostDto(): JournalpostDto {

        @Suppress("UNCHECKED_CAST")
        return JournalpostDto(
            avsenderNavn = avsenderMottaker?.navn,
            avsenderMottaker = if (avsenderMottaker != null) AvsenderMottakerDto(
                navn = avsenderMottaker!!.navn,
                ident = avsenderMottaker!!.id,
                type = when(avsenderMottaker!!.type){
                        AvsenderMottakerIdType.FNR -> AvsenderMottakerDtoIdType.FNR
                        AvsenderMottakerIdType.ORGNR -> AvsenderMottakerDtoIdType.ORGNR
                        else -> AvsenderMottakerDtoIdType.UKJENT
                }
            ) else null,
            dokumenter = dokumenter.stream().map { dok -> dok?.tilDokumentDto(hentJournalpostType()) }.collect(toList()) as List<DokumentDto>,
            dokumentDato = hentDokumentDato(),
            ekspedertDato = hentEkspedertDato(),
            dokumentType = hentJournalpostType(),
            fagomrade = tema,
            kilde = hentKanal(),
            kanal = hentKanal(),
            gjelderAktor = bruker?.tilAktorDto(),
            feilfort = isFeilregistrert(),
            innhold = tittel,
            journalfortDato = hentDatoJournalfort(),
            journalforendeEnhet = journalforendeEnhet,
            journalfortAv = hentJournalfortAv(),
            journalpostId = "JOARK-$journalpostId",
            journalstatus = hentJournalStatus(),
            mottattDato = hentDatoRegistrert(),
            returDetaljer = hentReturDetaljer(),
            brevkode = hentBrevkodeDto(),
            distribuertTilAdresse = tilleggsopplysninger.hentAdresseDo()?.toDistribuerTilAdresse()
        )
    }

    fun tilAvvik(): List<AvvikType> {
        val avvikTypeList = mutableListOf<AvvikType>()
        if (isStatusMottatt()) avvikTypeList.add(AvvikType.OVERFOR_TIL_ANNEN_ENHET)
        if (isStatusMottatt()) avvikTypeList.add(AvvikType.TREKK_JOURNALPOST)
        if (!isStatusMottatt() && hasSak() && !isStatusFeilregistrert()) avvikTypeList.add(AvvikType.FEILFORE_SAK)
        if (isInngaaendeDokument() && !isStatusFeilregistrert()) avvikTypeList.add(AvvikType.ENDRE_FAGOMRADE)
        if (isInngaaendeDokument() && isStatusJournalfort()) avvikTypeList.add(AvvikType.SEND_TIL_FAGOMRADE)
        if (isUtgaaendeDokument() && isDistribusjonKommetIRetur()) avvikTypeList.add(AvvikType.BESTILL_NY_DISTRIBUSJON)
        if (isUtgaaendeDokument() && isStatusFerdigsstilt() && !isDistribusjonBestilt() && kanal != JournalpostKanal.INGEN_DISTRIBUSJON) avvikTypeList.add(AvvikType.MANGLER_ADRESSE)
        return avvikTypeList
    }

    fun hasMottakerId(): Boolean = avsenderMottaker?.id != null
    fun isMottakerIdSamhandlerId(): Boolean = avsenderMottaker?.id?.startsWith("8") ?: avsenderMottaker?.id?.startsWith("9") ?: false
    fun hasSak(): Boolean = sak != null
    fun isStatusFeilregistrert(): Boolean = journalstatus == JournalStatus.FEILREGISTRERT
    fun isStatusMottatt(): Boolean = journalstatus == JournalStatus.MOTTATT
    fun isTemaBidrag(): Boolean = tema == "BID" || tema == "FAR"
    fun isStatusFerdigsstilt(): Boolean = journalstatus == JournalStatus.FERDIGSTILT
    fun isStatusJournalfort(): Boolean = journalstatus == JournalStatus.JOURNALFOERT
    fun isInngaaendeJournalfort(): Boolean = isInngaaendeDokument() && isStatusJournalfort()
    fun isStatusEkspedert(): Boolean = journalstatus == JournalStatus.EKSPEDERT
    fun kanTilknytteSaker(): Boolean = journalstatus == JournalStatus.JOURNALFOERT || journalstatus == JournalStatus.FERDIGSTILT || journalstatus == JournalStatus.EKSPEDERT
    fun isInngaaendeDokument(): Boolean = journalposttype == JournalpostType.I
    fun isUtgaaendeDokument(): Boolean = journalposttype == JournalpostType.U

    fun tilJournalpostResponse(): JournalpostResponse {
        val journalpost = tilJournalpostDto()
        val saksnummer = sak?.fagsakId
        val saksnummerList = if (saksnummer != null) mutableListOf(saksnummer) else mutableListOf()
        saksnummerList.addAll(tilknyttedeSaker)
        return JournalpostResponse(journalpost, saksnummerList)
    }

    private fun hentEkspedertDato(): LocalDate? {
        val registrert = relevanteDatoer
            .find { it.datotype == DATO_EKSPEDERT }

        return registrert?.somDato()
    }

    private fun hentDokumentDato(): LocalDate? {
        val registrert = relevanteDatoer
            .find { it.datotype == DATO_DOKUMENT }

        return registrert?.somDato()
    }

    private fun erTilknyttetSak(saksnummer: String?) = sak?.fagsakId == saksnummer
    fun hentAvsenderNavn() = avsenderMottaker?.navn
    fun erIkkeTilknyttetSakNarOppgitt(saksnummer: String?) = if (saksnummer == null) false else !erTilknyttetSak(saksnummer)
    fun kanJournalfores(journalpost: Journalpost): Boolean{
        return journalpost.isStatusMottatt() && journalpost.hasSak();
    }

    fun hentAntallDokumenter(): Int = dokumenter.size

}

enum class JournalpostKanal {
    NAV_NO,
    NAV_NO_CHAT,
    NAV_NO_UINNLOGGET,
    SKAN_NETS,
    LOKAL_UTSKRIFT,
    SENTRAL_UTSKRIFT,
    SDP,
    INGEN_DISTRIBUSJON
}
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DistribuertTilAdresseDo(
        var adresselinje1: String?,
        var adresselinje2: String?,
        var adresselinje3: String?,
        var land: String,
        var postnummer: String?,
        var poststed: String?
){
    private fun asJsonString(): String = toJsonString(this)
    fun toMap(): List<Map<String, String>> = asJsonString().chunked(100).mapIndexed{ index, it -> mapOf("nokkel" to "$DISTRIBUERT_ADRESSE_KEY${index}", "verdi" to it) }
    fun toDistribuerTilAdresse(): DistribuerTilAdresse {
        return DistribuerTilAdresse(
            adresselinje1 = adresselinje1,
            adresselinje2 = adresselinje2,
            adresselinje3 = adresselinje3,
            land = land,
            postnummer = postnummer,
            poststed = poststed
        )
    }
}

class TilleggsOpplysninger: MutableList<Map<String, String>> by mutableListOf() {

    private var REGEX_NOT_NUMBER = "\\D".toRegex()

    private fun extractIndexFromKey(keyWithIndex: String): Int {
        val indexStr = keyWithIndex.replace(REGEX_NOT_NUMBER, "")
        return try {
            Integer.parseInt(indexStr)
        } catch (e: NumberFormatException){
            -1
        }
    }
    fun hentJournalfortAv(): String? {
        return this.filter { it["nokkel"]?.contains(JOURNALFORT_AV_KEY) ?: false}
            .filter { Strings.isNotEmpty(it["verdi"]) }
            .map { it["verdi"] }
            .firstOrNull()
    }

    fun removeDistribusjonMetadata(){
        this.removeAll{ it["nokkel"]?.contains(DISTRIBUSJON_BESTILT_KEY) ?: false}
        this.removeAll{ it["nokkel"]?.contains(DISTRIBUERT_ADRESSE_KEY) ?: false}
    }

    fun setDistribusjonBestillt() {
      this.removeAll{ it["nokkel"]?.contains(DISTRIBUSJON_BESTILT_KEY) ?: false}
      this.add(mapOf("nokkel" to DISTRIBUSJON_BESTILT_KEY, "verdi" to "true"))
    }

    fun isDistribusjonBestilt(): Boolean{
        return this.any { it["nokkel"]?.contains(DISTRIBUSJON_BESTILT_KEY) ?: false }
    }

    fun setNyDistribusjonBestiltFlagg() {
        this.removeAll{ it["nokkel"]?.contains(AVVIK_NY_DISTRIBUSJON_BESTILT_KEY) ?: false}
        this.add(mapOf("nokkel" to AVVIK_NY_DISTRIBUSJON_BESTILT_KEY, "verdi" to "true"))
    }

    fun isNyDistribusjonBestilt(): Boolean {
        return this.filter { it["nokkel"]?.contains(AVVIK_NY_DISTRIBUSJON_BESTILT_KEY) ?: false }.any { it["verdi"] == "true" }
    }

    fun setEndretTemaFlagg() {
        this.removeAll{ it["nokkel"]?.contains(AVVIK_ENDRET_TEMA_KEY) ?: false}
        this.add(mapOf("nokkel" to AVVIK_ENDRET_TEMA_KEY, "verdi" to "true"))
    }

    fun removeEndretTemaFlagg() {
        this.removeAll{ it["nokkel"]?.contains(AVVIK_ENDRET_TEMA_KEY) ?: false}
        this.add(mapOf("nokkel" to AVVIK_ENDRET_TEMA_KEY, "verdi" to "false"))
    }

    fun isEndretTema(): Boolean{
        return this.filter { it["nokkel"]?.contains(AVVIK_ENDRET_TEMA_KEY) ?: false }.any { it["verdi"] == "true" }
    }

    fun addMottakerAdresse(adresseDo: DistribuertTilAdresseDo){
        this.removeAll{ it["nokkel"]?.contains(DISTRIBUERT_ADRESSE_KEY) ?: false}
        this.addAll(adresseDo.toMap())
    }

    fun addReturDetaljLog(returDetaljerLogDO: ReturDetaljerLogDO){
        this.addAll(returDetaljerLogDO.toMap())
    }

    fun updateReturDetaljLog(originalDate: LocalDate, returDetaljerLogDO: ReturDetaljerLogDO){
        val updatedTilleggsopplysninger = hentReturDetaljerLogDO().map { if (it.dato == originalDate) returDetaljerLogDO else it }.flatMap { it.toMap() }
        this.removeAll{ it["nokkel"]?.contains(RETUR_DETALJER_KEY) ?: false}
        this.addAll(updatedTilleggsopplysninger)
    }

    fun hentAdresseDo(): DistribuertTilAdresseDo? {
        // Key format (DISTRIBUERT_ADRESSE_KEY)(index)
        val adresseKeyValueMapList = this.filter { it["nokkel"]?.contains(DISTRIBUERT_ADRESSE_KEY) ?: false}
            .filter { Strings.isNotEmpty(it["verdi"]) }
            .sortedBy { extractIndexFromKey(it["nokkel"]!!) }

        if (adresseKeyValueMapList.isEmpty()){
            return null
        }

        val adresseJsonString = adresseKeyValueMapList.map { it["verdi"] }.joinToString("")
        return fromJsonString(adresseJsonString)
    }

    fun hentReturDetaljerLogDO(): List<ReturDetaljerLogDO> {
        // Key format (RETUR_DETALJER_KEY)(index)_(date)
        val returDetaljer = this.filter { it["nokkel"]?.contains(RETUR_DETALJER_KEY) ?: false}
            .filter { Strings.isNotEmpty(it["verdi"]) }
            .filter {
                val keySplit = it["nokkel"]!!.split("_")
                if (keySplit.size > 1) DateUtils.isValid(keySplit[1]) else false
            }
            .sortedBy { extractIndexFromKey(it["nokkel"]!!.split("_")[0]) }

        val returDetaljerList: MutableList<ReturDetaljerLogDO> = mutableListOf()
        returDetaljer.forEach {
            val dato = DateUtils.parseDate(it["nokkel"]!!.split("_")[1])
            val beskrivelse = it["verdi"]!!
            val existing = returDetaljerList.find{ it.dato == dato }
            if (existing != null){
                existing.beskrivelse = existing.beskrivelse + beskrivelse
            } else {
                returDetaljerList.add(ReturDetaljerLogDO(beskrivelse, dato!!))
            }

        }
        return returDetaljerList
    }

}
data class ReturDetaljerLogDO(
    var beskrivelse: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    var dato: LocalDate
) {
    fun toMap(): List<Map<String, String>> = beskrivelse.chunked(100).mapIndexed{ index, it -> mapOf("nokkel" to "$RETUR_DETALJER_KEY${index}_${DateUtils.formatDate(dato)}", "verdi" to it) }
}
data class AvsenderMottaker(
    var navn: String? = null,
    var id: String? = null,
    var type: AvsenderMottakerIdType? = null
)

enum class AvsenderMottakerIdType {
    FNR,
    UKJENT,
    ORGNR,
    HPRNR,
    UTL_ORG,
    NULL
}

enum class BrukerType {
    AKTOERID,
    FNR
}
data class Bruker(
    var id: String? = null,
    var type: String? = null
) {
    fun tilAktorDto(): AktorDto {
        return if (id != null) AktorDto(id!!, type ?: BrukerType.FNR.name) else throw JournalpostDataException("ingen id i $this")
    }

    @JsonIgnore
    fun isAktoerId(): Boolean {
        return this.type == BrukerType.AKTOERID.name
    }
}

data class Dokument(
    var tittel: String? = null,
    var dokumentInfoId: String? = null,
    var brevkode: String? = null,
) {
    fun tilDokumentDto(journalposttype: String?): DokumentDto = DokumentDto(
        dokumentreferanse = this.dokumentInfoId,
        dokumentType = journalposttype,
        tittel = this.tittel,
    )
}

data class DatoType(
    var dato: String? = null,
    var datotype: String? = null
) {
    fun somDato(): LocalDate {
        val datoStreng = dato?.substring(0, 10)

        return if (datoStreng != null) LocalDate.parse(datoStreng) else throw JournalpostDataException("Kunne ikke trekke ut dato fra: $dato")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Sak(
    var fagsakId: String? = null,
    var fagsakSystem: String? = null,
    var sakstype: String? = null,
    var tema: String? = null
) {
    constructor(fagsakId: String?): this(fagsakId, null, null, null)
}

enum class JournalStatus {
    MOTTATT,
    JOURNALFOERT,
    FERDIGSTILT,
    EKSPEDERT,
    UNDER_ARBEID,
    FEILREGISTRERT,
    UTGAAR,
    AVBRUTT,
    UKJENT_BRUKER,
    RESERVERT,
    OPPLASTING_DOKUMENT,
    UKJENT
}

data class EndreJournalpostCommandIntern(
    val endreJournalpostCommand: EndreJournalpostCommand,
    val enhet: String?,
    val endreAdresse: DistribuertTilAdresseDo?
) {
    constructor(endreJournalpostCommand: EndreJournalpostCommand, enhet: String): this(endreJournalpostCommand, enhet, null)
    fun skalJournalfores() = endreJournalpostCommand.skalJournalfores
    fun hentAvsenderNavn(journalpost: Journalpost) = endreJournalpostCommand.avsenderNavn ?: journalpost.hentAvsenderNavn()
    fun harEnTilknyttetSak(): Boolean = endreJournalpostCommand.tilknyttSaker.isNotEmpty()
    fun harGjelder(): Boolean = endreJournalpostCommand.gjelder != null
    fun hentTilknyttetSak() = endreJournalpostCommand.tilknyttSaker.first()
    fun hentTilknyttetSaker() = endreJournalpostCommand.tilknyttSaker
    fun hentFagomrade() = endreJournalpostCommand.fagomrade
    fun hentGjelder() = endreJournalpostCommand.gjelder
    fun hentGjelderType() = if (endreJournalpostCommand.gjelderType != null) endreJournalpostCommand.gjelderType!! else "FNR"

    fun sjekkGyldigEndring(journalpost: Journalpost){
        val violations = mutableListOf<String>()

        if (skalJournalfores()){
            if (!journalpost.isStatusMottatt()){
                violations.add("Journalpost med journalstatus ${journalpost.journalstatus} kan ikke journalføres")
            }
            if (!journalpost.hasSak() && endreJournalpostCommand.tilknyttSaker.isEmpty()){
                violations.add("Kan ikke registrere journalpost uten sak")
            }
            if (journalpost.bruker == null && endreJournalpostCommand.manglerGjelder()) {
                violations.add("Kan ikke registrere journalpost når det mangler gjelder")
            }
        } else if (journalpost.isStatusMottatt()){
            if (endreJournalpostCommand.tilknyttSaker.size > 1){
                violations.add("Kan ikke lagre journalpost med flere saker uten å journalføre")
            }
        } else if (journalpost.isUtgaaendeDokument()){
            if (!erGyldigEndringAvReturDato(journalpost)){
                violations.add("Returdetaljer inneholder ugyldig endring av returdato")
            }
        }
        if (violations.isNotEmpty()) {
            throw ViolationException(violations)
        }
    }

    fun erGyldigEndringAvReturDato(journalpost: Journalpost): Boolean {
        val endreReturDetaljer = endreJournalpostCommand.endreReturDetaljer?.filter { Strings.isNotEmpty(it.beskrivelse) }
        if (endreReturDetaljer != null && endreReturDetaljer.isNotEmpty()) {
            val manglerOriginalDato = !journalpost.manglerReturDetaljForSisteRetur() && endreReturDetaljer.any{it.originalDato == null}
            val nyReturDatoErIkkeEtterDokumentDato = endreReturDetaljer.none{it.originalDato == null && it.nyDato != null && it.nyDato!!.isBefore(journalpost.hentDatoDokument())}
//            val harIkkeEndretDatoPaaEldreReturDetaljer = endreReturDetaljer.filter{it.originalDato != null}.all{it.originalDato == it.nyDato}
            return nyReturDatoErIkkeEtterDokumentDato && manglerOriginalDato
        }
        return true
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TilknyttetJournalpost(
    var journalpostId: Long,
    var journalstatus: JournalStatus,
    var sak: Sak?
)

fun returDetaljerDOListDoToMap(returDetaljerLog: List<ReturDetaljerLogDO>): Map<String, String>{
    return mapOf("nokkel" to RETUR_DETALJER_KEY, "verdi" to jacksonObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(returDetaljerLog))
}

enum class JournalpostType(var dekode: String) {
    N("Notat"),
    I("Inngående dokument"),
    U("Utgående dokument")
}