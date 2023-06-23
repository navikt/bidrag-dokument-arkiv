package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.CacheConfig.SAKSBEHANDLERINFO_CACHE;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerInfoResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;

public class BidragOrganisasjonConsumer {
  public static final String SAKSBEHANDLER_INFO = "/saksbehandler/info/%s";
  private final HttpHeaderRestTemplate restTemplate;

  public BidragOrganisasjonConsumer(HttpHeaderRestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Cacheable(SAKSBEHANDLERINFO_CACHE)
  public SaksbehandlerInfoResponse hentSaksbehandlerInfo(String saksbehandlerIdent) {
    return restTemplate
        .exchange(
            String.format(SAKSBEHANDLER_INFO, saksbehandlerIdent),
            HttpMethod.GET,
            null,
            SaksbehandlerInfoResponse.class)
        .getBody();
  }
}
