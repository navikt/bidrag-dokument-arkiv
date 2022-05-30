package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true, value = ["journalpostId"])
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpprettJournalpostRequest(
    var sak: Sak? = null,
    var tittel: String? = null,
    var journalfoerendeEnhet: String? = null,
    var journalpostType: JournalpostType? = null,
    var datoRetur: String? = null,
    var behandlingstema: String? = null,
    var eksternReferanseId: String? = null,
    var tilleggsopplysninger: TilleggsOpplysninger = TilleggsOpplysninger(),
    var tema: String? = null,
    var kanal: String? = null,
    var datoMottatt: String? = null,
    var bruker: Bruker? = null,
    var dokumenter: List<Dokument> = emptyList(),
    var avsenderMottaker: AvsenderMottaker? = null
) {

    constructor(journalpost: Journalpost, dokumenterByte: Map<String, ByteArray>): this() {
        sak = journalpost.sak
        tema = journalpost.tema
        journalfoerendeEnhet = journalpost.journalforendeEnhet
        journalpostType = journalpost.journalposttype
        behandlingstema = journalpost.behandlingstema
        tittel = journalpost.tittel
        avsenderMottaker = journalpost.avsenderMottaker
        bruker = journalpost.bruker
        tilleggsopplysninger = journalpost.tilleggsopplysninger
        dokumenter = journalpost.dokumenter.map {
            Dokument(
                brevkode = it.brevkode,
                tittel = it.tittel,
                dokumentvarianter = listOf(
                    DokumentVariant(
                        variantformat = "ARKIV",
                        filtype = "PDFA",
                        fysiskDokument = dokumenterByte[it.dokumentInfoId]!!,
                        filnavn = "${journalpost.journalpostId}_${it.dokumentInfoId}.pdf"
                    ))
            )
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Dokument(
        val dokumentInfoId: String? = null,
        val dokumentKategori: String? = null,
        val tittel: String? = null,
        val brevkode: String? = null,
        val dokumentvarianter: List<DokumentVariant>? = emptyList()
    )

    @Suppress("unused") // properties used by jackson
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class DokumentVariant(
        val filtype: String? = null,
        val variantformat: String? = null,
        val fysiskDokument: ByteArray,
        val filnavn: String? = null
    )
}

data class OpprettJournalpostResponse(
    var journalpostId: Long? = null,
    val journalstatus: String? = null,
    val melding: String? = null,
    val journalpostferdigstilt: Boolean? = null,
    val dokumenter: List<DokumentInfo>? = emptyList()
)

data class DokumentInfo(
    val dokumentInfoId: String?
)