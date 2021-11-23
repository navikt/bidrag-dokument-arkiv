package no.nav.bidrag.dokument.arkiv.query

data class JournalpostQuery(val journalpostId: Long) : GraphQuery() {

    private val query = """
        query journalpost(${"$"}journalpostId: String!) {
            journalpost(journalpostId: ${"$"}journalpostId) {
              avsenderMottaker {
                navn
              }
              bruker {
                id
                type
              }
              dokumenter {
                dokumentInfoId
                tittel
              }
              sak {
                fagsakId
              }
              journalforendeEnhet
              journalfortAvNavn
              journalpostId
              kanal
              journalposttype
              journalstatus
              relevanteDatoer {
                dato
                datotype
              }
              tema
              tittel
            }
        }
        """
    override fun getQuery(): String {
        return query
    }

    override fun getVariables(): HashMap<String, Any> {
        return hashMapOf("journalpostId" to journalpostId)
    }
}