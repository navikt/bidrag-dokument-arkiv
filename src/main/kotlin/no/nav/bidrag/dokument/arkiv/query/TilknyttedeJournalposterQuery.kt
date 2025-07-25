package no.nav.bidrag.dokument.arkiv.query

data class TilknyttedeJournalposterQuery(val dokumentInfoId: String) : GraphQuery() {
    override fun getQuery(): String = this.graphqlQuery("tilknyttedeJournalposter")

    override fun getVariables(): HashMap<String, Any> = hashMapOf("dokumentInfoId" to dokumentInfoId)
}
