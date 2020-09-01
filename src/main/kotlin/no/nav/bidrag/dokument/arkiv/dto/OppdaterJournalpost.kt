package no.nav.bidrag.dokument.arkiv.dto

data class OppdaterJournalpostRequest(
        val journalpostId: Int,
        private val endreJournalpostCommand: EndreJournalpostCommandIntern,
        private var avsenderNavn: String? = null,
        private var journalforendeEnhet: String? = null
) {
    private var sakJson = SakJson()
    private val tittel = Tittel()

    constructor(journalpostId: Int, endreJournalpostCommand: EndreJournalpostCommandIntern, journalpost: Journalpost) : this(
            journalpostId = journalpostId,
            endreJournalpostCommand = endreJournalpostCommand
    ) {
        avsenderNavn = endreJournalpostCommand.hentAvsenderNavn(journalpost)
        journalforendeEnhet = endreJournalpostCommand.enhet

        val saksnummer = if (endreJournalpostCommand.harEnTilknyttetSak()) {
            endreJournalpostCommand.hentTilknyttetSak()
        } else {
            journalpost.sak?.fagsakId
        }

        sakJson = SakJson(saksnummer)
    }

    fun tilJournalpostApi(): String {
        return """
               {
                 "avsenderMottaker": {
                   "navn": "$avsenderNavn"
                 },
                 "behandlingstema": "${endreJournalpostCommand.hentFagomrade()}",
                 "bruker": {
                   "id": ${endreJournalpostCommand.hentGjelder()},
                   "idType": "${endreJournalpostCommand.hentGjelderType()}"
                 },
                 ${endreJournalpostCommand.hentJsonForEndredeDokumenter()}
                 "journalfoerendeEnhet": $journalforendeEnhet,
                 ${sakJson.tilJson()}
                 "tema": "${endreJournalpostCommand.hentFagomrade()}",
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

    private class SakJson(private val saksnummer: String? = null) {
        fun tilJson() = """
        "sak": {
          "arkivsaksnummer": $saksnummer,
          "arkivsaksystem": "GSAK"
        },
        """
    }

    private class Tittel(private val tittel: String? = null) {
        fun tilJson() = if (tittel == null) "" else """"tittel":"$tittel""""
    }
}

data class OppdaterJournalpostResponse(
        var journalpostId: Int? = null,
        var saksnummer: String? = null
)
