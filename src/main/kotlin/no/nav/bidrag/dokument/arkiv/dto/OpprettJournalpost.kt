package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.apache.commons.lang3.Validate

@JsonIgnoreProperties(ignoreUnknown = true, value = ["journalpostId"])
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpprettJournalpostRequest(
    var sak: OpprettJournalpostSak? = null,
    var tittel: String? = null,
    var journalfoerendeEnhet: String? = null,
    var journalpostType: String? = null,
    var datoRetur: String? = null,
    var behandlingstema: String? = null,
    var eksternReferanseId: String? = null,
    var tilleggsopplysninger: TilleggsOpplysninger = TilleggsOpplysninger(),
    var tema: String? = null,
    var kanal: String? = null,
    var datoMottatt: String? = null,
    var bruker: OpprettJournalpostBruker? = null,
    var dokumenter: List<Dokument> = emptyList(),
    var avsenderMottaker: OpprettJournalpostAvsenderMottaker? = null
) {

    constructor(journalpost: Journalpost, dokumenterByte: Map<String, ByteArray>): this() {
        sak = OpprettJournalpostSak(journalpost.sak?.fagsakId)
        tema = journalpost.tema
        journalfoerendeEnhet = journalpost.journalforendeEnhet
        journalpostType = when(journalpost.journalposttype){
            JournalpostType.U -> "UTGAAENDE"
            JournalpostType.I -> "INNGAAENDE"
            JournalpostType.N -> "NOTAT"
            else -> "UTGAAENDE"
        }
        behandlingstema = journalpost.behandlingstema
        tittel = journalpost.tittel
        avsenderMottaker = OpprettJournalpostAvsenderMottaker(journalpost.avsenderMottaker?.navn, journalpost.avsenderMottaker?.id, journalpost.avsenderMottaker?.type)
        bruker = OpprettJournalpostBruker(journalpost.bruker?.id, journalpost.bruker?.type)
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

    @Suppress("unused") // properties used by jackson
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class OpprettJournalpostSak(val fagsakId: String? = null) {
        val fagsaksystem = if (fagsakId == null) null else Fagsaksystem.BISYS
        val sakstype = if (fagsakId === null) null else Sakstype.FAGSAK
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class OpprettJournalpostAvsenderMottaker(val navn: String? = null, val id: String? = null, val idType: AvsenderMottakerIdType?)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class OpprettJournalpostBruker(val id: String? = null, val idType: String? = null)

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

fun validerJournalpostKanDupliseres(journalpost: Journalpost){
    Validate.isTrue(journalpost.tema == "BID" || journalpost.tema == "FAR", "Journalpost må ha tema BID/FAR")
    Validate.isTrue(journalpost.isUtgaaendeDokument(), "Journalpost må være utgående dokument")
    Validate.isTrue(journalpost.hasMottakerId(), "Journalpost må ha satt mottakerId")
    Validate.isTrue(journalpost.hasSak(), "Journalpost må ha sak")
    Validate.isTrue(journalpost.bruker?.id != null, "Journalpost må ha satt brukerid")
}