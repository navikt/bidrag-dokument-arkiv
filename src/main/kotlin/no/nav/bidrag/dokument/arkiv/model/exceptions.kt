package no.nav.bidrag.dokument.arkiv.model

import org.springframework.http.HttpStatus

abstract class HttpStatusException(message: String) : RuntimeException(message) {
    abstract val status: HttpStatus
}

class JournalIkkeFunnetException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus get() = HttpStatus.NOT_FOUND
}

class JournalpostDataException(message: String) : RuntimeException(message)
class JournalpostHendelseException(message: String, throwable: Throwable) : RuntimeException(message, throwable)
class JournalpostIkkeFunnetException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus get() = HttpStatus.NOT_FOUND
}

class SafException(message: String, override val status: HttpStatus) : HttpStatusException(message)
class PersonException(message: String, override val status: HttpStatus) : HttpStatusException(message)
class TokenException(message: String) : RuntimeException(message)
class ResourceDiscriminatorException(message: String) : RuntimeException(message)
class AvvikDetaljException(detalj: String) : RuntimeException("Manglende detalj i avvik: $detalj")
class AvvikNotSupportedException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus = HttpStatus.BAD_REQUEST
}