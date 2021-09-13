package no.nav.bidrag.dokument.arkiv.dto

import org.springframework.http.HttpStatus

abstract class HttpStatusException(message: String) : RuntimeException(message) {
    abstract val status: HttpStatus
}

class JournalIkkeFunnetException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus get() = HttpStatus.NOT_FOUND
}

class JournalpostDataException(message: String) : RuntimeException(message)
class JournalpostIkkeFunnetException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus get() = HttpStatus.NOT_FOUND
}

class SafException(message: String, override val status: HttpStatus) : HttpStatusException(message)
class TokenException(message: String) : RuntimeException(message)
