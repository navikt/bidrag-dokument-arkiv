package no.nav.bidrag.dokument.arkiv.consumer

import mu.KotlinLogging
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.dokument.arkiv.model.DistribusjonFeiletTekniskException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val LOGGER = KotlinLogging.logger {}

@Service
class BidragDokumentConsumer(
    @Value("\${BIDRAG_DOKUMENT_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag_dokument") {

    private val dokumentUrl
        get() =
            UriComponentsBuilder.fromUri(url)

    @Retryable(
        value = [DistribusjonFeiletTekniskException::class],
        backoff = Backoff(delay = 500, maxDelay = 2000, multiplier = 2.0)
    )
    fun hentDokument(
        dokumentId: String
    ): ByteArray = getForNonNullEntity(
        dokumentUrl.pathSegment("dokumentreferanse").pathSegment(dokumentId)
            .build().toUri()
    )

}