package no.nav.bidrag.dokument.arkiv.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.bidrag.dokument.arkiv.StaticContextAccessor
import org.slf4j.LoggerFactory

object JsonMapper {
    inline fun <reified T> fromJsonString(jsonString: String): T? {
        return try {
            StaticContextAccessor.getBean(ObjectMapper::class.java).readValue(jsonString)
        } catch (e: Exception) {
            LoggerFactory.getLogger(JsonMapper::class.java).error("Det skjedde en feil ved deserialisering json string ved bruk av ObjectMapper fra spring context. Bruker egendefinert ObjectMapper", e)
            ObjectMapper().registerModule(kotlinModule()).readValue(jsonString)
        }
    }

    fun toJsonString(dataObject: Any?): String {
        return try {
            StaticContextAccessor.getBean(ObjectMapper::class.java).writeValueAsString(dataObject)
        } catch (e: Exception) {
            LoggerFactory.getLogger(JsonMapper::class.java).error("Det skjedde en feil ved serialisering av json objekt ved bruk av ObjectMapper fra Spring context. Bruker egendefinert ObjectMapper", e)
            ObjectMapper().registerModule(kotlinModule()).writeValueAsString(dataObject)
        }
    }
}
