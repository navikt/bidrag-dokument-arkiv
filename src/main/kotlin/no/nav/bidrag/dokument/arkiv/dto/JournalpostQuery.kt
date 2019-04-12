package no.nav.bidrag.dokument.arkiv.dto

data class JournalpostQuery(val journalpostId: Int) : GraphQuery() {
    private val query = """

    journalpost(journalpostId: "$journalpostId") {
      avsenderMottaker {
        navn
      }
      bruker {
        id
        type
      }
      datoOpprettet
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
        arkivsaksnummer
      }
      tema
      tittel
    }

    """.trimIndent()

    override fun writeQuery(): String {
        return fullQuery(query)
    }
}
