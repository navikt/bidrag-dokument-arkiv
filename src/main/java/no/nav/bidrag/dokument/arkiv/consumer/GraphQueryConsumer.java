package no.nav.bidrag.dokument.arkiv.consumer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQuery;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostQuery;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class GraphQueryConsumer {

  private final RestTemplate restTemplate;

  public GraphQueryConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public Optional<Map> hentJournalpost(Integer journalpostId) {
    var journalpostQuery = new JournalpostQuery(journalpostId);

    var jsonResponse = restTemplate.exchange(
        "/", HttpMethod.POST, new HttpEntity<>(journalpostQuery.writeQuery()), Map.class
    );

    return Optional.ofNullable(jsonResponse.getBody());
  }

  public List<Map> finnJournalposter(Integer saksnummer, String fagomrade) {
    var dokumentoversiktFagsakQuery = new DokumentoversiktFagsakQuery(saksnummer, fagomrade);

    var jsonResponse = restTemplate.exchange(
        "/", HttpMethod.POST, new HttpEntity<>(dokumentoversiktFagsakQuery.writeQuery()), Map.class
    );

    Map body = jsonResponse.getBody();

    if (body == null) {
      return Collections.emptyList();
    }

    return List.of(body);
  }
}
