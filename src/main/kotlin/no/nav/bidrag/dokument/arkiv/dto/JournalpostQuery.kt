package no.nav.bidrag.dokument.arkiv.dto

data class JournalpostQuery(val journalpostId: Int) : GraphQuery() {
    private val query ="""
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
                tittel
              }
              sak {
                fagsakId
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
        }
        """

    override fun getQuery(): String {
        return query
    }

    override fun getVariables(): HashMap<String, Any> {
        return hashMapOf("journalpostId" to journalpostId)
    }


}
