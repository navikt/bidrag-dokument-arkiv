package no.nav.bidrag.dokument.arkiv.query

import org.apache.commons.lang3.StringUtils

abstract class GraphQuery {

    abstract fun getQuery(): String
    abstract fun getVariables(): HashMap<String, Any>

    fun graphqlQuery(pdlResource: String): String {
        return this::class.java.getResource("/graphql/$pdlResource.graphql")!!
            .readText()
            .graphqlCompatible()
    }

    private fun String.graphqlCompatible(): String {
        return StringUtils.normalizeSpace(this.replace("\n", ""))
    }
}
