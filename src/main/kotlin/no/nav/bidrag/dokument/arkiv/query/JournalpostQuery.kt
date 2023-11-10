package no.nav.bidrag.dokument.arkiv.query

data class JournalpostQuery(val journalpostId: Long) : GraphQuery() {

    override fun getQuery(): String {
        return this.graphqlQuery("journalpost")
    }

    override fun getVariables(): HashMap<String, Any> {
        return hashMapOf("journalpostId" to journalpostId)
    }
}
