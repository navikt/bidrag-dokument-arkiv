package no.nav.bidrag.dokument.arkiv.dto

data class DokumentoversiktFagsakQuery(val saksnummer: String, val tema: String) : GraphQuery() {
    private val query = """dokumentoversiktFagsak(fagsak: {fagsakId: \"$saksnummer\", fagsaksystem: \"BI01\"}, tema:$tema, foerste: 500) {
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
      }""".trimIndent()

    override fun getQuery(): String {
        return query
    }
}
