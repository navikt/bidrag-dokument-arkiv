package no.nav.bidrag.dokument.arkiv;

import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.consumer.AccessTokenConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.GraphQueryConsumer;
import no.nav.bidrag.dokument.arkiv.security.TokenForBasicAuthenticationGenerator;
import no.nav.bidrag.dokument.arkiv.security.OidcTokenGenerator;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Configuration
public class BidragDokumentArkivConfig {

  public static final String ISSUER = "isso";
  public static final String PROFILE_LIVE = "live";
  public static final String PROFILE_TEST = "test";

  @Bean
  GraphQueryConsumer graphQueryConsumer(
      @Qualifier("saf") HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Value("${SAF_GRAPHQL_URL}") String baseUrl
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);

    return new GraphQueryConsumer(httpHeaderRestTemplate);
  }

  @Bean
  DokarkivConsumer dokarkivConsumer(
      @Qualifier("dokarkiv") HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Value("${DOKARKIV_URL}") String baseUrl
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));

    return new DokarkivConsumer(httpHeaderRestTemplate);
  }

  @Bean
  AccessTokenConsumer accessTokenConsumer(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Value("${ACCESS_TOKEN_URL}") String baseUrl
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_FORM_URLENCODED_VALUE);

    return new AccessTokenConsumer(httpHeaderRestTemplate);
  }

  @Bean
  CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Bean
  ExceptionLogger exceptionLogger() {
    return new ExceptionLogger(BidragDokumentArkiv.class.getSimpleName());
  }

  @Bean
  TokenForBasicAuthenticationGenerator basicAuthenticationTokenGenerator(
      AccessTokenConsumer accessTokenConsumer,
      @Value("SRV_BD_ARKIV_AUTH") String secretForServiceUserNotEncoded
  ) {
    return new TokenForBasicAuthenticationGenerator(accessTokenConsumer, secretForServiceUserNotEncoded);
  }

  @Bean
  OidcTokenGenerator oidcTokenGenerator(TokenValidationContextHolder tokenValidationContextHolder) {
    return new OidcTokenGenerator(tokenValidationContextHolder);
  }
}
