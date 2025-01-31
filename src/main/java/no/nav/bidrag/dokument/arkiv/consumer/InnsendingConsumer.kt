package no.nav.bidrag.dokument.arkiv.consumer

import mu.KotlinLogging
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.dokument.arkiv.consumer.dto.DokumentSoknadDto
import no.nav.bidrag.dokument.arkiv.consumer.dto.EksternEttersendingsOppgave
import no.nav.bidrag.dokument.arkiv.consumer.dto.HentEtterseningsoppgaveRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
private val LOGGER = KotlinLogging.logger {}

@Service
class InnsendingConsumer(
    @Value("\${INNSENDING_API_URL}") val url: URI,
    @Qualifier("azureService") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "innsending-api") {
    private fun createUri(path: String? = null) = UriComponentsBuilder
        .fromUri(url)
        .path("ekstern/v1/oppgaver")
        .path(path ?: "")
        .build()
        .toUri()

    fun opprettEttersendingsoppgave(oppgave: EksternEttersendingsOppgave): DokumentSoknadDto {
        try {
            return postForNonNullEntity(createUri(), oppgave)
        } catch (e: HttpStatusCodeException) {
            LOGGER.info("Det skjedde en feil ved opprettelse av ettersendingsoppgave")
            secureLogger.error("Det skjedde en feil ved opprettelse av ettersendingsoppgave $oppgave", e)
            throw e
        }
    }

    fun hentEttersendingsoppgave(oppgave: HentEtterseningsoppgaveRequest): List<DokumentSoknadDto> {
        try {
            return executeMedMetrics(createUri()) {
                operations.exchange<List<DokumentSoknadDto>>(
                    createUri(),
                    HttpMethod.GET,
                    HttpEntity(oppgave),
                )
            }!!
        } catch (e: HttpStatusCodeException) {
            LOGGER.info("Det skjedde en feil ved henting av ettersendingsoppgave")
            secureLogger.error("Det skjedde en feil ved henting av ettersendingsoppgave $oppgave", e)
            throw e
        }
    }
}
