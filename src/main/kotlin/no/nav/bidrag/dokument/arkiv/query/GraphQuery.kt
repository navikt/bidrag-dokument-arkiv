package no.nav.bidrag.dokument.arkiv.query

abstract class GraphQuery {

    abstract fun getQuery(): String
    abstract fun getVariables(): HashMap<String, Any>
}
