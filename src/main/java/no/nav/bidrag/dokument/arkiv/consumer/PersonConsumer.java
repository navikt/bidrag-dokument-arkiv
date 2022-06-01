package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;
import static no.nav.bidrag.dokument.arkiv.CacheConfig.PERSON_ADRESSE_CACHE;
import static no.nav.bidrag.dokument.arkiv.CacheConfig.PERSON_CACHE;

import java.util.Optional;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.dto.HentPostadresseRequest;
import no.nav.bidrag.dokument.arkiv.dto.HentPostadresseResponse;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class PersonConsumer {
  private final RestTemplate restTemplate;
  private static final Logger LOGGER = LoggerFactory.getLogger(PersonConsumer.class);

  public PersonConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Cacheable(value = PERSON_CACHE, unless = "#result==null")
  public Optional<PersonResponse> hentPerson(String id){
    SECURE_LOGGER.info("Henter person {}", id);
    try {
      var personResponse = hentPersonFraConsumer(id);
      if (HttpStatus.NO_CONTENT.equals(personResponse.getStatusCode())){
        return Optional.empty();
      }

      return Optional.ofNullable(personResponse.getBody());
    } catch (HttpStatusCodeException e){
      LOGGER.error("Det skjedde en feil ved henting av person", e);
      SECURE_LOGGER.error("Det skjedde en feil ved henting av person {}", id, e);
      return Optional.empty();
    }
  }

  public ResponseEntity<PersonResponse> hentPersonFraConsumer(String id){
    return getRetryTemplate().execute(arg -> restTemplate.exchange(String.format("/informasjon/%s", id), HttpMethod.GET, null, PersonResponse.class));
  }

  private RetryTemplate getRetryTemplate(){
    RetryTemplate retryTemplate = new RetryTemplate();

    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setMultiplier(2.0);
    backOffPolicy.setMaxInterval(3000L);
    backOffPolicy.setInitialInterval(500);
    retryTemplate.setBackOffPolicy(backOffPolicy);

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(5);
    retryTemplate.setRetryPolicy(retryPolicy);
    retryTemplate.setThrowLastExceptionOnExhausted(true);

    return retryTemplate;
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
