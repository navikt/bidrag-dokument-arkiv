package no.nav.bidrag.dokument.arkiv.security;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER_STS;

import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.model.TokenException;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;

public class OidcTokenGenerator {

  private final TokenValidationContextHolder tokenValidationContextHolder;

  public OidcTokenGenerator(TokenValidationContextHolder tokenValidationContextHolder) {
    this.tokenValidationContextHolder = tokenValidationContextHolder;
  }

  @SuppressWarnings("unused") // metode-referanse sendes fra RestTemplateConfiguration
  public String getBearerToken() {
    return "Bearer " + fetchToken();
  }

  public Boolean isTokenIssuerSTS(){
    return Optional.ofNullable(tokenValidationContextHolder)
        .map(TokenValidationContextHolder::getTokenValidationContext)
        .map(TokenValidationContext::getIssuers)
        .map((issuers)-> issuers.contains(ISSUER_STS)).orElse(false);
  }

  public Optional<String> getToken() {
    return fetchTokenOptional();
  }

  private String fetchToken() {
    return fetchTokenOptional().orElseThrow(() -> new TokenException("Fant ingen Bearer token i kontekst"));
  }

  private Optional<String> fetchTokenOptional() {
    return Optional.ofNullable(tokenValidationContextHolder)
        .map(TokenValidationContextHolder::getTokenValidationContext)
        .map(TokenValidationContext::getFirstValidToken)
        .flatMap(token -> token.map(JwtToken::getTokenAsString));

  }
}
