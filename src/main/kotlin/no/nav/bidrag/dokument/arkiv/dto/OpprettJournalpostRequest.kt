package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.dto.JournalpostDto
import no.nav.bidrag.dokument.dto.NyJournalpostCommand
import no.nav.bidrag.dokument.dto.OpprettDokument
import java.util.stream.Collectors.toList

data class OpprettJournalpostRequest(
        var avsenderMottaker: OpprettAvsenderMottaker? = null,
        var behandlingstema: String? = null,
        var bruker: RegistrerBruker? = null,
        var dokumenter: List<OpprettDokumentApi> = emptyList(),
        var eksternReferanseId: String? = null,
        var journalfoerendeEnhet: String? = null,
        var journalpostType: String? = null,
        var kanal: String? = null,
        var sak: RegistrerSak? = null,
        var tema: String? = null,
        var tittel: String? = null
) {
    constructor(nyJournalpostCommand: NyJournalpostCommand) : this(
            avsenderMottaker = nyJournalpostCommand.avsenderNavn?.let { OpprettAvsenderMottaker(it) },
            behandlingstema = nyJournalpostCommand.behandlingstema,
            eksternReferanseId = nyJournalpostCommand.dokumentreferanse,
            journalfoerendeEnhet = nyJournalpostCommand.journalforendeEnhet,
            journalpostType = nyJournalpostCommand.dokumentType,
            kanal = "TODO: kodeverk",
            sak = nyJournalpostCommand.saksnummer?.let { RegistrerSak(arkivsaksnummer = it) },
            tema = nyJournalpostCommand.fagomrade,
            tittel = nyJournalpostCommand.tittel,
            dokumenter = nyJournalpostCommand.dokumenter.stream()
                    .map { OpprettDokumentApi(it) }
                    .collect(toList())
    )
// json som ikke er implementert
//    "tilleggsopplysninger": [{
//        "nokkel": "bucid",
//        "verdi": "eksempel_verdi_123"
//    }],
}

data class OpprettAvsenderMottaker(
        var navn: String
)

class RegistrerBruker(
        var id: String,
        val idType: String = "FNR"
)

data class OpprettDokumentApi(
        var brevkode: String?,
        var dokumentKategori: String?,
        var tittel: String?
) {
    constructor(opprettDokument: OpprettDokument) : this(
            brevkode = opprettDokument.brevkode,
            dokumentKategori = opprettDokument.dokumentKategori,
            tittel = opprettDokument.tittel
    )
}
// json som ikke er implementert
//    "dokumentvarianter": [
//        {
//            "filnavn": "eksempeldokument.pdf",
//            "filtype": "PDFA",
//            "fysiskDokument": "AAAAAAAA",
//            "variantformat": "ARKIV"
//        }
//    ],

data class RegistrerSak(
        val arkivsaksnummer: String,
        val arkivsaksystem: String = "GSAK"
)

data class OpprettJournalpostResponse(
        var journalpostId: Int = -1
) {
    fun tilJournalpostDto() = JournalpostDto(journalpostId = "JOARK-" + journalpostId)
}
