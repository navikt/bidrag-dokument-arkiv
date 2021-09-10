package no.nav.bidrag.dokument.arkiv.security;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER;

import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.dto.TokenException;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;

public class OidcTokenGenerator {

  private final TokenValidationContextHolder tokenValidationContextHolder;

  public OidcTokenGenerator(TokenValidationContextHolder tokenValidationContextHolder) {
    this.tokenValidationContextHolder = tokenValidationContextHolder;
  }

  @SuppressWarnings("unused") // metode-referanse sendes fra RestTemplateConfiguration
  public String fetchBearerToken() {
    return "Bearer " + fetchToken();
  }

  private String fetchToken() {
    return Optional.ofNullable(tokenValidationContextHolder)
        .map(TokenValidationContextHolder::getTokenValidationContext)
        .map(tokenValidationContext -> tokenValidationContext.getJwtTokenAsOptional(ISSUER))
        .map(Optional::get)
        .map(JwtToken::getTokenAsString)
        .orElseThrow(() -> new TokenException("Kunne ikke videresende Bearer token"));
  }
}
