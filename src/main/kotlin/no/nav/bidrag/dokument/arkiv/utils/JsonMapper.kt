package no.nav.bidrag.dokument.arkiv.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.bidrag.dokument.arkiv.StaticContextAccessor
import org.slf4j.LoggerFactory

object JsonMapper {
    inline fun <reified T> fromJsonString(jsonString: String): T? {
        return try {
            StaticContextAccessor.getBean(ObjectMapper::class.java).readValue(jsonString)
        } catch (e: Exception) {
            LoggerFactory.getLogger(JsonMapper::class.java).warn("Det skjedde en feil ved deserialisering json string", e)
            null
        }
    }

    fun toJsonString(dataObject: Any?): String {
        return try {
            StaticContextAccessor.getBean(ObjectMapper::class.java).writeValueAsString(dataObject)
        } catch (e: Exception) {
            LoggerFactory.getLogger(JsonMapper::class.java).warn("Det skjedde en feil ved serialisering av json objekt", e)
            throw RuntimeException("Det skjedde en feil ved serialisering av json objekt", e)
        }
    }
}