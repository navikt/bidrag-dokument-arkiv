package no.nav.bidrag.dokument.arkiv.consumer;

import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig;
import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.TokenContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class JournalforingConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(JournalforingConsumer.class);

  private final OIDCRequestContextHolder oidcRequestContextHolder;
  private final RestTemplate restTemplate;

  public JournalforingConsumer(OIDCRequestContextHolder oidcRequestContextHolder, RestTemplate restTemplate) {
    this.oidcRequestContextHolder = oidcRequestContextHolder;
    this.restTemplate = restTemplate;
  }

  public Optional<GetJournalpostResponse> hentJournalforing(Integer id) {
    var journalforingDtoResponseEntity = Optional.ofNullable(
        restTemplate.exchange(
            "/journalposter/" + id,
            HttpMethod.GET,
            hentHttpEntityMedBearerToken(),
            GetJournalpostResponse.class
        )
    );

    journalforingDtoResponseEntity.ifPresent(responseEntity -> {
      var httpStatus = responseEntity.getStatusCode();
      LOGGER.info("JournalforingDto med id={} har http status {} - {}", id, httpStatus, httpStatus.getReasonPhrase());
    });

    return journalforingDtoResponseEntity.map(ResponseEntity::getBody);
  }

  private <T> HttpEntity<T> hentHttpEntityMedBearerToken() {

    var httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.AUTHORIZATION,
        "Bearer " + Optional.ofNullable(oidcRequestContextHolder)
            .map(OIDCRequestContextHolder::getOIDCValidationContext)
            .map(oidcValidationContext -> oidcValidationContext.getToken(BidragDokumentArkivConfig.ISSUER))
            .map(TokenContext::getIdToken)
            .orElseThrow(() -> new IllegalStateException("Kunne ikke videresende Bearer token"))
    );

    return new HttpEntity<>(null, httpHeaders);
  }
}
