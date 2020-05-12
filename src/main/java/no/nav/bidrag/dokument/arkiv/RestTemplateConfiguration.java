package no.nav.bidrag.dokument.arkiv;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER;

import java.util.Optional;
import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
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
      TokenValidationContextHolder tokenValidationContextHolder,
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate
  ) {
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.AUTHORIZATION, () -> "Bearer " + fetchBearerToken(tokenValidationContextHolder));

    return httpHeaderRestTemplate;
  }

  @Bean
  @Qualifier("saf")
  @Scope("prototype")
  public HttpHeaderRestTemplate safRestTemplate(
      NavConsumerTokenGenerator navConsumerTokenGenerator,
      @Qualifier("dokarkiv") HttpHeaderRestTemplate httpHeaderRestTemplate
  ) {
    httpHeaderRestTemplate.addHeaderGenerator(NavConsumerTokenGenerator.HEADER_NAV_CONSUMER_TOKEN, navConsumerTokenGenerator::generateToken);

    return httpHeaderRestTemplate;
  }

  private String fetchBearerToken(TokenValidationContextHolder tokenValidationContextHolder) {
    return Optional.ofNullable(tokenValidationContextHolder)
        .map(TokenValidationContextHolder::getTokenValidationContext)
        .map(tokenValidationContext -> tokenValidationContext.getJwtTokenAsOptional(ISSUER))
        .map(Optional::get)
        .map(JwtToken::getTokenAsString)
        .orElseThrow(() -> new IllegalStateException("Kunne ikke videresende Bearer token"));
  }
}
