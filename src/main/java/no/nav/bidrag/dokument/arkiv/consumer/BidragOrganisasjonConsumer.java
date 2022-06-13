package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.CacheConfig.GEOGRAFISK_ENHET_CACHE;
import static no.nav.bidrag.dokument.arkiv.CacheConfig.GEOGRAFISK_ENHET_WITH_TEMA_CACHE;
import static no.nav.bidrag.dokument.arkiv.CacheConfig.SAKSBEHANDLERINFO_CACHE;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.dto.GeografiskTilknytningResponse;
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerInfoResponse;
import org.apache.logging.log4j.util.Strings;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;

public class BidragOrganisasjonConsumer {

  public static final String ARBEIDSFORDELING_URL="/arbeidsfordeling/enhetsliste/geografisktilknytning/%s";
  public static final String SAKSBEHANDLER_INFO="/saksbehandler/info/%s";
  private final HttpHeaderRestTemplate restTemplate;

  public BidragOrganisasjonConsumer(HttpHeaderRestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Cacheable(value = GEOGRAFISK_ENHET_WITH_TEMA_CACHE, unless="#result == null")
  public String hentGeografiskEnhet(String personId, String tema){
    if (Strings.isEmpty(personId)){
      return null;
    }
    var arbeidsfordelingUrl = String.format(ARBEIDSFORDELING_URL, personId);
    if (Strings.isNotEmpty(tema)){
      arbeidsfordelingUrl = arbeidsfordelingUrl+"?tema="+tema;
    }
    var response = restTemplate.exchange(arbeidsfordelingUrl, HttpMethod.GET, null, GeografiskTilknytningResponse.class);
    return response.getBody() == null ? null : response.getBody().getEnhetIdent();
  }



  @Cacheable(SAKSBEHANDLERINFO_CACHE)
  public SaksbehandlerInfoResponse hentSaksbehandlerInfo(String saksbehandlerIdent){
    return restTemplate.exchange(String.format(SAKSBEHANDLER_INFO, saksbehandlerIdent), HttpMethod.GET, null, SaksbehandlerInfoResponse.class).getBody();
  }
}
