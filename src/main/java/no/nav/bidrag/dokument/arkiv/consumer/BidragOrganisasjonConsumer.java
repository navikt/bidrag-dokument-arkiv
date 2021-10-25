package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.CacheConfig.GEOGRAFISK_ENHET_CACHE;
import static no.nav.bidrag.dokument.arkiv.CacheConfig.SAKSBEHANDLERINFO_CACHE;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.dto.GeografiskTilknytningResponse;
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerInfoResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;

public class BidragOrganisasjonConsumer {

  public static final String ARBEIDSFORDELING_URL="/arbeidsfordeling/enhetsliste/geografisktilknytning/%s";
  public static final String SAKSBEHANDLER_INFO="/saksbehandler/info/%s";
  private final HttpHeaderRestTemplate restTemplate;

  public BidragOrganisasjonConsumer(HttpHeaderRestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Cacheable(GEOGRAFISK_ENHET_CACHE)
  public HttpResponse<GeografiskTilknytningResponse> hentGeografiskEnhet(String personId){
      var response = restTemplate.exchange(String.format(ARBEIDSFORDELING_URL, personId), HttpMethod.GET, null, GeografiskTilknytningResponse.class);
      return new HttpResponse<>(response);
  }

  @Cacheable(SAKSBEHANDLERINFO_CACHE)
  public HttpResponse<SaksbehandlerInfoResponse> hentSaksbehandlerInfo(String saksbehandlerIdent){
    var response = restTemplate.exchange(String.format(SAKSBEHANDLER_INFO, saksbehandlerIdent), HttpMethod.GET, null, SaksbehandlerInfoResponse.class);
    return new HttpResponse<>(response);
  }
}
