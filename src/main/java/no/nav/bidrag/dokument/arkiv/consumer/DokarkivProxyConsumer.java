package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.security.TokenForBasicAuthenticationGenerator.HEADER_NAV_CONSUMER_TOKEN;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakRequest;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class DokarkivProxyConsumer {

  public static final String URL_KNYTT_TIL_ANNEN_SAK = "/rest/journalpostapi/v1/journalpost/%s/knyttTilAnnenSak";

  private final RestTemplate restTemplate;

  public DokarkivProxyConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public HttpResponse<KnyttTilAnnenSakResponse> knyttTilSak(Long journalpostId, KnyttTilAnnenSakRequest knyttTilAnnenSakRequest) {
    var oppdaterJoarnalpostApiUrl = String.format(URL_KNYTT_TIL_ANNEN_SAK, journalpostId);
    var oppdaterJournalpostResponseEntity = restTemplate.exchange(
        oppdaterJoarnalpostApiUrl, HttpMethod.PUT, new HttpEntity<>(knyttTilAnnenSakRequest), KnyttTilAnnenSakResponse.class
    );

    return new HttpResponse<>(oppdaterJournalpostResponseEntity);
  }


  public void leggTilAuthorizationToken(HttpHeaderRestTemplate.ValueGenerator valueGenerator) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      ((HttpHeaderRestTemplate) restTemplate).addHeaderGenerator(HttpHeaders.AUTHORIZATION, valueGenerator);
    }
  }

  public void leggTilNavConsumerToken(HttpHeaderRestTemplate.ValueGenerator valueGenerator) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      ((HttpHeaderRestTemplate) restTemplate).addHeaderGenerator(HEADER_NAV_CONSUMER_TOKEN, valueGenerator);
    }
  }
}
