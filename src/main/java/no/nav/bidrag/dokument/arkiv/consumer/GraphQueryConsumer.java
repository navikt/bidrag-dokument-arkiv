package no.nav.bidrag.dokument.arkiv.consumer;

import java.util.Map;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostQuery;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class GraphQueryConsumer {

  private final RestTemplate restTemplate;

  public GraphQueryConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public Optional<Map<String, Object>> hentJournalpost(Integer journalpostId) {
    var journalpostQuery = new JournalpostQuery(journalpostId);

    var jsonResponse = restTemplate.exchange(
        "/", HttpMethod.POST, new HttpEntity<>(journalpostQuery.writeQuery()), Map.class
    );

    //noinspection unchecked
    return Optional.ofNullable(jsonResponse.getBody());
  }
}
