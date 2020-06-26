package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.dto.AktorDto
import no.nav.bidrag.dokument.dto.DokumentDto
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
                journalpostId = "JOARK-" + journalpostId,
                journalstatus = journalstatus,
                mottattDato = hentDatoRegistrert()
        )
    }

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

    fun erTilknyttetSak(saksnummer: String) = sak?.fagsakId == saksnummer

    fun hentAvsenderNavn() = avsenderMottaker?.navn
}

data class AvsenderMottaker(
        var navn: String? = null
)

data class Bruker(
        var id: String? = null,
        var type: String? = null
) {
    fun tilAktorDto(): AktorDto {
        return if (id != null) AktorDto(id!!) else throw IllegalStateException("ingne id i $this")
    }
}

data class Dokument(
        var tittel: String? = null
) {
    fun tilDokumentDto(journalposttype: String?): DokumentDto = DokumentDto(
            dokumentType = journalposttype,
            tittel = this.tittel
    )
}

data class DatoType(
        var dato: String? = null,
        var datotype: String? = null
) {
    fun somDato(): LocalDate {
        val datoStreng = dato?.substring(0, 10)

        if (datoStreng != null) {
            return LocalDate.parse(datoStreng)
        }

        throw IllegalStateException("Kunne ikke trekke ut dato fra: " + dato)
    }
}

data class Sak(
        var fagsakId: String? = null
)
