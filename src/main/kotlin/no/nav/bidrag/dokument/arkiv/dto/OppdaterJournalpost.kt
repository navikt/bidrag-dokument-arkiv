package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.dto.EndreDokument
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand
import java.util.stream.Collectors.toList

data class OppdaterJournalpostRequest(
        val journalpostId: Int,
        private val avsenderNavn: String?,
        private val behandlingstema: String?,
        private val endreDokumenter: List<EndreDokument> = emptyList(),
        private val gjelder: String,
        private var gjelderType: String,
        private var journalforendeEnhet: String?,
        private val saksnummer: String,
        private val tema: String?,
        private val tittel: String?
) {
    constructor(journalpostId: Int, saksnummerTilEndring: String, endreCmd: EndreJournalpostCommand, journalpost: Journalpost) : this(
            journalpostId = journalpostId,
            avsenderNavn = if (endreCmd.avsenderNavn != null) endreCmd.avsenderNavn else journalpost.avsenderMottaker?.navn,
            behandlingstema = endreCmd.behandlingstema,
            endreDokumenter = endreCmd.endreDokumenter,
            gjelder = endreCmd.gjelder!!,
            gjelderType = if (endreCmd.gjelderType != null) endreCmd.gjelderType!! else "FNR",
            journalforendeEnhet = if (endreCmd.journalforendeEnhet != null) endreCmd.journalforendeEnhet else journalpost.journalforendeEnhet,
            saksnummer = saksnummerTilEndring,
            tema = endreCmd.fagomrade,
            tittel = endreCmd.tittel

    )

    fun tilJournalpostApi(): String {
        return """
               {
                 "avsenderMottaker": {
                   "navn": "$avsenderNavn"
                 },
                 "behandlingstema": "$behandlingstema",
                 "bruker": {
                   "id": $gjelder,
                   "idType": "$gjelderType"
                 },
                 "dokumenter": [${hentJsonForEndredeDokumenter()}],
                 "journalfoerendeEnhet": $journalforendeEnhet,
                 "sak": {
                   "arkivsaksnummer": $saksnummer,
                   "arkivsaksystem": "GSAK"
                 },
                 "tema": "$tema",
                 "tilleggsopplysninger": [],
                 "tittel": "$tittel"
               }
               """.trimIndent()
    }
// json som ikke er implementert
//    "tilleggsopplysninger": [{
//        "nokkel": "bucid",
//        "verdi": "eksempel_verdi_123"
//    }],

    private fun hentJsonForEndredeDokumenter(): String {
        return endreDokumenter.stream()
                .map { dok: EndreDokument -> Dokumentendring(dok).tilJson()  }
                .collect(toList()).joinToString(",")
    }
}

internal data class Dokumentendring(val endreDokument: EndreDokument) {
    fun tilJson() = """
        {
          "brevkode": "${endreDokument.brevkode}",
          "dokumentInfoId": ${endreDokument.dokId},
          "tittel": "${endreDokument.tittel}"
        }
        """.trimIndent()
}

data class OppdaterJournalpostResponse(
        var journalpostId: Int? = null,
        var saksnummer: String? = null
)
