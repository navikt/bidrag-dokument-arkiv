package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.CacheConfig.PERSON_ADRESSE_CACHE;
import static no.nav.bidrag.dokument.arkiv.CacheConfig.PERSON_CACHE;

import java.util.Objects;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.dto.HentPostadresseRequest;
import no.nav.bidrag.dokument.arkiv.dto.HentPostadresseResponse;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

public class PersonConsumer {
  private final RestTemplate restTemplate;

  public PersonConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Cacheable(PERSON_CACHE)
  public HttpResponse<PersonResponse> hentPerson(String id){
    var personResponse =  restTemplate.exchange(String.format("/informasjon/%s", id), HttpMethod.GET, null, PersonResponse.class);
    return new HttpResponse<>(personResponse);
  }

  @Cacheable(value = PERSON_ADRESSE_CACHE, unless = "#result == null")
  public HentPostadresseResponse hentAdresse(String id){
    return restTemplate.exchange("/adresse/post", HttpMethod.POST, new HttpEntity<>(new HentPostadresseRequest(id)), HentPostadresseResponse.class).getBody();
  }

  public void leggTilInterceptor(ClientHttpRequestInterceptor requestInterceptor) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      restTemplate.getInterceptors().add(requestInterceptor);
    }
  }
}
