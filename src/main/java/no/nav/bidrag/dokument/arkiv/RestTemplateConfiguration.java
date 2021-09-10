package no.nav.bidrag.dokument.arkiv;

import static no.nav.bidrag.dokument.arkiv.security.TokenForBasicAuthenticationGenerator.HEADER_NAV_CONSUMER_TOKEN;

import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.EnvironmentProperties;
import no.nav.bidrag.dokument.arkiv.security.OidcTokenGenerator;
import no.nav.bidrag.dokument.arkiv.security.TokenForBasicAuthenticationGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
public class RestTemplateConfiguration {

  @Bean
  @Qualifier("base")
  @Scope("prototype")
  public HttpHeaderRestTemplate restTemplate(
      EnvironmentProperties environmentProperties
  ) {
    HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate();
    httpHeaderRestTemplate.setRequestFactory( new HttpComponentsClientHttpRequestFactory());

    httpHeaderRestTemplate.addHeaderGenerator(CorrelationId.CORRELATION_ID_HEADER, CorrelationId::fetchCorrelationIdForThread);
    httpHeaderRestTemplate.addHeaderGenerator("Nav-Callid", CorrelationId::fetchCorrelationIdForThread);
    httpHeaderRestTemplate.addHeaderGenerator("Nav-Consumer-Id", ()-> environmentProperties.naisAppName);

    return httpHeaderRestTemplate;
  }

  @Bean
  @Qualifier("dokarkiv")
  @Scope("prototype")
  public HttpHeaderRestTemplate dokarkivRestTemplate(
      OidcTokenGenerator oidcTokenGenerator,
      TokenForBasicAuthenticationGenerator tokenForBasicAuthenticationGenerator,
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate
  ) {
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.AUTHORIZATION, oidcTokenGenerator::fetchBearerToken);
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);
    httpHeaderRestTemplate.addHeaderGenerator(HEADER_NAV_CONSUMER_TOKEN, tokenForBasicAuthenticationGenerator::generateToken);

    return httpHeaderRestTemplate;
  }

  @Bean
  @Qualifier("saf")
  @Scope("prototype")
  public HttpHeaderRestTemplate safRestTemplate(
      OidcTokenGenerator oidcTokenGenerator,
      @Qualifier("dokarkiv") HttpHeaderRestTemplate httpHeaderRestTemplate
  ) {
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.AUTHORIZATION, oidcTokenGenerator::fetchBearerToken);

    return httpHeaderRestTemplate;
  }
}
