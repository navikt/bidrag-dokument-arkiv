package no.nav.bidrag.dokument.arkiv.dto

abstract class GraphQuery {
    protected fun fullQuery(query: String): String {
        return """
            query {
                ${query}
            }""".trimIndent()
    }

    abstract fun writeQuery(): String
}
