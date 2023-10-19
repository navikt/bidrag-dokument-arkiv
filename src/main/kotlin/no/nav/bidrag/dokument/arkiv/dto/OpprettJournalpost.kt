package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest
import org.apache.commons.lang3.Validate

typealias DokumentId = String
typealias DokumentByte = Map<DokumentId, ByteArray>

@JsonIgnoreProperties(ignoreUnknown = true, value = ["journalpostId"])
@JsonInclude(JsonInclude.Include.NON_NULL)
data class JoarkOpprettJournalpostRequest(
    val sak: OpprettJournalpostSak? = null,
    val tittel: String? = null,
    val datoDokument: String? = null,
    val journalfoerendeEnhet: String? = null,
    val journalpostType: JoarkJournalpostType? = null,
    val datoRetur: String? = null,
    val behandlingstema: String? = null,
    val eksternReferanseId: String? = null,
    val tilleggsopplysninger: TilleggsOpplysninger = TilleggsOpplysninger(),
    val tema: String? = null,
    val kanal: String? = null,
    val datoMottatt: String? = null,
    val bruker: OpprettJournalpostBruker? = null,
    val dokumenter: List<Dokument> = listOf(),
    val avsenderMottaker: OpprettJournalpostAvsenderMottaker? = null
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
    data class OpprettJournalpostAvsenderMottaker(
        val navn: String? = null,
        val id: String? = null,
        val idType: AvsenderMottakerIdType? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class OpprettJournalpostBruker(val id: String? = null, val idType: String?)

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Dokument(
        @JsonIgnore
        val dokumentInfoId: String? = null,
        val tittel: String? = null,
        val brevkode: String? = null,
        val dokumentvarianter: List<DokumentVariant> = emptyList()
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

@DslMarker
annotation class OpprettJournalpostRequestBuilderDsl

@OpprettJournalpostRequestBuilderDsl
object med

@OpprettJournalpostRequestBuilderDsl
object fjern
class OpprettJournalpostRequestBuilder {
    private var fjernDistribusjonMetadata: Boolean = false
    private var eksternReferanseId: String? = null
    private var journalførendeenhet: String? = null
    private var tittel: String? = null
    private var tema: String? = null
    private var fjernSak: Boolean = false
    private var fjernJournalførendeEnhet: Boolean = false
    private var dokumenter: MutableList<JoarkOpprettJournalpostRequest.Dokument> = mutableListOf()

    @OpprettJournalpostRequestBuilderDsl
    infix fun fjern.distribusjonMetadata(value: Boolean) {
        fjernDistribusjonMetadata = value
    }

    @OpprettJournalpostRequestBuilderDsl
    infix fun med.tema(_tema: String?) {
        tema = _tema
    }

    @OpprettJournalpostRequestBuilderDsl
    infix fun fjern.sakstilknytning(value: Boolean) {
        fjernSak = value
    }

    @OpprettJournalpostRequestBuilderDsl
    infix fun fjern.journalførendeenhet(value: Boolean) {
        fjernJournalførendeEnhet = value
    }

    @OpprettJournalpostRequestBuilderDsl
    infix fun med.journalførendeenhet(jfrEnhet: String?) {
        journalførendeenhet = jfrEnhet
    }

    @OpprettJournalpostRequestBuilderDsl
    infix fun med.dokumenter(_dokumenter: List<Dokument>) {
        dokumenter = _dokumenter.map {
            JoarkOpprettJournalpostRequest.Dokument(
                tittel = it.tittel,
                brevkode = it.brevkode,
                dokumentInfoId = it.dokumentInfoId
            )
        }.toMutableList()
    }

    @OpprettJournalpostRequestBuilderDsl
    infix fun med.eksternReferanseId(_eksternReferanseId: String?) {
        eksternReferanseId = _eksternReferanseId
    }

    @OpprettJournalpostRequestBuilderDsl
    infix fun med.tittel(_tittel: String?) {
        tittel = _tittel
    }

    @OpprettJournalpostRequestBuilderDsl
    operator fun JoarkOpprettJournalpostRequest.Dokument.unaryPlus() {
        dokumenter.add(this)
    }

    internal fun build(journalpost: Journalpost): JoarkOpprettJournalpostRequest {
        val avsenderType =
            if (journalpost.avsenderMottaker?.type == AvsenderMottakerIdType.NULL) null else journalpost.avsenderMottaker?.type
        return JoarkOpprettJournalpostRequest(
            sak = if (fjernSak || journalpost.sak?.fagsakId.isNullOrEmpty()) {
                null
            } else {
                JoarkOpprettJournalpostRequest.OpprettJournalpostSak(
                    journalpost.sak?.fagsakId
                )
            },
            tema = tema ?: "BID",
            journalfoerendeEnhet = if (fjernJournalførendeEnhet) {
                null
            } else {
                journalførendeenhet
                    ?: journalpost.journalforendeEnhet
            },
            journalpostType = when (journalpost.journalposttype) {
                JournalpostType.U -> JoarkJournalpostType.UTGAAENDE
                JournalpostType.I -> JoarkJournalpostType.INNGAAENDE
                JournalpostType.N -> JoarkJournalpostType.NOTAT
                else -> JoarkJournalpostType.UTGAAENDE
            },
            kanal = if (fjernDistribusjonMetadata) {
                null
            } else if (journalpost.isNotat()) {
                null
            } else {
                when (journalpost.kanal?.name) {
                    JournalpostKanal.SENTRAL_UTSKRIFT.name -> "S"
                    JournalpostKanal.LOKAL_UTSKRIFT.name -> "L"
                    JournalpostKanal.UKJENT.name -> null
                    else -> journalpost.kanal?.name
                }
            },
            behandlingstema = journalpost.behandlingstema,
            eksternReferanseId = eksternReferanseId,
            tittel = tittel ?: journalpost.hentTittel(),
            avsenderMottaker = JoarkOpprettJournalpostRequest.OpprettJournalpostAvsenderMottaker(
                journalpost.avsenderMottaker?.navn,
                journalpost.avsenderMottaker?.id,
                avsenderType
            ),
            bruker = JoarkOpprettJournalpostRequest.OpprettJournalpostBruker(
                journalpost.bruker?.id,
                journalpost.bruker?.type
            ),
            tilleggsopplysninger = if (fjernDistribusjonMetadata) {
                val tillegssopplysninger = TilleggsOpplysninger()
                tillegssopplysninger.addAll(journalpost.tilleggsopplysninger)
                tillegssopplysninger.removeDistribusjonMetadata()
                tillegssopplysninger.lockAllReturDetaljerLog()
                tillegssopplysninger
            } else {
                journalpost.tilleggsopplysninger
            },
            dokumenter = dokumenter.mapIndexed { i, it ->
                it.copy(tittel = if (i == 0) tittel ?: it.tittel else it.tittel)
            },
            datoMottatt = if (journalpost.isInngaaendeDokument()) {
                journalpost.hentDatoRegistrert()?.toString() ?: journalpost.hentDatoDokument()
                    ?.toString()
            } else {
                null
            }
        )
    }
}

@OpprettJournalpostRequestBuilderDsl
fun dupliserJournalpost(
    journalpost: Journalpost,
    setup: OpprettJournalpostRequestBuilder.() -> Unit
): JoarkOpprettJournalpostRequest {
    val opprettJournalpostBuilder = OpprettJournalpostRequestBuilder()
    opprettJournalpostBuilder.setup()
    return opprettJournalpostBuilder.build(journalpost)
}

fun opprettDokumentVariant(
    filnavn: String? = null,
    dokumentByte: ByteArray
): JoarkOpprettJournalpostRequest.DokumentVariant {
    return JoarkOpprettJournalpostRequest.DokumentVariant(
        variantformat = "ARKIV",
        filtype = "PDFA",
        fysiskDokument = dokumentByte,
        filnavn = if (filnavn != null) "$filnavn.pdf" else null
    )
}

data class JoarkOpprettJournalpostResponse(
    var journalpostId: Long? = null,
    val journalstatus: String? = null,
    val melding: String? = null,
    val journalpostferdigstilt: Boolean = false,
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

enum class JoarkJournalpostType {
    INNGAAENDE,
    UTGAAENDE,
    NOTAT
}

enum class JoarkMottakUtsendingKanal {
    NAV_NO,
    SKAN_IM,
    SKAN_BID,
    S, // Sentral print
    L, // Lokal print
    INGEN_DISTRIBUSJON
}

fun validerKanOppretteJournalpost(
    opprettJournalpost: JoarkOpprettJournalpostRequest,
    skalFerdigstilles: Boolean = false
) {
    Validate.isTrue(opprettJournalpost.journalpostType != null, "Journalposttype må settes")
    Validate.isTrue(opprettJournalpost.bruker?.id != null, "Journalpost må ha satt brukerid")
    opprettJournalpost.dokumenter.forEach {
        Validate.isTrue(
            it.dokumentvarianter.isNotEmpty(),
            "Dokument \"${it.dokumentInfoId ?: it.tittel}\" må minst ha en dokumentvariant"
        )
        Validate.isTrue(!it.tittel.isNullOrEmpty(), "Alle dokumenter må ha tittel")
    }

    if (opprettJournalpost.journalpostType != JoarkJournalpostType.NOTAT) {
        Validate.isTrue(
            opprettJournalpost.hasAvsenderMottaker(),
            "Journalpost må ha satt avsender/mottaker"
        )
    }

    if (opprettJournalpost.journalpostType == JoarkJournalpostType.NOTAT) {
        Validate.isTrue(opprettJournalpost.kanal == null, "Kanal skal ikke settes for notater")
    }

    if (opprettJournalpost.journalpostType == JoarkJournalpostType.INNGAAENDE) {
        Validate.isTrue(
            opprettJournalpost.kanal != null,
            "Kanal må settes for inngående journalpost"
        )
    }

    if (skalFerdigstilles) {
        Validate.isTrue(
            opprettJournalpost.tema == "BID" || opprettJournalpost.tema == "FAR",
            "Journalpost som skal ferdigstilles må ha tema BID/FAR"
        )
        Validate.isTrue(
            !opprettJournalpost.journalfoerendeEnhet.isNullOrEmpty(),
            "Journalpost som skal ferdigstilles må ha satt journalførendeEnhet"
        )
        Validate.isTrue(
            opprettJournalpost.hasSak(),
            "Journalpost som skal ferdigstilles må ha minst en sak"
        )
    }
}

fun validerUtgaaendeJournalpostKanDupliseres(journalpost: Journalpost) {
    Validate.isTrue(
        journalpost.tema == "BID" || journalpost.tema == "FAR",
        "Journalpost må ha tema BID/FAR"
    )
    Validate.isTrue(journalpost.isUtgaaendeDokument(), "Journalpost må være utgående dokument")
    Validate.isTrue(journalpost.hasMottakerId(), "Journalpost må ha satt mottakerId")
    Validate.isTrue(journalpost.hasSak(), "Journalpost må ha sak")
    Validate.isTrue(journalpost.bruker?.id != null, "Journalpost må ha satt brukerid")
}

fun validerKanOppretteJournalpost(request: OpprettJournalpostRequest) {
    Validate.isTrue(request.hasGjelder(), "Journalpost må ha satt gjelder ident")
    request.dokumenter.forEachIndexed { index, it ->
        Validate.isTrue(
            it.tittel.isNotEmpty(),
            "Dokument ${index + 1} mangler tittel. Alle dokumenter må ha satt tittel"
        )
    }
    if (request.journalposttype != no.nav.bidrag.transport.dokument.JournalpostType.NOTAT) {
        Validate.isTrue(
            request.hasAvsenderMottaker(),
            "Journalpost må ha satt avsender/mottaker navn eller ident"
        )
    }

    if (request.skalFerdigstilles) {
        Validate.isTrue(
            request.tema == "BID" || request.tema == "FAR",
            "Journalpost som skal ferdigstilles må ha tema BID/FAR"
        )
        Validate.isTrue(
            !request.hentJournalførendeEnhet().isNullOrEmpty(),
            "Journalpost som skal ferdigstilles må ha satt journalførendeEnhet"
        )
        Validate.isTrue(request.hasSak(), "Journalpost som skal ferdigstilles må ha minst en sak")
    }
}
