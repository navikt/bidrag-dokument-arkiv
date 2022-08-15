package no.nav.bidrag.dokument.arkiv.consumer;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakRequest;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

public class DokarkivKnyttTilSakConsumer {

  public static final String URL_KNYTT_TIL_ANNEN_SAK = "/rest/journalpostapi/v1/journalpost/%s/knyttTilAnnenSak";

  private final RestTemplate restTemplate;

  public DokarkivKnyttTilSakConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public KnyttTilAnnenSakResponse knyttTilSak(Long journalpostId, KnyttTilAnnenSakRequest knyttTilAnnenSakRequest) {
    var oppdaterJoarnalpostApiUrl = String.format(URL_KNYTT_TIL_ANNEN_SAK, journalpostId);
    var oppdaterJournalpostResponseEntity = restTemplate.exchange(
        oppdaterJoarnalpostApiUrl, HttpMethod.PUT, new HttpEntity<>(knyttTilAnnenSakRequest), KnyttTilAnnenSakResponse.class
    );

    return oppdaterJournalpostResponseEntity.getBody();
  }

  public void leggTilInterceptor(ClientHttpRequestInterceptor requestInterceptor) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      restTemplate.getInterceptors().add(requestInterceptor);
    }
  }

}
