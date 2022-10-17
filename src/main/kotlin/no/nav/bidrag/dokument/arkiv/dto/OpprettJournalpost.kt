package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.apache.commons.lang3.Validate

typealias DokumentId = String
typealias DokumentByte = Map<DokumentId, ByteArray>
@JsonIgnoreProperties(ignoreUnknown = true, value = ["journalpostId"])
@JsonInclude(JsonInclude.Include.NON_NULL)
open class JoarkOpprettJournalpostRequest(
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
        @JsonIgnore
        val dokumentInfoId: String? = null,
        var tittel: String? = null,
        val brevkode: String? = null,
        var dokumentvarianter: List<DokumentVariant> = emptyList()
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

class OpprettJournalpost: JoarkOpprettJournalpostRequest(){
    @JsonIgnore
    var originalJournalpostId: Long? = null

    fun hasSak(): Boolean {
        return sak?.fagsakId != null
    }

    fun hasAvsenderMottaker(): Boolean {
        return avsenderMottaker?.navn?.isNotEmpty() == true || avsenderMottaker?.id?.isNotEmpty() == true
    }
    fun kopierFra(journalpost: Journalpost): OpprettJournalpost{
        dupliser(journalpost, emptyMap())
        return this;
    }
    fun dupliser(journalpost: Journalpost, dokumentByte: DokumentByte = emptyMap()): OpprettJournalpost{
        originalJournalpostId = journalpost.hentJournalpostIdLong()
        sak = OpprettJournalpostSak(journalpost.sak?.fagsakId)
        tema = "BID"
        journalfoerendeEnhet = journalpost.journalforendeEnhet
        journalpostType = when(journalpost.journalposttype){
            JournalpostType.U -> "UTGAAENDE"
            JournalpostType.I -> "INNGAAENDE"
            JournalpostType.N -> "NOTAT"
            else -> "UTGAAENDE"
        }
        kanal = journalpost.kanal?.name
        behandlingstema = journalpost.behandlingstema
        tittel = journalpost.hentTittel()
        avsenderMottaker = OpprettJournalpostAvsenderMottaker(journalpost.avsenderMottaker?.navn, journalpost.avsenderMottaker?.id, journalpost.avsenderMottaker?.type)
        bruker = OpprettJournalpostBruker(journalpost.bruker?.id, journalpost.bruker?.type)
        tilleggsopplysninger = journalpost.tilleggsopplysninger
        dokumenter = journalpost.dokumenter.filter{ dokumentByte[it.dokumentInfoId] != null }.map {
            Dokument(
                brevkode = it.brevkode,
                tittel = it.tittel,
                dokumentvarianter = listOf(opprettDokumentVariant("${journalpost.journalpostId}_${it.dokumentInfoId}", dokumentByte[it.dokumentInfoId]!!))
            )
        } as MutableList<Dokument>
        return this
    }
    fun medDokument(dokumentInfoId: String?, dokument: ByteArray?, tittel: String?, brevkode: String?): OpprettJournalpost{
        dokumenter.add(
            Dokument(
                dokumentInfoId = dokumentInfoId,
                brevkode = brevkode,
                tittel = tittel,
                dokumentvarianter = if (dokument!=null) listOf(opprettDokumentVariant(null, dokument)) else emptyList()
            )
        )
        return this
    }

    fun medJournalforendeEnhet(enhet: String): OpprettJournalpost{
        journalfoerendeEnhet = enhet
        return this
    }

    fun medKanal(jpKanal: JournalpostKanal?): OpprettJournalpost{
        kanal = jpKanal?.name
        return this
    }

    fun medSak(saksnummer: String): OpprettJournalpost{
        sak = OpprettJournalpostSak(saksnummer)
        return this
    }

    fun medTittel(oppdaterTittel: String): OpprettJournalpost{
        tittel = oppdaterTittel
        if (dokumenter.size > 0){
            dokumenter[0].tittel = oppdaterTittel
        }
        return this
    }

    fun medTema(oppdaterTema: String): OpprettJournalpost{
        tema = oppdaterTema
        return this
    }

    fun medEksternReferanseId(eksternReferanseId: String): OpprettJournalpost{
        this.eksternReferanseId = eksternReferanseId
        return this
    }

}

fun opprettDokumentVariant(filnavn: String?, dokumentByte: ByteArray): JoarkOpprettJournalpostRequest.DokumentVariant{
    return JoarkOpprettJournalpostRequest.DokumentVariant(
        variantformat = "ARKIV",
        filtype = "PDFA",
        fysiskDokument = dokumentByte,
        filnavn = if (filnavn != null) "${filnavn}.pdf" else null
    )
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

fun validerKanOppretteJournalpost(opprettJournalpost: OpprettJournalpost){
    Validate.isTrue(opprettJournalpost.tema == "BID" || opprettJournalpost.tema == "FAR", "Journalpost må ha tema BID/FAR")
    Validate.isTrue(opprettJournalpost.hasAvsenderMottaker(), "Journalpost må ha satt avsender/mottaker")
    Validate.isTrue(opprettJournalpost.hasSak(), "Journalpost må ha sak")
    Validate.isTrue(opprettJournalpost.bruker?.id != null, "Journalpost må ha satt brukerid")
    opprettJournalpost.dokumenter.forEach{
        Validate.isTrue(it.dokumentvarianter.isNotEmpty(), "Dokument \"${it.dokumentInfoId?:it.tittel}\" må minst ha en dokumentvariant")
        Validate.isTrue(it.tittel != null, "Alle dokumenter må ha tittel")
    }
}

fun validerUtgaaendeJournalpostKanDupliseres(journalpost: Journalpost){
    Validate.isTrue(journalpost.tema == "BID" || journalpost.tema == "FAR", "Journalpost må ha tema BID/FAR")
    Validate.isTrue(journalpost.isUtgaaendeDokument(), "Journalpost må være utgående dokument")
    Validate.isTrue(journalpost.hasMottakerId(), "Journalpost må ha satt mottakerId")
    Validate.isTrue(journalpost.hasSak(), "Journalpost må ha sak")
    Validate.isTrue(journalpost.bruker?.id != null, "Journalpost må ha satt brukerid")
}