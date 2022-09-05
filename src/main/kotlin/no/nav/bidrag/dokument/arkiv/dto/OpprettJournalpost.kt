package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.apache.commons.lang3.Validate

@JsonIgnoreProperties(ignoreUnknown = true, value = ["journalpostId"])
@JsonInclude(JsonInclude.Include.NON_NULL)
data class JoarkOpprettJournalpostRequest(
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
    var dokumenter: MutableList<Dokument> = mutableListOf(),
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
        dokumenter = journalpost.dokumenter.filter{ dokumenterByte[it.dokumentInfoId] != null }.map {
            Dokument(
                brevkode = it.brevkode,
                tittel = it.tittel,
                dokumentvarianter = opprettDokumentVariant("${journalpost.journalpostId}_${it.dokumentInfoId}", dokumenterByte[it.dokumentInfoId]!!)
            )
        } as MutableList<Dokument>
    }

    fun addDokument(dokument: Dokument){
        this.dokumenter.add(dokument)
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

fun opprettDokumentVariant(filnavn: String?, dokumentByte: ByteArray): List<JoarkOpprettJournalpostRequest.DokumentVariant>{
    return listOf(JoarkOpprettJournalpostRequest.DokumentVariant(
        variantformat = "ARKIV",
        filtype = "PDFA",
        fysiskDokument = dokumentByte,
        filnavn = if (filnavn != null) "${filnavn}.pdf" else null
    ))
}

data class JoarkOpprettJournalpostResponse(
    var journalpostId: Long? = null,
    val journalstatus: String? = null,
    val melding: String? = null,
    val journalpostferdigstilt: Boolean? = null,
    val dokumenter: List<DokumentInfo>? = emptyList()
)


data class OpprettDokument(
    var dokumentInfoId: String?,
    var dokument: ByteArray?,
    var tittel: String?,
    var brevkode: String?
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