package no.nav.bidrag.dokument.arkiv.dto

import org.springframework.http.HttpStatus

class SafException(
    override var message: String,
    private var reason: String,
) : RuntimeException() {
    fun status(): HttpStatus = when (reason) {
        "not_found" -> HttpStatus.NOT_FOUND // throw SafNotFoundException()
        "forbidden" -> HttpStatus.FORBIDDEN
        "bad_request" -> HttpStatus.BAD_REQUEST
        "server_error" -> HttpStatus.INTERNAL_SERVER_ERROR
        else -> HttpStatus.BAD_REQUEST
    }
}
