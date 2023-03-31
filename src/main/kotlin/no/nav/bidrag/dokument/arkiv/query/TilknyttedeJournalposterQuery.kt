package no.nav.bidrag.dokument.arkiv.query

data class TilknyttedeJournalposterQuery(val dokumentInfoId: String) : GraphQuery() {
    override fun getQuery(): String {
        return this.graphqlQuery("tilknyttedeJournalposter")
    }

    override fun getVariables(): HashMap<String, Any> {
        return hashMapOf("dokumentInfoId" to dokumentInfoId)
    }
}
