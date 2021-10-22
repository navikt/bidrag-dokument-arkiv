package no.nav.bidrag.dokument.arkiv.security;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.EnvironmentProperties;
import no.nav.bidrag.dokument.arkiv.consumer.AccessTokenConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler;
import no.nav.bidrag.tilgangskontroll.SecurityUtils;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Configuration
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
@OpenAPIDefinition(
    info = @Info(title = "bidrag-dokument-arkiv", version = "v1"),
    security = @SecurityRequirement(name = "bearer-key")
)
@SecurityScheme(
    bearerFormat = "JWT",
    name = "bearer-key",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP
)
public class SecurityConfig {

  @Bean
  AccessTokenConsumer accessTokenConsumer(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      EnvironmentProperties environmentProperties
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.securityTokenUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_FORM_URLENCODED_VALUE);

    return new AccessTokenConsumer(httpHeaderRestTemplate);
  }

  @Bean
  TokenForBasicAuthenticationGenerator basicAuthenticationTokenGenerator(
      AccessTokenConsumer accessTokenConsumer,
      EnvironmentProperties environmentProperties
  ) {
    return new TokenForBasicAuthenticationGenerator(accessTokenConsumer, environmentProperties.secretForServiceUser);
  }

  @Bean
  OidcTokenGenerator oidcTokenGenerator(TokenValidationContextHolder tokenValidationContextHolder) {
    return new OidcTokenGenerator(tokenValidationContextHolder);
  }
}
