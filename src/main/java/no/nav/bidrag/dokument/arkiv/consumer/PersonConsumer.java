package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.CacheConfig.PERSON_CACHE;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

  public void leggTilSikkerhet(HttpHeaderRestTemplate.ValueGenerator valueGenerator) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      ((HttpHeaderRestTemplate) restTemplate).addHeaderGenerator(HttpHeaders.AUTHORIZATION, valueGenerator);
    }
  }
}
