package no.nav.bidrag.dokument.arkiv.consumer;

import org.springframework.web.client.RestTemplate;

public class GraphQueryConsumer {

  private final RestTemplate restTemplate;

  public GraphQueryConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
}
