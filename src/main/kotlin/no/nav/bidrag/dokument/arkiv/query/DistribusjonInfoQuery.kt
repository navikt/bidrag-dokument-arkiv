package no.nav.bidrag.dokument.arkiv.query

class DistribusjonInfoQuery(val journalpostId: String) : GraphQuery() {

    override fun getQuery(): String {
        return this.graphqlQuery("distinfo")
    }

    override fun getVariables(): HashMap<String, Any> {
        return hashMapOf("journalpostId" to journalpostId)
    }
}