package no.nav.bidrag.dokument.arkiv.dto

data class DokumentoversiktFagsakQuery(val saksnummer: Int) : GraphQuery() {
    private val query = """

      dokumentoversiktFagsak(fagsak: {fagsakId: "$saksnummer", fagsaksystem: "BI01", foerste: 50}) {
        journalposter {
          avsenderMottaker {
            navn
          }
          bruker {
            id
            type
          }
          dokumenter {
            tittel
          }
          journalforendeEnhet
          journalfortAvNavn
          journalpostId
          journalposttype
          journalstatus
          relevanteDatoer {
            dato
            datotype
          }
          sak {
            fagsakId
          }
          tema
          tittel
        }
      }

    """.trimIndent()

    override fun writeQuery(): String {
        return fullQuery(query)
    }
}
