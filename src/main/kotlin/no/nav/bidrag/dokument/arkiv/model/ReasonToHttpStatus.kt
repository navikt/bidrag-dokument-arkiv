package no.nav.bidrag.dokument.arkiv.model

import org.springframework.http.HttpStatus

class ReasonToHttpStatus(errorReason: Any?) {
    val status: HttpStatus

    init {
        status = when (errorReason?.toString()) {
            "not_found" -> HttpStatus.NOT_FOUND
            "forbidden" -> HttpStatus.FORBIDDEN
            "bad_request" -> HttpStatus.BAD_REQUEST
            "server_error" -> HttpStatus.INTERNAL_SERVER_ERROR
            else -> HttpStatus.BAD_REQUEST
        }
    }
}