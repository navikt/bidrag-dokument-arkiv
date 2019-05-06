package no.nav.bidrag.dokument.arkiv.consumer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQuery;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class GraphQueryConsumer {

  private final static Logger LOGGER = LoggerFactory.getLogger(GraphQueryConsumer.class);

  private final RestTemplate restTemplate;

  public GraphQueryConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public Optional<Map> hentJournalpost(Integer journalpostId) {
    var journalpostQuery = new JournalpostQuery(journalpostId);
    var jsonResponse = consumeQuery(journalpostQuery.writeQuery());

    return Optional.ofNullable(jsonResponse.getBody());
  }

  public List<Map> finnJournalposter(String saksnummer, String fagomrade) {
    var dokumentoversiktFagsakQuery = new DokumentoversiktFagsakQuery(saksnummer, fagomrade);
    var jsonResponse = consumeQuery(dokumentoversiktFagsakQuery.writeQuery());
    var body = jsonResponse.getBody();

    if (body == null) {
      return Collections.emptyList();
    }

    return List.of(body);
  }

  private ResponseEntity<Map> consumeQuery(String query) {
    LOGGER.info(query);

    return restTemplate.exchange(
        "/", HttpMethod.POST, new HttpEntity<>(query), Map.class
    );
  }
}
