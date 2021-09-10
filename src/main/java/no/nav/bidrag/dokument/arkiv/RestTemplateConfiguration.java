package no.nav.bidrag.dokument.arkiv;

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

@Configuration
public class RestTemplateConfiguration {

  @Bean
  @Qualifier("base")
  @Scope("prototype")
  public HttpHeaderRestTemplate restTemplate(
      EnvironmentProperties environmentProperties
  ) {
    HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate();

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
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, ()->"application/json");
    httpHeaderRestTemplate.addHeaderGenerator("Nav-Consumer-Token", tokenForBasicAuthenticationGenerator::generateToken);

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
