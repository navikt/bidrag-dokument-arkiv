package no.nav.bidrag.dokument.arkiv.query

class DistribusjonInfoQuery(val journalpostId: String) : GraphQuery() {

    override fun getQuery(): String = this.graphqlQuery("distinfo")

    override fun getVariables(): HashMap<String, Any> = hashMapOf("journalpostId" to journalpostId)
}
