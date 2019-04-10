package no.nav.bidrag.dokument.arkiv.dto

data class JournalpostQuery(val journalpostId: String) : GraphQuery() {
    private val queru = """

    journalpost(journalpostId: "$journalpostId") {
      journalpostId
      tittel
      journalposttype
      journalstatus
      temq
      dokumenter {
        dokumentInfoId
        tittel
      }
    }

    """.trimIndent()

    override fun writeQuery(): String {
        return fullQuery(queru)
    }
}
