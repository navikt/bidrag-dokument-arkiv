package no.nav.bidrag.dokument.arkiv.aop

import no.nav.bidrag.dokument.arkiv.model.HttpStatusException
import no.nav.bidrag.dokument.arkiv.model.JournalIkkeFunnetException
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException
import no.nav.bidrag.dokument.arkiv.model.KnyttTilSakManglerTemaException
import no.nav.bidrag.dokument.arkiv.model.KunneIkkeJournalforeOpprettetJournalpost
import no.nav.bidrag.dokument.arkiv.model.OppdaterJournalpostFeiletFunksjoneltException
import no.nav.bidrag.dokument.arkiv.model.PersonException
import no.nav.bidrag.dokument.arkiv.model.UgyldigAvvikException
import no.nav.bidrag.dokument.arkiv.model.ViolationException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpStatusCodeException

@RestControllerAdvice
class HttpStatusRestControllerAdvice {
    @ResponseBody
    @ExceptionHandler(PersonException::class)
    fun handleTechnicalException(exception: Exception): ResponseEntity<*> {
        LOGGER.warn(exception.message)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, exception.message)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleViolationException(exception: ViolationException): ResponseEntity<*> {
        LOGGER.warn(exception.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, exception.message)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(
        KunneIkkeJournalforeOpprettetJournalpost::class
    )
    fun handleBadRequest(exception: Exception): ResponseEntity<*> {
        LOGGER.warn(exception.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, exception.message)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleOtherExceptions(exception: Exception): ResponseEntity<*> {
        LOGGER.error(exception.message, exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, exception.message)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: Exception): ResponseEntity<*> {
        LOGGER.warn(exception.message)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WARNING, exception.message)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleIllegalArgumentException(
        illegalArgumentException: IllegalArgumentException
    ): ResponseEntity<*> {
        LOGGER.warn(illegalArgumentException.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, illegalArgumentException.message)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleHttpStatusException(httpStatusException: HttpStatusException): ResponseEntity<*> {
        LOGGER.warn(httpStatusException.message)
        return ResponseEntity.status(httpStatusException.status)
            .header(HttpHeaders.WARNING, httpStatusException.message)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(KnyttTilSakManglerTemaException::class, OppdaterJournalpostFeiletFunksjoneltException::class, UgyldigAvvikException::class)
    fun ugyldigInput(exception: Exception): ResponseEntity<*> {
        LOGGER.warn(exception.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, exception.message)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(JournalIkkeFunnetException::class, JournalpostIkkeFunnetException::class)
    fun journalpostIkkeFunnet(exception: Exception): ResponseEntity<*> {
        LOGGER.warn(exception.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .header(HttpHeaders.WARNING, exception.message)
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpClientErrorException(exception: HttpStatusCodeException): ResponseEntity<*> {
        val errorMessage = getErrorMessage(exception)
        LOGGER.warn(errorMessage, exception)
        return ResponseEntity
            .status(exception.statusCode)
            .header(HttpHeaders.WARNING, errorMessage)
            .build<Any>()
    }

    private fun getErrorMessage(exception: HttpStatusCodeException): String {
        val errorMessage = StringBuilder()
        if (exception.responseHeaders != null) {
            errorMessage.append("Det skjedde en feil ved kall mot ekstern tjeneste: ")
            exception.responseHeaders?.get("Warning")
                ?.let { if (it.size > 0) errorMessage.append(it[0]) }
            errorMessage.append(" - ")
        }

        if (!exception.statusText.isNullOrEmpty()) {
            errorMessage.append(exception.statusText)
        }

        return errorMessage.toString()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(HttpStatusRestControllerAdvice::class.java)
    }
}