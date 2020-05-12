package no.nav.bidrag.dokument.arkiv;

import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.security.TokenForBasicAuthenticationGenerator;
import no.nav.bidrag.dokument.arkiv.security.OidcTokenGenerator;
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
  public HttpHeaderRestTemplate restTemplate() {
    HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate();

    httpHeaderRestTemplate.addHeaderGenerator(CorrelationId.CORRELATION_ID_HEADER, CorrelationId::fetchCorrelationIdForThread);

    return httpHeaderRestTemplate;
  }

  @Bean
  @Qualifier("token")
  @Scope("prototype")
  public HttpHeaderRestTemplate tokenRestTemplate(@Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate) {
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.AUTHORIZATION, () -> "Basic <TODO BASE64 CREDENTIALS>");

    return httpHeaderRestTemplate;
  }

  @Bean
  @Qualifier("dokarkiv")
  @Scope("prototype")
  public HttpHeaderRestTemplate dokarkivRestTemplate(
      OidcTokenGenerator oidcTokenGenerator,
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate
  ) {
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.AUTHORIZATION, oidcTokenGenerator::fetchBearerToken);

    return httpHeaderRestTemplate;
  }

  @Bean
  @Qualifier("saf")
  @Scope("prototype")
  public HttpHeaderRestTemplate safRestTemplate(
      TokenForBasicAuthenticationGenerator tokenForBasicAuthenticationGenerator,
      @Qualifier("dokarkiv") HttpHeaderRestTemplate httpHeaderRestTemplate
  ) {
    httpHeaderRestTemplate
        .addHeaderGenerator(TokenForBasicAuthenticationGenerator.HEADER_NAV_CONSUMER_TOKEN, tokenForBasicAuthenticationGenerator::generateToken);

    return httpHeaderRestTemplate;
  }
}
