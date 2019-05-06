package no.nav.bidrag.dokument.arkiv.dto

abstract class GraphQuery {
    abstract fun getQuery(): String

    fun writeQuery(): String {
        val query = getQuery().replace(Regex("\n"), "")
        return "{\"query\":\"query {$query}\"}"
    }
}
