package no.nav.bidrag.dokument.arkiv.dto

abstract class GraphQuery {
    protected fun fullQuery(query: String): String {
        return "query {$query}"
    }

    abstract fun writeQuery(): String
}
