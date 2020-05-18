package no.nav.bidrag.dokument.arkiv.dto

abstract class GraphQuery {
    abstract fun getQuery(): String

    fun writeQuery(): String {
        return """
            {
              "query":"{${getQuery()}}"
            }
        """.replace("\n", "").trim()
    }
}
