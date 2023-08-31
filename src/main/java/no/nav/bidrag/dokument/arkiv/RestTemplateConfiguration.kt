package no.nav.bidrag.dokument.arkiv

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.EnvironmentProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.metrics.web.client.ObservationRestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory

@Configuration
@Import(RestOperationsAzure::class)
class RestTemplateConfiguration {
    @Bean
    @Qualifier("base")
    @Scope("prototype")
    fun restTemplate(
        environmentProperties: EnvironmentProperties,
        observationRestTemplateCustomizer: ObservationRestTemplateCustomizer
    ): HttpHeaderRestTemplate {
        val httpHeaderRestTemplate = HttpHeaderRestTemplate()
        val cf = HttpComponentsClientHttpRequestFactory()
        cf.setBufferRequestBody(false)
        httpHeaderRestTemplate.requestFactory = cf
        httpHeaderRestTemplate.withDefaultHeaders()
        httpHeaderRestTemplate.addHeaderGenerator(
            "Nav-Callid"
        ) { CorrelationId.fetchCorrelationIdForThread() }
        httpHeaderRestTemplate.addHeaderGenerator("Nav-Consumer-Id") { environmentProperties.naisAppName }
        observationRestTemplateCustomizer.customize(httpHeaderRestTemplate)
        return httpHeaderRestTemplate
    }
}
