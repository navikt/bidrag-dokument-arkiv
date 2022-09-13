package no.nav.bidrag.dokument.arkiv.model

import org.springframework.http.HttpStatus

class ViolationException(violations: MutableList<String>) : RuntimeException("Ugyldige data: ${violations.joinToString(", ")}")

abstract class HttpStatusException(message: String, throwable: Throwable? = null) : RuntimeException(message, throwable) {
    abstract val status: HttpStatus
}

open class FunksjonellFeilException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus get() = HttpStatus.BAD_REQUEST
}

open class TekniskFeilException(message: String, throwable: Throwable) : HttpStatusException(message, throwable) {
    override val status: HttpStatus get() = HttpStatus.INTERNAL_SERVER_ERROR
}

class JournalIkkeFunnetException(message: String) : RuntimeException(message)
class JournalpostDataException(message: String) : RuntimeException(message)
class JournalpostHendelseException(message: String, throwable: Throwable) : RuntimeException(message, throwable)
class JournalpostIkkeFunnetException(message: String) : RuntimeException(message)

class SafException(message: String, override val status: HttpStatus) : HttpStatusException(message)
class PersonException(message: String) : RuntimeException(message)
class ResourceDiscriminatorException(message: String) : RuntimeException(message)
class AvvikDetaljException(detalj: String) : RuntimeException("Manglende detalj i avvik: $detalj")
class AvvikNotSupportedException(message: String) : HttpStatusException(message) {
    override val status: HttpStatus = HttpStatus.BAD_REQUEST
}
class UgyldigAvvikException(message: String) : RuntimeException(message);

class KnyttTilSakManglerTemaException(message: String) : RuntimeException(message)

class UgyldigDistribusjonException(message: String): FunksjonellFeilException(message)
class DistribusjonFeiletFunksjoneltException(message: String): FunksjonellFeilException(message)
class DistribusjonFeiletTekniskException(message: String, throwable: Throwable): TekniskFeilException(message, throwable)

class OppdaterJournalpostFeiletFunksjoneltException(message: String): FunksjonellFeilException(message)
class OppdaterJournalpostFeiletTekniskException(message: String, throwable: Throwable): TekniskFeilException(message, throwable)

class JournalpostHarIkkeKommetIRetur(message: String): RuntimeException(message)

class KunneIkkeFerdigstilleOpprettetJournalpost(message: String): RuntimeException(message)