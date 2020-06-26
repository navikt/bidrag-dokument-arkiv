package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.dto.EndreDokument
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand
import java.util.stream.Collectors.toList

data class OppdaterJournalpostRequest(
        val journalpostId: Int,
        private val endreJournalpostCommand: EndreJournalpostCommand,
        private var avsenderNavn: String? = null,
        private var journalforendeEnhet: String? = null
) {
    private var sakJson = SakJson()
    private val tittel = Tittel()

    constructor(journalpostId: Int, endreJournalpostCommand: EndreJournalpostCommand, journalpost: Journalpost) : this(
            journalpostId = journalpostId,
            endreJournalpostCommand = endreJournalpostCommand
    ) {
        avsenderNavn = if (endreJournalpostCommand.avsenderNavn != null) endreJournalpostCommand.avsenderNavn else journalpost.hentAvsenderNavn()
        journalforendeEnhet = if (endreJournalpostCommand.journalforendeEnhet != null) endreJournalpostCommand.journalforendeEnhet else journalpost.journalforendeEnhet

        val saksnummer: String?

        if (endreJournalpostCommand.tilknyttSaker.isNotEmpty()) {
            if (endreJournalpostCommand.tilknyttSaker.size > 1)
                throw IllegalArgumentException("joark støtter bare en sak per journalpost")

            saksnummer = endreJournalpostCommand.tilknyttSaker.first()
        } else {
            saksnummer = journalpost.sak?.fagsakId
        }

        sakJson = SakJson(saksnummer)
    }

    fun tilJournalpostApi(): String {
        return """
               {
                 "avsenderMottaker": {
                   "navn": "$avsenderNavn"
                 },
                 "behandlingstema": "${endreJournalpostCommand.fagomrade}",
                 "bruker": {
                   "id": ${endreJournalpostCommand.gjelder},
                   "idType": "${hentGjelderType()}"
                 },
                 ${hentJsonForEndredeDokumenter()}
                 "journalfoerendeEnhet": $journalforendeEnhet,
                 ${sakJson.tilJson()}
                 "tema": "${endreJournalpostCommand.fagomrade}",
                 "tilleggsopplysninger": [],
                 ${tittel.tilJson()}"
               }
               """.trimIndent()
    }
// json som ikke er implementert
//    "tilleggsopplysninger": [{
//        "nokkel": "bucid",
//        "verdi": "eksempel_verdi_123"
//    }],

    private fun hentGjelderType() = if (endreJournalpostCommand.gjelderType != null) endreJournalpostCommand.gjelderType!! else "FNR"
    private fun hentJsonForEndredeDokumenter() = if (endreJournalpostCommand.endreDokumenter.isEmpty()) "" else """"dokumenter": [${hentJsonPerDokument()}],"""
    private fun hentJsonPerDokument() = endreJournalpostCommand.endreDokumenter.stream()
            .map { dok -> Dokumentendring(dok).tilJson() }
            .collect(toList()).joinToString(",")

    private class Dokumentendring(private val endreDokument: EndreDokument) {
        fun tilJson() = """
        {
          "brevkode": "${endreDokument.brevkode}",
          "dokumentInfoId": ${endreDokument.dokId},
          "tittel": "${endreDokument.tittel}"
        }
        """.trimIndent()
    }

    private class SakJson(private val saksnummer: String? = null) {
        fun tilJson() = """
        "sak": {
          "arkivsaksnummer": $saksnummer,
          "arkivsaksystem": "GSAK"
        },
        """.trimIndent()
    }

    private class Tittel(private val tittel: String? = null) {
        fun tilJson() = if (tittel == null) "" else """"tittel":"$tittel""""
    }
}

data class OppdaterJournalpostResponse(
        var journalpostId: Int? = null,
        var saksnummer: String? = null
)
