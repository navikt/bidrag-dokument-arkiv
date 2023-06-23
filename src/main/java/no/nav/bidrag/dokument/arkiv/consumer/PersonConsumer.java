package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;
import static no.nav.bidrag.dokument.arkiv.CacheConfig.PERSON_ADRESSE_CACHE;
import static no.nav.bidrag.dokument.arkiv.CacheConfig.PERSON_CACHE;

import java.util.Optional;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.model.PersonException;
import no.nav.bidrag.domain.ident.PersonIdent;
import no.nav.bidrag.transport.person.PersonAdresseDto;
import no.nav.bidrag.transport.person.PersonDto;
import no.nav.bidrag.transport.person.PersonRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class PersonConsumer {

  private final RestTemplate restTemplate;
  private static final Logger LOGGER = LoggerFactory.getLogger(PersonConsumer.class);

  public PersonConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Cacheable(value = PERSON_CACHE, unless = "#result==null")
  @Retryable(
      retryFor = Exception.class,
      maxAttempts = 5,
      backoff = @Backoff(delay = 500, maxDelay = 3000, multiplier = 2.0))
  public Optional<PersonDto> hentPerson(String id) {
    try {
      var personResponse =
          restTemplate.exchange(
              "/informasjon",
              HttpMethod.POST,
              new HttpEntity<>(new PersonRequest(new PersonIdent(id))),
              PersonDto.class);
      if (HttpStatus.NO_CONTENT == personResponse.getStatusCode()) {
        return Optional.empty();
      }

      return Optional.ofNullable(personResponse.getBody());
    } catch (HttpStatusCodeException e) {
      LOGGER.warn("Det skjedde en feil ved henting av person", e);
      SECURE_LOGGER.warn("Det skjedde en feil ved henting av person {}", id, e);
      throw new PersonException("Det skjedde en feil ved henting av person");
    }
  }

  @Cacheable(value = PERSON_ADRESSE_CACHE, unless = "#result == null")
  public PersonAdresseDto hentAdresse(String id) {
    return restTemplate
        .exchange(
            "/adresse/post",
            HttpMethod.POST,
            new HttpEntity<>(new PersonRequest(new PersonIdent(id))),
            PersonAdresseDto.class)
        .getBody();
  }

  public void leggTilInterceptor(ClientHttpRequestInterceptor requestInterceptor) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      restTemplate.getInterceptors().add(requestInterceptor);
    }
  }
}
