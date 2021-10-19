package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.arkiv.model.JournalpostDataException
import no.nav.bidrag.dokument.dto.AktorDto
import no.nav.bidrag.dokument.dto.AvvikType
import no.nav.bidrag.dokument.dto.DokumentDto
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand
import no.nav.bidrag.dokument.dto.JournalpostDto
import no.nav.bidrag.dokument.dto.JournalpostResponse
import java.time.LocalDate
import java.util.stream.Collectors.toList

private const val DATO_DOKUMENT = "DATO_DOKUMENT"
private const val DATO_JOURNALFORT = "DATO_JOURNALFOERT"
private const val DATO_REGISTRERT = "DATO_REGISTRERT"

data class Journalpost(
    var avsenderMottaker: AvsenderMottaker? = null,
    var bruker: Bruker? = null,
    var dokumenter: List<Dokument> = emptyList(),
    var journalforendeEnhet: String? = null,
    var journalfortAvNavn: String? = null,
    var journalpostId: String? = null,
    var journalposttype: String? = null,
    var journalstatus: String? = null,
    var relevanteDatoer: List<DatoType> = emptyList(),
    var sak: Sak? = null,
    var tema: String? = null,
    var tittel: String? = null
) {

    fun hentStatus(): String? {
        return when(journalstatus){
            "MOTTATT"->"M"
            else -> journalstatus
        }
    }
    fun hentJournalpostIdLong() = journalpostId?.toLong()
    fun hentJournalpostIdMedPrefix() = "JOARK-"+journalpostId
    fun hentDatoJournalfort(): LocalDate? {
        val journalfort = relevanteDatoer
            .find { it.datotype == DATO_JOURNALFORT }

        return journalfort?.somDato()
    }

    fun hentDatoRegistrert(): LocalDate? {
        val registrert = relevanteDatoer
            .find { it.datotype == DATO_REGISTRERT }

        return registrert?.somDato()
    }

    fun tilJournalpostDto(): JournalpostDto {

        @Suppress("UNCHECKED_CAST")
        return JournalpostDto(
            avsenderNavn = avsenderMottaker?.navn,
            dokumenter = dokumenter.stream().map { dok -> dok?.tilDokumentDto(journalposttype) }.collect(toList()) as List<DokumentDto>,
            dokumentDato = hentDokumentDato(),
            dokumentType = journalposttype,
            fagomrade = tema,
            gjelderAktor = bruker?.tilAktorDto(),
            innhold = tittel,
            journalfortDato = hentDatoJournalfort(),
            journalforendeEnhet = journalforendeEnhet,
            journalfortAv = journalfortAvNavn,
            journalpostId = "JOARK-$journalpostId",
            journalstatus = journalstatus,
            mottattDato = hentDatoRegistrert()
        )
    }

    fun tilAvvik(): List<AvvikType> {
        val avvikTypeList = mutableListOf<AvvikType>()
        if (isStatusMottatt() && isInngaaendeDokument()) avvikTypeList.add(AvvikType.OVERFOR_TIL_ANNEN_ENHET)
//        if (isInngaaendeDokument()) avvikTypeList.add(AvvikType.INNG_TIL_UTG_DOKUMENT)
//        if (isStatusMottatt()) avvikTypeList.add(AvvikType.TREKK_JOURNALPOST)
        if (!isStatusMottatt() && hasSak() && !isStatusFeilregistrert()) avvikTypeList.add(AvvikType.FEILFORE_SAK)
        avvikTypeList.add(AvvikType.ENDRE_FAGOMRADE)
        return avvikTypeList;
    }

    fun hasSak(): Boolean = sak != null
    fun isStatusFeilregistrert(): Boolean = journalstatus == "FEILREGISTRERT"
    fun isStatusMottatt(): Boolean = journalstatus == "MOTTATT"
    fun isInngaaendeDokument(): Boolean = journalposttype == "I"

    fun tilJournalpostResponse(): JournalpostResponse {
        val journalpost = tilJournalpostDto()
        val saksnummer = sak?.fagsakId

        return JournalpostResponse(journalpost, if (saksnummer != null) listOf(saksnummer) else emptyList())
    }

    private fun hentDokumentDato(): LocalDate? {
        val registrert = relevanteDatoer
            .find { it.datotype == DATO_DOKUMENT }

        return registrert?.somDato()
    }

    private fun erTilknyttetSak(saksnummer: String?) = sak?.fagsakId == saksnummer
    fun hentAvsenderNavn() = avsenderMottaker?.navn
    fun erIkkeTilknyttetSakNarOppgitt(saksnummer: String?) = if (saksnummer == null) false else !erTilknyttetSak(saksnummer)
}

data class AvsenderMottaker(
    var navn: String? = null
)

data class Bruker(
    var id: String? = null,
    var type: String? = null
) {
    fun tilAktorDto(): AktorDto {
        return if (id != null) AktorDto(id!!, type ?: "FNR") else throw JournalpostDataException("ingen id i $this")
    }
    fun isAktoerId(): Boolean {
        return this.type == "AKTOERID"
    }
}

data class Dokument(
    var tittel: String? = null,
    var dokumentInfoId: String? = null
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

data class Sak(
    var fagsakId: String? = null
)

data class EndreJournalpostCommandIntern(
    val endreJournalpostCommand: EndreJournalpostCommand,
    val enhet: String
) {
    fun skalJournalfores() = endreJournalpostCommand.skalJournalfores
    fun hentAvsenderNavn(journalpost: Journalpost) = endreJournalpostCommand.avsenderNavn ?: journalpost.hentAvsenderNavn()
    fun harEnTilknyttetSak(): Boolean {
        if (endreJournalpostCommand.tilknyttSaker.size > 1)
            throw JournalpostDataException("joark støtter bare en sak per journalpost")

        return endreJournalpostCommand.tilknyttSaker.size == 1
    }

    fun hentTilknyttetSak() = endreJournalpostCommand.tilknyttSaker.first()
    fun hentFagomrade() = endreJournalpostCommand.fagomrade
    fun hentGjelder() = endreJournalpostCommand.gjelder
    fun hentGjelderType() = if (endreJournalpostCommand.gjelderType != null) endreJournalpostCommand.gjelderType!! else "FNR"
}
