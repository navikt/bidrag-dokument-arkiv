package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.bidrag.dokument.arkiv.SECURE_LOGGER
import no.nav.bidrag.dokument.arkiv.consumer.dto.DokumentSoknadDto
import no.nav.bidrag.dokument.arkiv.model.JournalpostDataException
import no.nav.bidrag.dokument.arkiv.model.ViolationException
import no.nav.bidrag.dokument.arkiv.model.runCatchingIgnoreException
import no.nav.bidrag.dokument.arkiv.utils.DateUtils
import no.nav.bidrag.dokument.arkiv.utils.JsonMapper.fromJsonString
import no.nav.bidrag.dokument.arkiv.utils.JsonMapper.toJsonString
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.transport.dokument.AktorDto
import no.nav.bidrag.transport.dokument.AvsenderMottakerDto
import no.nav.bidrag.transport.dokument.AvsenderMottakerDtoIdType
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.DistribuerTilAdresse
import no.nav.bidrag.transport.dokument.DokumentArkivSystemDto
import no.nav.bidrag.transport.dokument.DokumentDto
import no.nav.bidrag.transport.dokument.DokumentStatusDto
import no.nav.bidrag.transport.dokument.EndreJournalpostCommand
import no.nav.bidrag.transport.dokument.EttersendingsoppgaveStatus
import no.nav.bidrag.transport.dokument.EttersendingsoppgaveVedleggDto
import no.nav.bidrag.transport.dokument.EttersendingsppgaveDto
import no.nav.bidrag.transport.dokument.EttersingdsoppgaveOpplastingsStatus
import no.nav.bidrag.transport.dokument.FARSKAP_UTELUKKET_PREFIKS
import no.nav.bidrag.transport.dokument.IdentType
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.dokument.JournalpostStatus
import no.nav.bidrag.transport.dokument.Kanal
import no.nav.bidrag.transport.dokument.KodeDto
import no.nav.bidrag.transport.dokument.MottakerAdresseTo
import no.nav.bidrag.transport.dokument.OpprettEttersendingsoppgaveVedleggDto
import no.nav.bidrag.transport.dokument.OpprettEttersendingsppgaveDto
import no.nav.bidrag.transport.dokument.ReturDetaljer
import no.nav.bidrag.transport.dokument.ReturDetaljerLog
import no.nav.bidrag.transport.dokument.isNumeric
import org.apache.logging.log4j.util.Strings
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Collectors.toList

private val LOGGER = KotlinLogging.logger {}

// Max key length is 20
const val RETUR_DETALJER_KEY = "retur"
const val DISTRIBUERT_ADRESSE_KEY = "distAdresse"
const val ETTERSENDINGSOPPGAVE_KEY = "ettOppgave"
const val DISTRIBUERT_AV_IDENT_KEY = "distribuertAvIdent"
const val DOKDIST_BESTILLING_ID = "dokdistBestillingsId"
const val SAMHANDLER_ID_KEY = "samhandlerId"
const val DISTRIBUSJON_BESTILT_KEY = "distribusjonBestilt"
const val ORIGINALT_DISTRIBUERT_DIGITALT_KEY = "origDistDigitalt"
const val AVVIK_ENDRET_TEMA_KEY = "avvikEndretTema"
const val ORIGINAL_BESTILT_KEY = "originalBestilt"
const val AVVIK_NY_DISTRIBUSJON_BESTILT_KEY = "avvikNyDistribusjon"
const val JOURNALFORT_AV_KEY = "journalfortAv"
const val JOURNALFORT_AV_IDENT_KEY = "journalfortAvIdent"
private const val DATO_DOKUMENT = "DATO_DOKUMENT"
private const val DATO_EKSPEDERT = "DATO_EKSPEDERT"
private const val DATO_JOURNALFORT = "DATO_JOURNALFOERT"
private const val DATO_REGISTRERT = "DATO_REGISTRERT"
private const val DATO_RETUR = "DATO_AVS_RETUR"

object JournalstatusDto {
    const val EKSPEDERT = "E"
    const val AVBRUTT = "A"
    const val KLAR_TIL_PRINT = "KP"
    const val UNDER_PRODUKSJON = "D"
    const val RETUR = "RE"
    const val JOURNALFORT = "J"
    const val FERDIGSTILT = "FS"
    const val FEILREGISTRERT = "F"
    const val MOTTAKSREGISTRERT = "M"
    const val RESERVERT = "R"
    const val UTGAR = "U"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DistribusjonsInfo(
    val journalposttype: String? = null,
    val journalstatus: String,
    val kanal: JournalpostKanal? = null,
    val utsendingsinfo: UtsendingsInfo? = null,
    val tilleggsopplysninger: TilleggsOpplysninger = TilleggsOpplysninger(),
    val relevanteDatoer: List<DatoType> = emptyList(),
) {
    fun hentDatoDokument(): LocalDateTime? {
        val registrert = relevanteDatoer
            .find { it.datotype == DATO_DOKUMENT }

        return registrert?.dato?.let { LocalDateTime.parse(it) }
    }

    fun hentDatoEkspedert(): LocalDateTime? {
        val registrert = relevanteDatoer
            .find { it.datotype == DATO_EKSPEDERT }

        return registrert?.dato?.let { LocalDateTime.parse(it) }
    }

    fun hentDistribuertAvIdent(): String? = tilleggsopplysninger.hentDistribuertAvIdent()

    fun hentBestillingId(): String? = tilleggsopplysninger.hentBestillingId()

    fun isStatusEkspedert(): Boolean = journalstatus == JournalStatus.EKSPEDERT.name
    fun isUtgaaendeDokument(): Boolean = journalposttype == JournalpostType.U.name
    fun isDistribusjonBestilt(): Boolean = tilleggsopplysninger.isDistribusjonBestilt() || isStatusEkspedert()

    fun hentJournalStatus(): JournalpostStatus = when (journalstatus) {
        JournalStatus.MOTTATT.name -> JournalpostStatus.MOTTATT
        JournalStatus.JOURNALFOERT.name -> JournalpostStatus.JOURNALFØRT
        JournalStatus.FEILREGISTRERT.name -> JournalpostStatus.FEILREGISTRERT
        JournalStatus.EKSPEDERT.name -> JournalpostStatus.EKSPEDERT
        JournalStatus.FERDIGSTILT.name ->
            if (isUtgaaendeDokument() && kanal != JournalpostKanal.INGEN_DISTRIBUSJON) {
                if (isDistribusjonBestilt() || kanal == JournalpostKanal.LOKAL_UTSKRIFT) {
                    JournalpostStatus.DISTRIBUERT
                } else {
                    JournalpostStatus.KLAR_FOR_DISTRIBUSJON
                }
            } else {
                JournalpostStatus.FERDIGSTILT
            }

        JournalStatus.UNDER_ARBEID.name, JournalStatus.RESERVERT.name -> JournalpostStatus.UNDER_PRODUKSJON
        JournalStatus.UTGAAR.name -> JournalpostStatus.UTGÅR
        JournalStatus.AVBRUTT.name -> JournalpostStatus.AVBRUTT
        else -> JournalpostStatus.UKJENT
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class UtsendingsInfo(
    val digitalpostSendt: DigitalpostSendt? = null,
    val epostVarselSendt: EpostVarselSendt? = null,
    val fysiskpostSendt: FysiskpostSendt? = null,
    val smsVarselSendt: SmsVarselSendt? = null,
    val varselSendt: List<VarselSendt> = emptyList(),
) {
    val sisteVarselSendt get() = if (varselSendt.isNotEmpty()) varselSendt[0] else null
    val varselType get() = if (varselSendt.isNotEmpty()) varselSendt[0] else null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpostVarselSendt(val adresse: String, val tittel: String, val varslingstekst: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DigitalpostSendt(val adresse: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FysiskpostSendt(val adressetekstKonvolutt: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VarselSendt(
    val varslingstidspunkt: LocalDateTime?,
    val varslingstekst: String,
    val adresse: String,
    val tittel: String?,
    val type: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SmsVarselSendt(val varslingstekst: String, val adresse: String)

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
    var tilleggsopplysninger: TilleggsOpplysninger = TilleggsOpplysninger(),
    val utsendingsinfo: UtsendingsInfo? = null,
) {

    fun distribuertTilAdresse(): DistribuerTilAdresse? = parseFysiskPostadrese() ?: tilleggsopplysninger.hentAdresseDo()?.toDistribuerTilAdresse()
    fun ettersendingsoppgave(): EttersendingsppgaveDto? = tilleggsopplysninger.hentInnsendingsoppgave()?.toEttersendingsoppgaveDto()

    fun mottakerAdresse(): MottakerAdresseTo? = distribuertTilAdresse()?.let {
        MottakerAdresseTo(
            adresselinje1 = it.adresselinje1 ?: "",
            adresselinje2 = it.adresselinje2,
            adresselinje3 = it.adresselinje3,
            postnummer = it.postnummer,
            poststed = it.poststed,
            landkode = it.land,
        )
    }

    private fun parseFysiskPostadrese(): DistribuerTilAdresse? {
        return try {
            utsendingsinfo?.fysiskpostSendt?.adressetekstKonvolutt?.let {
                val postadresseSplit = it.split("\n").reversed()
                val landkode2 = postadresseSplit.getOrNull(0)
                val adresse = if (landkode2 != "NO") {
                    val postadresseList = postadresseSplit.subList(1, postadresseSplit.size).reversed()
                    val postadresseOgSted = if (postadresseList.size == 4) postadresseList.lastOrNull()?.split(" ") ?: emptyList() else emptyList()
                    val postnummer = if (postadresseOgSted.firstOrNull()?.isNumeric == true) postadresseOgSted.firstOrNull() else null
                    val poststed = postadresseOgSted.subList(if (postadresseOgSted.firstOrNull()?.isNumeric == true) 1 else 0, postadresseOgSted.size)
                        .joinToString(" ")
                    DistribuerTilAdresse(
                        adresselinje1 = postadresseList.getOrNull(0),
                        adresselinje2 = postadresseList.getOrNull(1),
                        adresselinje3 = postadresseList.getOrNull(2),
                        postnummer = postnummer,
                        poststed = poststed.ifEmpty { null },
                        land = landkode2,
                    )
                } else {
                    val postnummerPoststed = postadresseSplit.getOrNull(1)?.split(" ") ?: emptyList()
                    val postnummer = if (postnummerPoststed.size == 2) postnummerPoststed.getOrNull(0) else null
                    val poststed =
                        if (postnummerPoststed.size == 1) {
                            postnummerPoststed.getOrNull(0)
                        } else {
                            postnummerPoststed.getOrNull(
                                1,
                            )
                        }
                    val adresselinje1 = when (postadresseSplit.size) {
                        3 -> postadresseSplit.getOrNull(2)
                        4 -> postadresseSplit.getOrNull(3)
                        5 -> postadresseSplit.getOrNull(4)
                        else -> null
                    }
                    val adresselinje2 = when (postadresseSplit.size) {
                        4 -> postadresseSplit.getOrNull(2)
                        5 -> postadresseSplit.getOrNull(3)
                        else -> null
                    }
                    val adresselinje3 = if (postadresseSplit.size == 5) postadresseSplit.getOrNull(2) else null
                    DistribuerTilAdresse(
                        adresselinje1 = adresselinje1,
                        adresselinje2 = adresselinje2,
                        adresselinje3 = adresselinje3,
                        poststed = poststed,
                        postnummer = postnummer,
                        land = landkode2,
                    )
                }
//                SECURE_LOGGER.info {
//                    "Lest og mappet postadresse fra SAF ${
//                        it.split("\n").joinToString("\\n")
//                    } til $adresse"
//                }
                return adresse
            }
        } catch (e: Exception) {
            SECURE_LOGGER.error(e) { "Det skjedde en feil ved mapping av adresse fra SAF ${utsendingsinfo?.fysiskpostSendt?.adressetekstKonvolutt}" }
            return null
        }
    }

    fun isBidragTema(): Boolean = tema == "BID" || tema == "FAR"
    fun isFarskap(): Boolean = tema == "FAR"
    fun isFarskapUtelukket(): Boolean = hentTittel()?.startsWith(FARSKAP_UTELUKKET_PREFIKS, ignoreCase = true) == true

    fun hentGjelderId(): String? = bruker?.id

    fun harAvsenderMottaker(): Boolean = avsenderMottaker?.navn != null || avsenderMottaker?.id != null

    fun hentAvsenderMottakerId(): String? = avsenderMottaker?.id
    fun hentJournalStatus(): JournalpostStatus? = if (isDistribusjonKommetIRetur() && kanal != JournalpostKanal.INGEN_DISTRIBUSJON) {
        JournalpostStatus.RETUR
    } else {
        when (journalstatus) {
            JournalStatus.MOTTATT -> JournalpostStatus.MOTTATT
            JournalStatus.JOURNALFOERT -> JournalpostStatus.JOURNALFØRT
            JournalStatus.FEILREGISTRERT -> JournalpostStatus.FEILREGISTRERT
            JournalStatus.EKSPEDERT -> JournalpostStatus.EKSPEDERT
            JournalStatus.FERDIGSTILT ->
                if (hentBrevkodeDto()?.kode == "CRM_MELDINGSKJEDE") {
                    JournalpostStatus.JOURNALFØRT
                } else if (isUtgaaendeDokument() && kanal != JournalpostKanal.INGEN_DISTRIBUSJON) {
                    if (isDistribusjonBestilt() || kanal == JournalpostKanal.LOKAL_UTSKRIFT) {
                        JournalpostStatus.EKSPEDERT
                    } else {
                        JournalpostStatus.KLAR_FOR_DISTRIBUSJON
                    }
                } else if (isNotat()) {
                    JournalpostStatus.RESERVERT
                } else {
                    JournalpostStatus.JOURNALFØRT
                }

            JournalStatus.UNDER_ARBEID, JournalStatus.RESERVERT -> JournalpostStatus.UNDER_PRODUKSJON
            JournalStatus.UTGAAR -> JournalpostStatus.UTGÅR
            JournalStatus.AVBRUTT -> JournalpostStatus.AVBRUTT
            else -> JournalpostStatus.fraKode(journalstatus?.name)
        }
    }

    fun isDistribusjonKommetIRetur() = (isDistribusjonBestilt()) && antallRetur != null && antallRetur!! > 0

    fun hentBrevkode(): String? = hentHoveddokument()?.brevkode

    fun isDistribusjonBestilt(): Boolean = tilleggsopplysninger.isDistribusjonBestilt() || isStatusEkspedert() || hasEkspedertDate()

    fun isFeilregistrert() = journalstatus == JournalStatus.FEILREGISTRERT

    fun hentKanal(): Kanal? = when (kanal) {
        JournalpostKanal.NAV_NO -> Kanal.NAV_NO
        JournalpostKanal.NAV_NO_CHAT -> Kanal.NAV_NO
        JournalpostKanal.NAV_NO_UINNLOGGET -> Kanal.NAV_NO
        JournalpostKanal.SKAN_NETS -> Kanal.SKAN_NETS
//            JournalpostKanal.SKAN_IM -> Kanal.SKAN_IM
        JournalpostKanal.LOKAL_UTSKRIFT -> Kanal.LOKAL_UTSKRIFT
        JournalpostKanal.SENTRAL_UTSKRIFT -> Kanal.SENTRAL_UTSKRIFT
        JournalpostKanal.SDP -> Kanal.SDP
        JournalpostKanal.INGEN_DISTRIBUSJON -> Kanal.INGEN_DISTRIBUSJON
        else -> null
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

    fun hasReturDetaljerWithDate(date: LocalDate) = !tilleggsopplysninger.hentReturDetaljerLogDO().stream().filter { it.dato == date }
        .findAny().isEmpty

    fun hasLockedReturDetaljerWithDate(date: LocalDate) = !tilleggsopplysninger.hentReturDetaljerLogDO().stream()
        .filter { it.dato == date && it.locked == true }.findAny().isEmpty

    fun hentJournalfortAvNavn(): String? = tilleggsopplysninger.hentJournalfortAv() ?: journalfortAvNavn

    fun hentJournalfortAvIdent(): String? = tilleggsopplysninger.hentJournalfortAvIdent()

    fun hentReturDetaljer(): ReturDetaljer? {
        val returDetaljerLog = hentReturDetaljerLog()
        if (isDistribusjonKommetIRetur() || returDetaljerLog.isNotEmpty()) {
            val senestReturDato = returDetaljerLog
                .filter { it.dato != null && it.locked != true }
                .filter {
                    !isDistribusjonKommetIRetur() ||
                        it.dato!!.isEqual(hentDatoDokument()) ||
                        it.dato!!.isAfter(
                            hentDatoDokument(),
                        )
                }
                .maxOfOrNull { it.dato!! }
            return ReturDetaljer(
                dato = if (isDistribusjonKommetIRetur()) {
                    hentDatoRetur() ?: senestReturDato
                } else {
                    null
                },
                logg = returDetaljerLog,
                antall = returDetaljerLog.size,
            )
        }

        return null
    }

    fun manglerReturDetaljForSisteRetur(): Boolean {
        if (!isDistribusjonKommetIRetur()) {
            return false
        }
        val returDetaljerLog = tilleggsopplysninger.hentReturDetaljerLogDO()
        return returDetaljerLog.filter { it.locked != true }
            .none { it.dato.isEqual(hentDatoDokument()) || it.dato.isAfter(hentDatoDokument()) }
    }

    fun hentReturDetaljerLog(): List<ReturDetaljerLog> {
        val returDetaljerLog = tilleggsopplysninger.hentReturDetaljerLogDO()
        val logg = returDetaljerLog.map {
            ReturDetaljerLog(
                dato = it.dato,
                beskrivelse = it.beskrivelse,
                locked = it.locked == true,
            )
        }.toMutableList()

        if (manglerReturDetaljForSisteRetur()) {
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
        val returDato = relevanteDatoer.find { it.datotype == DATO_RETUR }
        return returDato?.somDato()
    }

    fun hentTilknyttetSaker(): Set<String> {
        val saksnummer = sak?.fagsakId
        val saksnummerList = if (saksnummer != null) mutableSetOf(saksnummer) else mutableSetOf()
        saksnummerList.addAll(tilknyttedeSaker)
        return saksnummerList
    }

    fun hentSaksnummer(): String? = sak?.fagsakId
    fun leggTilTilknyttetSak(tilknyttetSak: String) {
        tilknyttedeSaker = tilknyttedeSaker + listOf(tilknyttetSak)
    }

    fun hentBrevkodeDto(): KodeDto? = if (hentBrevkode() != null) KodeDto(kode = hentBrevkode()) else null

    fun hentHoveddokument(): Dokument? = if (dokumenter.isNotEmpty()) dokumenter[0] else null
    fun hentTittel(): String? = hentHoveddokument()?.tittel ?: tittel
    fun tilJournalpostDto(ettersendingsOppgave: DokumentSoknadDto? = null): JournalpostDto {
        val erSamhandlerId = tilleggsopplysninger.hentSamhandlerId() != null
        @Suppress("UNCHECKED_CAST")
        return JournalpostDto(
            avsenderNavn = avsenderMottaker?.navn,
            avsenderMottaker = if (avsenderMottaker != null) {
                AvsenderMottakerDto(
                    navn = avsenderMottaker!!.navn,
                    ident = tilleggsopplysninger.hentSamhandlerId() ?: avsenderMottaker!!.id,
                    type = if (erSamhandlerId) {
                        AvsenderMottakerDtoIdType.UKJENT
                    } else {
                        when (avsenderMottaker!!.type) {
                            AvsenderMottakerIdType.FNR -> AvsenderMottakerDtoIdType.FNR
                            AvsenderMottakerIdType.ORGNR -> AvsenderMottakerDtoIdType.ORGNR
                            else -> AvsenderMottakerDtoIdType.UKJENT
                        }
                    },
                    adresse = mottakerAdresse(),
                )
            } else {
                null
            },
            dokumenter = dokumenter.stream()
                .map { dok -> dok?.tilDokumentDto(hentJournalpostType()) }
                .collect(toList()) as List<DokumentDto>,
            dokumentDato = hentDokumentDato(),
            ekspedertDato = hentEkspedertDato(),
            dokumentType = hentJournalpostType(),
            fagomrade = tema,
            kilde = hentKanal(),
            kanal = hentKanal(),
            gjelderAktor = bruker?.tilAktorDto(),
            feilfort = isFeilregistrert(),
            innhold = hentTittel(),
            journalfortDato = hentDatoJournalfort(),
            journalforendeEnhet = journalforendeEnhet,
            journalfortAv = hentJournalfortAvNavn(),
            journalpostId = "JOARK-$journalpostId",
            journalstatus = hentJournalStatus()?.kode,
            status = hentJournalStatus(),
            mottattDato = hentDatoRegistrert(),
            returDetaljer = hentReturDetaljer(),
            brevkode = hentBrevkodeDto(),
            distribuertTilAdresse = distribuertTilAdresse(),
            ettersendingsppgave = ettersendingsOppgave?.tilEttersendingsoppgaveDto() ?: ettersendingsoppgave(),
        )
    }

    fun tilAvvik(): List<AvvikType> {
        if (!isTemaBidrag()) {
            return if (isStatusMottatt() && isInngaaendeDokument()) {
                listOf(AvvikType.ENDRE_FAGOMRADE)
            } else if (isInngaaendeDokument() || isNotat()) {
                listOf(AvvikType.KOPIER_FRA_ANNEN_FAGOMRADE)
            } else {
                emptyList()
            }
        }
        val avvikTypeList = mutableListOf<AvvikType>()
        if (isStatusMottatt()) avvikTypeList.add(AvvikType.OVERFOR_TIL_ANNEN_ENHET)
        if (isStatusMottatt() && isTemaBidrag()) avvikTypeList.add(AvvikType.TREKK_JOURNALPOST)
        if (isSkanning() && !tilleggsopplysninger.isOriginalBestilt() && !isFeilregistrert()) {
            avvikTypeList.add(AvvikType.BESTILL_ORIGINAL)
        }
        if (isSkanning() && !isFeilregistrert()) avvikTypeList.add(AvvikType.BESTILL_RESKANNING)
        if (isSkanning() && !isFeilregistrert()) avvikTypeList.add(AvvikType.BESTILL_SPLITTING)
        if (!isStatusMottatt() && hasSak() && !isStatusFeilregistrert()) avvikTypeList.add(AvvikType.FEILFORE_SAK)
        if (isInngaaendeDokument() && !isStatusFeilregistrert()) avvikTypeList.add(AvvikType.ENDRE_FAGOMRADE)
        if (isInngaaendeDokument() && isStatusJournalfort()) avvikTypeList.add(AvvikType.SEND_TIL_FAGOMRADE)

        if (isUtgaaendeDokument() && isStatusEkspedert() && isLokalUtksrift()) {
            avvikTypeList.add(AvvikType.REGISTRER_RETUR)
        }
        if (isUtgaaendeDokument() && !isLokalUtksrift() && isDistribusjonKommetIRetur() && !tilleggsopplysninger.isNyDistribusjonBestilt()) {
            avvikTypeList.add(
                AvvikType.BESTILL_NY_DISTRIBUSJON,
            )
        }
        if (isUtgaaendeDokument() && kanal != JournalpostKanal.INGEN_DISTRIBUSJON) {
            if ((isStatusFerdigsstilt() || isStatusEkspedert()) &&
                isDistribusjonKommetIRetur() ||
                isStatusFerdigsstilt() &&
                !isDistribusjonBestilt()
            ) {
                avvikTypeList.add(
                    AvvikType.MANGLER_ADRESSE,
                )
            }
        }

        if (isFarskap() && !isFarskapUtelukket() && !isStatusMottatt()) avvikTypeList.add(AvvikType.FARSKAP_UTELUKKET)
        return avvikTypeList
    }

    fun hasMottakerId(): Boolean = avsenderMottaker?.id != null
    fun hasMottakerNavn(): Boolean = !avsenderMottaker?.navn.isNullOrEmpty()
    fun isMottakerIdSamhandlerId(): Boolean = avsenderMottaker?.id?.startsWith("8") ?: avsenderMottaker?.id?.startsWith("9") ?: false

    fun hasSak(): Boolean = sak != null
    fun isStatusFeilregistrert(): Boolean = journalstatus == JournalStatus.FEILREGISTRERT
    fun isStatusMottatt(): Boolean = journalstatus == JournalStatus.MOTTATT
    fun isTemaBidrag(): Boolean = tema == "BID" || tema == "FAR"
    fun isStatusFerdigsstilt(): Boolean = journalstatus == JournalStatus.FERDIGSTILT
    fun isStatusJournalfort(): Boolean = journalstatus == JournalStatus.JOURNALFOERT
    fun isInngaaendeJournalfort(): Boolean = isInngaaendeDokument() && isStatusJournalfort()
    fun isStatusEkspedert(): Boolean = journalstatus == JournalStatus.EKSPEDERT
    fun hasEkspedertDate(): Boolean = hentEkspedertDato() != null
    fun isLokalUtksrift(): Boolean = kanal == JournalpostKanal.LOKAL_UTSKRIFT
    fun kanTilknytteSaker(): Boolean = journalstatus == JournalStatus.JOURNALFOERT || journalstatus == JournalStatus.FERDIGSTILT || journalstatus == JournalStatus.EKSPEDERT

    fun isInngaaendeDokument(): Boolean = journalposttype == JournalpostType.I
    fun isNotat(): Boolean = journalposttype == JournalpostType.N
    fun isUtgaaendeDokument(): Boolean = journalposttype == JournalpostType.U

    fun isSkanning(): Boolean = kanal == JournalpostKanal.SKAN_IM

    fun erJournalførtSenereEnnEttÅrSiden() = isStatusJournalfort() &&
        hentDatoJournalfort() != null &&
        hentDatoJournalfort()!!.isBefore(
            LocalDate.now().minusYears(1),
        )

    fun tilJournalpostResponse(ettersendingsOppgave: DokumentSoknadDto? = null): JournalpostResponse {
        val journalpost = tilJournalpostDto(ettersendingsOppgave)
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

    fun kanJournalfores(journalpost: Journalpost): Boolean = journalpost.isStatusMottatt() && journalpost.hasSak()

    fun hentAntallDokumenter(): Int = dokumenter.size
}

enum class JournalpostKanal(val beskrivelse: String) {
    NAV_NO("Nav.no"),
    NAV_NO_UINNLOGGET("Nav.no uten ID-porten-pålogging"),
    NAV_NO_CHAT("Innlogget samtale"),
    INNSENDT_NAV_ANSATT("Registrert av Nav-ansatt"),
    LOKAL_UTSKRIFT("Lokal utskrift"),
    SENTRAL_UTSKRIFT("Sentral utskrift"),
    ALTINN("Altinn"),
    EESSI("EESSI"),
    EIA("EIA"),
    EKST_OPPS("Eksternt oppslag"),
    SDP("Digital postkasse til innbyggere"),
    TRYGDERETTEN("Trygderetten"),
    HELSENETTET("Helsenettet"),
    INGEN_DISTRIBUSJON("Ingen distribusjon"),
    UKJENT("Ukjent"),
    DPVT("Taushetsbelagt digital post til virksomhet"),

    SKAN_NETS("Skanning Nets"),
    SKAN_PEN("Skanning Pensjon"),
    SKAN_IM("Skanning Iron Mountain"),
}

fun JournalpostKanal.tilKanalDto() = when (this) {
    JournalpostKanal.NAV_NO -> Kanal.NAV_NO
    JournalpostKanal.NAV_NO_CHAT -> Kanal.NAV_NO
    JournalpostKanal.NAV_NO_UINNLOGGET -> Kanal.NAV_NO
    JournalpostKanal.SKAN_NETS -> Kanal.SKAN_NETS
    JournalpostKanal.LOKAL_UTSKRIFT -> Kanal.LOKAL_UTSKRIFT
    JournalpostKanal.SENTRAL_UTSKRIFT -> Kanal.SENTRAL_UTSKRIFT
    JournalpostKanal.SDP -> Kanal.SDP
    JournalpostKanal.INGEN_DISTRIBUSJON -> Kanal.INGEN_DISTRIBUSJON
    else -> null
}

enum class JournalpostUtsendingKanal {
    NAV_NO,
    NAV_NO_CHAT,
    L, // Lokal utskrift
    S, // Sentral utksrift
    INGEN_DISTRIBUSJON,
    UKJENT,
    ALTINN,
    EIA,
    EESSI,
    TRYGDERETTEN,
    HELSENETTET,
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EttersendingsoppgaveDo(
    val tittel: String,
    val skjemaId: String,
    val språk: String,
    val innsendingsId: String? = null,
    val innsendingsFristDager: Int,
    val fristDato: LocalDate? = null,
    val slettesDato: LocalDate? = null,
    val opprettetDato: LocalDate? = null,
    val vedleggsliste: List<EttersendingsoppgaveVedleggDo>,
) {
    private fun asJsonString(): String = toJsonString(this)
    fun toMap(): List<Map<String, String>> = asJsonString().chunked(100).mapIndexed { index, it ->
        mapOf(
            "nokkel" to "$ETTERSENDINGSOPPGAVE_KEY$index",
            "verdi" to it,
        )
    }

    fun toOpprettEttersendingsoppgaveDto() = OpprettEttersendingsppgaveDto(
        tittel = tittel,
        skjemaId = skjemaId,
        innsendingsFristDager = innsendingsFristDager,
        språk = when (språk) {
            "nb_NO" -> Språk.NB
            "nn_NO" -> Språk.NN
            "de_DE" -> Språk.DE
            "en_GB" -> Språk.EN
            "fr_FR" -> Språk.FR
            else -> Språk.values().find { it.name.lowercase() == språk.lowercase() } ?: Språk.NB
        },
        vedleggsliste = vedleggsliste.map {
            OpprettEttersendingsoppgaveVedleggDto(
                tittel = it.tittel,
                vedleggsnr = it.vedleggsnr,
            )
        },
    )
    fun toEttersendingsoppgaveDto() = EttersendingsppgaveDto(
        tittel = tittel,
        slettesDato = slettesDato,
        fristDato = fristDato,
        skjemaId = skjemaId,
        språk = språk,
        opprettetDato = opprettetDato,
        innsendingsId = innsendingsId,
        status = if (innsendingsId != null) EttersendingsoppgaveStatus.UKJENT else EttersendingsoppgaveStatus.IKKE_OPPRETTET,
        vedleggsliste = vedleggsliste.map {
            EttersendingsoppgaveVedleggDto(
                tittel = it.tittel,
                vedleggsnr = it.vedleggsnr,
            )
        },
    )

    data class EttersendingsoppgaveVedleggDo(
        val tittel: String? = null,
        val url: String? = null,
        val vedleggsnr: String,
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DistribuertTilAdresseDo(
    var adresselinje1: String?,
    var adresselinje2: String?,
    var adresselinje3: String?,
    var land: String,
    var postnummer: String?,
    var poststed: String?,
) {
    private fun asJsonString(): String = toJsonString(this)
    fun toMap(): List<Map<String, String>> = asJsonString().chunked(100).mapIndexed { index, it ->
        mapOf(
            "nokkel" to "$DISTRIBUERT_ADRESSE_KEY$index",
            "verdi" to it,
        )
    }

    fun toDistribuerTilAdresse(): DistribuerTilAdresse = DistribuerTilAdresse(
        adresselinje1 = adresselinje1,
        adresselinje2 = adresselinje2,
        adresselinje3 = adresselinje3,
        land = land,
        postnummer = postnummer,
        poststed = poststed,
    )

    fun toMottakerAdresse(): MottakerAdresseTo = MottakerAdresseTo(
        adresselinje1 = adresselinje1 ?: "",
        adresselinje2 = adresselinje2,
        adresselinje3 = adresselinje3,
        landkode = land,
        postnummer = postnummer,
        poststed = poststed,
    )
}

class TilleggsOpplysninger : MutableList<Map<String, String>> by mutableListOf() {

    private var REGEX_NOT_NUMBER = "\\D".toRegex()

    private fun extractIndexFromKey(keyWithIndex: String): Int {
        val indexStr = keyWithIndex.replace(REGEX_NOT_NUMBER, "")
        return try {
            Integer.parseInt(indexStr)
        } catch (e: NumberFormatException) {
            -1
        }
    }

    fun setDistribuertAvIdent(distribuertAvIdent: String) {
        this.removeAll { it["nokkel"]?.contains(DISTRIBUERT_AV_IDENT_KEY) ?: false }
        this.add(mapOf("nokkel" to DISTRIBUERT_AV_IDENT_KEY, "verdi" to distribuertAvIdent))
    }

    fun hentBestillingId(): String? = this.filter { it["nokkel"]?.contains(DOKDIST_BESTILLING_ID) ?: false }
        .filter { Strings.isNotEmpty(it["verdi"]) }
        .map { it["verdi"] }
        .firstOrNull()

    fun hentDistribuertAvIdent(): String? = this.filter { it["nokkel"]?.contains(DISTRIBUERT_AV_IDENT_KEY) ?: false }
        .filter { Strings.isNotEmpty(it["verdi"]) }
        .map { it["verdi"] }
        .firstOrNull()

    fun setJournalfortAvIdent(journalfortAvIdent: String) {
        this.removeAll { it["nokkel"]?.contains(JOURNALFORT_AV_IDENT_KEY) ?: false }
        this.add(mapOf("nokkel" to JOURNALFORT_AV_IDENT_KEY, "verdi" to journalfortAvIdent))
    }

    fun hentJournalfortAvIdent(): String? = this.filter { it["nokkel"]?.contains(JOURNALFORT_AV_IDENT_KEY) ?: false }
        .filter { Strings.isNotEmpty(it["verdi"]) }
        .map { it["verdi"] }
        .firstOrNull()

    fun hentJournalfortAv(): String? = this.filter { it["nokkel"]?.contains(JOURNALFORT_AV_KEY) ?: false }
        .filter { Strings.isNotEmpty(it["verdi"]) }
        .map { it["verdi"] }
        .firstOrNull()

    fun removeDistribusjonMetadata() {
        this.removeAll { it["nokkel"]?.contains(DISTRIBUSJON_BESTILT_KEY) ?: false }
        this.removeAll { it["nokkel"]?.contains(DISTRIBUERT_ADRESSE_KEY) ?: false }
        this.removeAll { it["nokkel"]?.contains(DOKDIST_BESTILLING_ID) ?: false }
        this.removeAll { it["nokkel"]?.contains(ORIGINALT_DISTRIBUERT_DIGITALT_KEY) ?: false }
    }

    fun setOriginalDistribuertDigitalt() {
        this.removeAll { it["nokkel"]?.contains(ORIGINALT_DISTRIBUERT_DIGITALT_KEY) ?: false }
        this.add(mapOf("nokkel" to ORIGINALT_DISTRIBUERT_DIGITALT_KEY, "verdi" to "true"))
    }

    fun isOriginalDistribuertDigitalt(): Boolean = this.any { it["nokkel"]?.contains(ORIGINALT_DISTRIBUERT_DIGITALT_KEY) ?: false }

    fun setDistribusjonBestillt() {
        this.removeAll { it["nokkel"]?.contains(DISTRIBUSJON_BESTILT_KEY) ?: false }
        this.add(mapOf("nokkel" to DISTRIBUSJON_BESTILT_KEY, "verdi" to "true"))
    }

    fun isDistribusjonBestilt(): Boolean = this.any { it["nokkel"]?.contains(DISTRIBUSJON_BESTILT_KEY) ?: false }

    fun setNyDistribusjonBestiltFlagg() {
        this.removeAll { it["nokkel"]?.contains(AVVIK_NY_DISTRIBUSJON_BESTILT_KEY) ?: false }
        this.add(mapOf("nokkel" to AVVIK_NY_DISTRIBUSJON_BESTILT_KEY, "verdi" to "true"))
    }

    fun isNyDistribusjonBestilt(): Boolean = this.filter { it["nokkel"]?.contains(AVVIK_NY_DISTRIBUSJON_BESTILT_KEY) ?: false }
        .any { it["verdi"] == "true" }

    fun setEndretTemaFlagg() {
        this.removeAll { it["nokkel"]?.contains(AVVIK_ENDRET_TEMA_KEY) ?: false }
        this.add(mapOf("nokkel" to AVVIK_ENDRET_TEMA_KEY, "verdi" to "true"))
    }

    fun setOriginalBestiltFlagg() {
        this.removeAll { it["nokkel"]?.contains(ORIGINAL_BESTILT_KEY) ?: false }
        this.add(mapOf("nokkel" to ORIGINAL_BESTILT_KEY, "verdi" to "true"))
    }

    fun isOriginalBestilt(): Boolean = this.filter { it["nokkel"]?.contains(ORIGINAL_BESTILT_KEY) ?: false }
        .any { it["verdi"] == "true" }

    fun removeEndretTemaFlagg() {
        this.removeAll { it["nokkel"]?.contains(AVVIK_ENDRET_TEMA_KEY) ?: false }
        this.add(mapOf("nokkel" to AVVIK_ENDRET_TEMA_KEY, "verdi" to "false"))
    }

    fun isEndretTema(): Boolean = this.filter { it["nokkel"]?.contains(AVVIK_ENDRET_TEMA_KEY) ?: false }
        .any { it["verdi"] == "true" }

    fun addInnsendingsOppgave(ettersendingsOppgave: EttersendingsoppgaveDo) {
        this.removeAll { it["nokkel"]?.contains(ETTERSENDINGSOPPGAVE_KEY) ?: false }
        this.addAll(ettersendingsOppgave.toMap())
    }

    fun addMottakerAdresse(adresseDo: DistribuertTilAdresseDo) {
        this.removeAll { it["nokkel"]?.contains(DISTRIBUERT_ADRESSE_KEY) ?: false }
        this.addAll(adresseDo.toMap())
    }

    fun leggTilSamhandlerId(samhandlerId: String) {
        this.removeAll { it["nokkel"]?.contains(SAMHANDLER_ID_KEY) ?: false }
        this.add(mapOf("nokkel" to SAMHANDLER_ID_KEY, "verdi" to samhandlerId))
    }
    fun hentInnsendingsoppgave(): EttersendingsoppgaveDo? {
        // Key format (DISTRIBUERT_ADRESSE_KEY)(index)
        val ettersendingsoppgaveMap =
            this.filter { it["nokkel"]?.contains(ETTERSENDINGSOPPGAVE_KEY) ?: false }
                .filter { Strings.isNotEmpty(it["verdi"]) }
                .sortedBy { extractIndexFromKey(it["nokkel"]!!) }

        if (ettersendingsoppgaveMap.isEmpty()) {
            return null
        }

        val jsonString = ettersendingsoppgaveMap.map { it["verdi"] }.joinToString("")
        return fromJsonString(jsonString)
    }
    fun hentSamhandlerId(): String? = this.filter { it["nokkel"]?.contains(SAMHANDLER_ID_KEY) ?: false }
        .filter { Strings.isNotEmpty(it["verdi"]) }
        .map { it["verdi"] }
        .firstOrNull()

    fun addReturDetaljLog(returDetaljerLogDO: ReturDetaljerLogDO) {
        this.addAll(returDetaljerLogDO.toMap())
    }

    fun unlockReturDetaljerLog(logDate: LocalDate) {
        val updatedTilleggsopplysninger = hentReturDetaljerLogDO().map {
            if (it.dato == logDate && it.locked == true) it.locked = false
            it
        }.flatMap { it.toMap() }
        this.removeAll { it["nokkel"]?.contains(RETUR_DETALJER_KEY) ?: false }
        this.addAll(updatedTilleggsopplysninger)
    }

    fun updateReturDetaljLog(originalDate: LocalDate, returDetaljerLogDO: ReturDetaljerLogDO) {
        val updatedTilleggsopplysninger =
            hentReturDetaljerLogDO().map { if (it.dato == originalDate && it.locked != true) returDetaljerLogDO else it }
                .flatMap { it.toMap() }
        this.removeAll { it["nokkel"]?.contains(RETUR_DETALJER_KEY) ?: false }
        this.addAll(updatedTilleggsopplysninger)
    }

    fun hentAdresseDo(): DistribuertTilAdresseDo? {
        // Key format (DISTRIBUERT_ADRESSE_KEY)(index)
        val adresseKeyValueMapList =
            this.filter { it["nokkel"]?.contains(DISTRIBUERT_ADRESSE_KEY) ?: false }
                .filter { Strings.isNotEmpty(it["verdi"]) }
                .sortedBy { extractIndexFromKey(it["nokkel"]!!) }

        if (adresseKeyValueMapList.isEmpty()) {
            return null
        }

        val adresseJsonString = adresseKeyValueMapList.map { it["verdi"] }.joinToString("")
        return fromJsonString(adresseJsonString)
    }

    fun lockAllReturDetaljerLog() {
        val updatedReturDetaljer = hentReturDetaljerLogDO().map {
            it.locked = true
            it
        }.flatMap { it.toMap() }
        this.removeAll { it["nokkel"]?.contains(RETUR_DETALJER_KEY) ?: false }
        this.addAll(updatedReturDetaljer)
    }

    fun hentReturDetaljerLogDO(): List<ReturDetaljerLogDO> {
        // Key format (RETUR_DETALJER_KEY)(index)_(date)
        val returDetaljer = this.filter { it["nokkel"]?.contains(RETUR_DETALJER_KEY) ?: false }
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
            val locked = it["nokkel"]?.startsWith("L") == true
            val existing = returDetaljerList.find { rd -> rd.dato == dato && rd.locked == locked }
            if (existing != null) {
                existing.beskrivelse = existing.beskrivelse + beskrivelse
            } else {
                returDetaljerList.add(ReturDetaljerLogDO(beskrivelse, dato!!, locked))
            }
        }
        return returDetaljerList
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReturDetaljerLogDO(
    var beskrivelse: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    var dato: LocalDate,
    var locked: Boolean? = false,
) {
    fun toMap(): List<Map<String, String>> = beskrivelse.chunked(100).mapIndexed { index, it ->
        mapOf(
            "nokkel" to "${if (locked == true) "L" else ""}$RETUR_DETALJER_KEY${index}_${
                DateUtils.formatDate(
                    dato,
                )
            }",
            "verdi" to it,
        )
    }
}

data class AvsenderMottaker(var navn: String? = null, var id: String? = null, var type: AvsenderMottakerIdType? = null)

enum class AvsenderMottakerIdType {
    FNR,
    UKJENT,
    ORGNR,
    HPRNR,
    UTL_ORG,
    NULL,
}

enum class BrukerType {
    AKTOERID,
    FNR,
}

data class Bruker(var id: String? = null, var type: String? = null) {
    fun tilAktorDto(): AktorDto = if (id != null) {
        AktorDto(
            id!!,
            runCatchingIgnoreException { type?.let { IdentType.valueOf(it) } } ?: IdentType.FNR,
        )
    } else {
        throw JournalpostDataException("ingen id i $this")
    }

    @JsonIgnore
    fun isAktoerId(): Boolean = this.type == BrukerType.AKTOERID.name
}

data class Dokument(var tittel: String? = null, var dokumentInfoId: String? = null, var brevkode: String? = null) {
    fun tilDokumentDto(journalposttype: String?): DokumentDto = DokumentDto(
        arkivSystem = DokumentArkivSystemDto.JOARK,
        dokumentmalId = brevkode,
        dokumentreferanse = this.dokumentInfoId,
        dokumentType = journalposttype,
        status = DokumentStatusDto.FERDIGSTILT,
        tittel = this.tittel,
    )
}

data class DatoType(var dato: String? = null, var datotype: String? = null) {
    fun somDato(): LocalDate {
        val datoStreng = dato?.substring(0, 10)

        return if (datoStreng != null) {
            LocalDate.parse(datoStreng)
        } else {
            throw JournalpostDataException(
                "Kunne ikke trekke ut dato fra: $dato",
            )
        }
    }

    fun somDatoTidspunkt(): LocalDateTime = if (dato != null) {
        LocalDateTime.parse(dato)
    } else {
        throw JournalpostDataException(
            "Kunne ikke trekke ut dato fra: $dato",
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Sak(var fagsakId: String? = null, var fagsakSystem: String? = null, var sakstype: String? = null, var tema: String? = null) {
    constructor(fagsakId: String?) : this(fagsakId, null, null, null)
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
    UKJENT,
}

data class EndreJournalpostCommandIntern(
    val endreJournalpostCommand: EndreJournalpostCommand,
    val enhet: String?,
    val endreAdresse: DistribuertTilAdresseDo?,
) {
    constructor(endreJournalpostCommand: EndreJournalpostCommand, enhet: String) : this(
        endreJournalpostCommand,
        enhet,
        null,
    )

    fun skalJournalfores() = endreJournalpostCommand.skalJournalfores
    fun hentAvsenderNavn(journalpost: Journalpost) = endreJournalpostCommand.avsenderNavn ?: journalpost.hentAvsenderNavn()

    fun harEnTilknyttetSak(): Boolean = endreJournalpostCommand.tilknyttSaker.isNotEmpty()
    fun harGjelder(): Boolean = endreJournalpostCommand.gjelder != null
    fun hentTilknyttetSak() = endreJournalpostCommand.tilknyttSaker.first()
    fun hentTilknyttetSaker() = endreJournalpostCommand.tilknyttSaker
    fun hentFagomrade() = endreJournalpostCommand.fagomrade
    fun hentGjelder() = endreJournalpostCommand.gjelder
    fun hentGjelderType() = if (endreJournalpostCommand.gjelderType != null) endreJournalpostCommand.gjelderType!! else IdentType.FNR

    fun sjekkGyldigEndring(journalpost: Journalpost) {
        val violations = mutableListOf<String>()

        if (skalJournalfores()) {
            if (!journalpost.isStatusMottatt()) {
                violations.add("Journalpost med journalstatus ${journalpost.journalstatus} kan ikke journalføres")
            }
            if (!journalpost.hasSak() && endreJournalpostCommand.tilknyttSaker.isEmpty()) {
                violations.add("Kan ikke registrere journalpost uten sak")
            }
            if (journalpost.bruker == null && endreJournalpostCommand.manglerGjelder()) {
                violations.add("Kan ikke registrere journalpost når det mangler gjelder")
            }
        } else if (journalpost.isStatusMottatt()) {
            if (endreJournalpostCommand.tilknyttSaker.size > 1) {
                violations.add("Kan ikke lagre journalpost med flere saker uten å journalføre")
            }
        } else if (journalpost.isUtgaaendeDokument()) {
            sjekkGyldigEndringAvReturDato(journalpost, violations)
        }

        if (journalpost.isNotat()) {
            if (endreJournalpostCommand.dokumentDato?.isAfter(LocalDate.now()) == true) {
                violations.add("Dokumentdato kan ikke endres til fram i tid")
            }
        }
        if (violations.isNotEmpty()) {
            throw ViolationException(violations)
        }
    }

    fun sjekkGyldigEndringAvReturDato(journalpost: Journalpost, violations: MutableList<String>) {
        val endreReturDetaljer =
            endreJournalpostCommand.endreReturDetaljer?.filter { Strings.isNotEmpty(it.beskrivelse) }
        if (!endreReturDetaljer.isNullOrEmpty()) {
            val kanEndreReturDetaljer = journalpost.isDistribusjonKommetIRetur()
            if (!kanEndreReturDetaljer) {
                violations.add("Kan ikke endre returdetaljer på journalpost som ikke har kommet i retur")
            }

            val skalLeggeTilNyReturDetalj = endreReturDetaljer.any { it.originalDato == null }
            val nyReturDatoErLikEllerEtterDokumentDato =
                endreReturDetaljer.any { it.originalDato == null && it.nyDato?.isBefore(journalpost.hentDatoDokument()) == false }
            if (skalLeggeTilNyReturDetalj && !nyReturDatoErLikEllerEtterDokumentDato) {
                violations.add("Kan ikke opprette ny returdetalj med returdato før dokumentdato")
            }

            val kanLeggeTilNyReturDetalj = journalpost.manglerReturDetaljForSisteRetur()
            val erGyldigOpprettelseAvNyReturDetalj =
                (kanLeggeTilNyReturDetalj || !skalLeggeTilNyReturDetalj)
            if (!erGyldigOpprettelseAvNyReturDetalj) {
                violations.add("Kan ikke opprette ny returdetalj (originalDato=null)")
            }

            val endringAvLaastReturDetalj =
                endreReturDetaljer.any {
                    it.originalDato != null &&
                        journalpost.hasLockedReturDetaljerWithDate(
                            it.originalDato!!,
                        )
                }
            val opprettelseAvEksisterendeReturDato =
                endreReturDetaljer.any {
                    it.originalDato == null &&
                        journalpost.hasLockedReturDetaljerWithDate(
                            it.nyDato!!,
                        )
                }
            if (endringAvLaastReturDetalj || opprettelseAvEksisterendeReturDato) {
                violations.add("Kan ikke endre låste returdetaljer")
            }

            val oppdatertReturDatoErEtterDagensDato =
                endreReturDetaljer.any { it.nyDato?.isAfter(LocalDate.now()) == true }
            if (oppdatertReturDatoErEtterDagensDato) {
                violations.add("Kan ikke oppdatere returdato til etter dagens dato")
            }
            // Ved bestilling av ny distribusjon opprettes det ny journalpost. Da vil dokumentdato være lik siste utsendt dato og ikke første
            val harEndretDatoPaaReturDetaljerFoerDokumentDato =
                endreReturDetaljer.any { it.originalDato?.isBefore(journalpost.hentDatoDokument()) == true && it.originalDato != it.nyDato }
            if (harEndretDatoPaaReturDetaljerFoerDokumentDato) {
                violations.add("Kan ikke endre returdetaljer opprettet før dokumentdato")
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TilknyttetJournalpost(
    var journalpostId: Long,
    var journalstatus: JournalStatus,
    var sak: Sak?,
    var dokumenter: List<TilknyttetDokument> = emptyList(),
) {
    fun isNotFeilregistrert() = journalstatus != JournalStatus.FEILREGISTRERT
    data class TilknyttetDokument(val tittel: String? = null)

    val tittel get() = dokumenter.firstOrNull()?.tittel
}

fun returDetaljerDOListDoToMap(returDetaljerLog: List<ReturDetaljerLogDO>): Map<String, String> = mapOf(
    "nokkel" to RETUR_DETALJER_KEY,
    "verdi" to jacksonObjectMapper().registerModule(JavaTimeModule())
        .writeValueAsString(returDetaljerLog),
)

enum class JournalpostType(var dekode: String) {
    N("Notat"),
    I("Inngående dokument"),
    U("Utgående dokument"),
}

fun DokumentSoknadDto.tilEttersendingsoppgaveDto() = EttersendingsppgaveDto(
    tittel = tittel,
    skjemaId = skjemanr,
    innsendingsId = innsendingsId,
    språk = spraak ?: Språk.NB.name,
    opprettetDato = opprettetDato.toLocalDate(),
    fristDato = innsendingsFristDato?.toLocalDate(),
    slettesDato = skalSlettesDato?.toLocalDate(),
    vedleggsliste = vedleggsListe.map {
        EttersendingsoppgaveVedleggDto(
            tittel = it.tittel,
            vedleggsnr = it.vedleggsnr ?: "",
            status = EttersingdsoppgaveOpplastingsStatus.values().find { s -> s.name == it.opplastingsStatus.name } ?: EttersingdsoppgaveOpplastingsStatus.UKJENT,
        )
    },
    status = EttersendingsoppgaveStatus.values().find { it.name == status.name } ?: EttersendingsoppgaveStatus.UKJENT,
)
