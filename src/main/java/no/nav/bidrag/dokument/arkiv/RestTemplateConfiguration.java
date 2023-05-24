package no.nav.bidrag.dokument.arkiv;

import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.EnvironmentProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
public class RestTemplateConfiguration {

  @Bean
  @Qualifier("base")
  @Scope("prototype")
  public HttpHeaderRestTemplate restTemplate(
      EnvironmentProperties environmentProperties,
      MetricsRestTemplateCustomizer metricsRestTemplateCustomizer) {
    HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate();
    httpHeaderRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    httpHeaderRestTemplate.withDefaultHeaders();
    httpHeaderRestTemplate.addHeaderGenerator(
        "Nav-Callid", CorrelationId::fetchCorrelationIdForThread);
    httpHeaderRestTemplate.addHeaderGenerator(
        "Nav-Consumer-Id", () -> environmentProperties.naisAppName);
    metricsRestTemplateCustomizer.customize(httpHeaderRestTemplate);
    return httpHeaderRestTemplate;
  }
}
