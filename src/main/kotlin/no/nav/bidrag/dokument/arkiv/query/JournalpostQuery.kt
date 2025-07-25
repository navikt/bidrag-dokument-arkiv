package no.nav.bidrag.dokument.arkiv.query

data class JournalpostQuery(val journalpostId: Long) : GraphQuery() {

    override fun getQuery(): String = this.graphqlQuery("journalpost")

    override fun getVariables(): HashMap<String, Any> = hashMapOf("journalpostId" to journalpostId.toString())
}
