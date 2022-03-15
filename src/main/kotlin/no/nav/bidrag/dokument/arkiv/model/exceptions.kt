package no.nav.bidrag.dokument.arkiv.model

import org.springframework.http.HttpStatus

abstract class HttpStatusException(message: String, throwable: Throwable? = null) : RuntimeException(message, throwable) {
    abstract val status: HttpStatus
}

open class FunksjonellFeilException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus get() = HttpStatus.BAD_REQUEST
}

open class TekniskFeilException(message: String, throwable: Throwable) : HttpStatusException(message, throwable) {
    override val status: HttpStatus get() = HttpStatus.INTERNAL_SERVER_ERROR
}

class JournalIkkeFunnetException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus get() = HttpStatus.NOT_FOUND
}

class JournalpostManglerMottakerIdException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus get() = HttpStatus.BAD_REQUEST
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
class UgyldigAvvikException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus = HttpStatus.BAD_REQUEST
}
abstract class AvvikFeiletException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
}

abstract class EndreJournalpostFeiletException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
}

class LagreJournalpostFeiletException(message: String) : EndreJournalpostFeiletException(message)
class FerdigstillFeiletException(message: String) : EndreJournalpostFeiletException(message)

class OppdaterJournalpostFeiletException(message: String) : AvvikFeiletException(message)
class TrekkJournalpostFeiletException(message: String) : AvvikFeiletException(message)
class FeilforSakFeiletException(message: String) : AvvikFeiletException(message)

class DistribusjonFeiletException(message: String): FunksjonellFeilException(message)
class DistribusjonFeiletTekniskException(message: String, throwable: Throwable): TekniskFeilException(message, throwable)