package no.nav.bidrag.dokument.arkiv.consumer;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakRequest;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakResponse;
import no.nav.bidrag.dokument.arkiv.model.KunneIkkeKnytteSakTilJournalpost;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class DokarkivKnyttTilSakConsumer {

  public static final String URL_KNYTT_TIL_ANNEN_SAK = "/rest/journalpostapi/v1/journalpost/%s/knyttTilAnnenSak";
  private final RestTemplate restTemplate;

  public DokarkivKnyttTilSakConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Retryable(value = KunneIkkeKnytteSakTilJournalpost.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, maxDelay = 5000, multiplier = 2.0))
  public KnyttTilAnnenSakResponse knyttTilSak(Long journalpostId, KnyttTilAnnenSakRequest knyttTilAnnenSakRequest) {
    try {
      var oppdaterJoarnalpostApiUrl = String.format(URL_KNYTT_TIL_ANNEN_SAK, journalpostId);
      var oppdaterJournalpostResponseEntity = restTemplate.exchange(
          oppdaterJoarnalpostApiUrl, HttpMethod.PUT, new HttpEntity<>(knyttTilAnnenSakRequest), KnyttTilAnnenSakResponse.class
      );

      return oppdaterJournalpostResponseEntity.getBody();
    } catch (HttpStatusCodeException e) {
      var message = String.format("Det skjedde en feil ved tilknytting av sak %s til journalpost %s med tema %s",
          knyttTilAnnenSakRequest.getFagsakId(), journalpostId, knyttTilAnnenSakRequest.getTema());
      throw new KunneIkkeKnytteSakTilJournalpost(message, e);
    }
  }

  public void leggTilInterceptor(ClientHttpRequestInterceptor requestInterceptor) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      restTemplate.getInterceptors().add(requestInterceptor);
    }
  }

}
