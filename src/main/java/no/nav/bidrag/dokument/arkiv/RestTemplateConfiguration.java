package no.nav.bidrag.dokument.arkiv;

import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.EnvironmentProperties;
import no.nav.bidrag.dokument.arkiv.security.TokenForBasicAuthenticationGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
public class RestTemplateConfiguration {

  @Bean
  @Qualifier("base")
  @Scope("prototype")
  public HttpHeaderRestTemplate restTemplate(
      EnvironmentProperties environmentProperties,
      MetricsRestTemplateCustomizer metricsRestTemplateCustomizer
  ) {
    HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate();
    httpHeaderRestTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    httpHeaderRestTemplate.addHeaderGenerator(CorrelationId.CORRELATION_ID_HEADER, CorrelationId::fetchCorrelationIdForThread);
    httpHeaderRestTemplate.addHeaderGenerator("Nav-Callid", CorrelationId::fetchCorrelationIdForThread);
    httpHeaderRestTemplate.addHeaderGenerator("Nav-Consumer-Id", ()-> environmentProperties.naisAppName);
    metricsRestTemplateCustomizer.customize(httpHeaderRestTemplate);
    return httpHeaderRestTemplate;
  }

  @Bean
  @Qualifier("serviceuser")
  @Scope("prototype")
  public HttpHeaderRestTemplate serviceUserRestTemplate(
      @Qualifier("base") HttpHeaderRestTemplate restTemplate,
      TokenForBasicAuthenticationGenerator tokenForBasicAuthenticationGenerator
  ) {
    restTemplate.addHeaderGenerator(HttpHeaders.AUTHORIZATION, tokenForBasicAuthenticationGenerator::generateToken);
    return restTemplate;
  }

}
