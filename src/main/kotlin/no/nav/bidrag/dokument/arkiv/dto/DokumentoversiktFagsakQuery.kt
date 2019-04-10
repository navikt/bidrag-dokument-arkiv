package no.nav.bidrag.dokument.arkiv.dto

data class DokumentoversiktFagsakQuery(val saksnummer: String) : GraphQuery() {
    private val query = """

      dokumentoversiktFagsak(fagsak: {fagsakId: "$saksnummer", fagsaksystem: "BI01", foerste: 50}) {
        journalposter {
          journalpostId
          avsenderMottaker {
            erLikBruker
            id
            land
            navn
          }
        }
      }

    """.trimIndent()

    override fun writeQuery(): String {
        return fullQuery(query)
    }
}
