package no.nav.bidrag.dokument.arkiv.consumer;

import org.springframework.web.client.RestTemplate;

public class AccessTokenConsumer {

  private static final String REST_TOKEN_ENDPOINT = "/v1/sts/token";

  private final RestTemplate restTemplate;

  public AccessTokenConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
}
