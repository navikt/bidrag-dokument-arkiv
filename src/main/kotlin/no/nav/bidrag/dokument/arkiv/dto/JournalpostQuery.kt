package no.nav.bidrag.dokument.arkiv.dto

data class JournalpostQuery(val journalpostId: Int) : GraphQuery() {
    private val query = """
        journalpost(journalpostId: $journalpostId) {
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
          tema
          tittel
        }
        """.trimIndent()

    override fun getQuery(): String {
        return query
    }
}
