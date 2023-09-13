package no.nav.bidrag.dokument.arkiv.consumer

import mu.KotlinLogging
import no.nav.bidrag.commons.web.client.AbstractRestClient
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
class DokdistKanalConsumer(
    @Value("\${DOKDISTKANAL_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations
) : AbstractRestClient(restTemplate, "dokdistkanal") {

    private val dokdistkanalUrl
        get() =
            UriComponentsBuilder.fromUri(url).pathSegment("rest")
                .pathSegment("bestemDistribusjonskanal")

    @Retryable(backoff = Backoff(delay = 500, maxDelay = 2000, multiplier = 2.0))
    fun bestimDistribusjonsKanal(
        gjelderId: String,
        mottakerId: String? = null
    ): BestemKanalResponse = postForNonNullEntity(
        dokdistkanalUrl.build().toUri(),
        BestemKanalRequest(
            brukerId = gjelderId,
            mottakerId = mottakerId ?: "11111111111"
        )
    )
}

data class BestemKanalRequest(
    val brukerId: String,
    val mottakerId: String,
    val erArkivert: Boolean = true,
    val tema: String = "BID"
)

data class BestemKanalResponse(
    val regel: String,
    val regelBegrunnelse: String,
    val distribusjonskanal: DistribusjonsKanal
)

enum class DistribusjonsKanal {
    PRINT,
    SDP,
    DITT_NAV,
    LOKAL_PRINT,
    INGEN_DISTRIBUSJON,
    TRYGDERETTEN,
    DPVT
}
