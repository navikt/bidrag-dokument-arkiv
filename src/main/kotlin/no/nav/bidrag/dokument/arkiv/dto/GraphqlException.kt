package no.nav.bidrag.dokument.arkiv.dto

import org.springframework.http.HttpStatus
import java.lang.RuntimeException

class GraphqlException(
        override var message: String,
        var reason: String,
) : RuntimeException() {
    fun status(): HttpStatus =
            when (reason) {
                "not_found" -> HttpStatus.NOT_FOUND
                "forbidden" -> HttpStatus.FORBIDDEN
                "bad_request" -> HttpStatus.BAD_REQUEST
                "server_error" -> HttpStatus.INTERNAL_SERVER_ERROR
                else -> HttpStatus.BAD_REQUEST
            }

    fun toJson() = """
        {
            "message": "$message"
        }
    """.trimIndent()
}
