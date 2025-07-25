package no.nav.bidrag.dokument.arkiv.consumer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.HttpResponse
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostResponse
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.model.DistribusjonFeiletFunksjoneltException
import no.nav.bidrag.dokument.arkiv.model.DistribusjonFeiletTekniskException
import no.nav.bidrag.transport.dokument.DistribuerJournalpostResponse
import no.nav.bidrag.transport.dokument.DistribuerTilAdresse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.apache.logging.log4j.util.Strings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
@Service
class DokdistFordelingConsumer(
    @Value("\${DOKDISTFORDELING_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "dokdistfordenling") {
    private fun createUri() = UriComponentsBuilder
        .fromUri(url)
        .path("rest/v1/distribuerjournalpost")
        .build()
        .toUri()

    @Retryable(retryFor = [DistribusjonFeiletTekniskException::class], backoff = Backoff(delay = 500, maxDelay = 2000, multiplier = 2.0))
    fun distribuerJournalpost(
        journalpost: Journalpost,
        batchId: String?,
        adresse: DistribuerTilAdresse?,
    ): DistribuerJournalpostResponse {
        val journalpostId = journalpost.hentJournalpostIdLong()
        val request =
            DokDistDistribuerJournalpostRequest(
                journalpostId!!,
                journalpost.hentBrevkode(),
                journalpost.hentTittel(),
                adresse,
                batchId,
            )
        LOGGER.info(
            "Bestiller distribusjon for journalpost {} med distribusjonstype {} og distribusjonstidspunkt {}{}",
            request.journalpostId,
            request.distribusjonstype,
            request.distribusjonstidspunkt,
            if (Strings.isNotEmpty(batchId)) String.format(" og batchId %s", batchId) else "",
        )

        try {
            return postForNonNullEntity<DokDistDistribuerJournalpostResponse>(createUri(), request)
                .toDistribuerJournalpostResponse(journalpostId)
        } catch (e: HttpStatusCodeException) {
            val status = e.statusCode
            val errorMessage = parseErrorMessage(e)
            if (HttpStatus.CONFLICT == status) {
                LOGGER.warn(
                    "Distribusjon er allerede bestillt for journalpost {}. Fortsetter behandling.",
                    journalpostId,
                )
                return conflictExceptionToResponse(journalpostId, e)
            }

            if (HttpStatus.BAD_REQUEST == status || HttpStatus.NOT_FOUND == status) {
                throw DistribusjonFeiletFunksjoneltException(
                    String.format(
                        "Distribusjon feilet for JOARK journalpost %s med status %s og feilmelding: %s",
                        journalpostId,
                        e.statusCode,
                        errorMessage,
                    ),
                )
            }

            throw DistribusjonFeiletTekniskException(
                String.format(
                    "Distribusjon feilet teknisk for JOARK journalpost %s med status %s og feilmelding: %s",
                    journalpostId,
                    e.statusCode,
                    errorMessage,
                ),
                e,
            )
        }
    }

    private fun conflictExceptionToResponse(
        journalpostId: Long,
        e: HttpStatusCodeException,
    ): DistribuerJournalpostResponse {
        try {
            val response =
                commonObjectmapper.readValue(
                    e.responseBodyAsString,
                    DokDistDistribuerJournalpostResponse::class.java,
                )
            return response.toDistribuerJournalpostResponse(journalpostId)
        } catch (ex: Exception) {
            return DistribuerJournalpostResponse(journalpostId.toString(), null, null)
        }
    }

    private fun parseErrorMessage(e: HttpStatusCodeException): String? {
        try {
            val jsonNode = commonObjectmapper.readValue<JsonNode>(e.responseBodyAsString, JsonNode::class.java)
            if (jsonNode.has("message")) {
                return jsonNode.get("message").asText()
            }
            return e.message
        } catch (ex: Exception) {
            return e.message
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(DokarkivConsumer::class.java)
    }
}
