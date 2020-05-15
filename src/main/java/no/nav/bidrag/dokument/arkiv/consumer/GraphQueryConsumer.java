package no.nav.bidrag.dokument.arkiv.consumer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQuery;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQueryResponse;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
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

  public Optional<Journalpost> hentJournalpost(Integer journalpostId) {
    var journalpostQuery = new JournalpostQuery(journalpostId);
    var muligQueryResponseEntity = consumeQuery(journalpostQuery.writeQuery());

    return muligQueryResponseEntity
        .map(ResponseEntity::getBody)
        .map(dokumentoversiktFagsakQueryResponse -> dokumentoversiktFagsakQueryResponse.hentJournalpost(journalpostId));
  }

  public List<Journalpost> finnJournalposter(String saksnummer, String fagomrade) {
    var dokumentoversiktFagsakQuery = new DokumentoversiktFagsakQuery(saksnummer, fagomrade);
    var muligJsonResponse = consumeQuery(dokumentoversiktFagsakQuery.writeQuery());

    return muligJsonResponse
        .map(HttpEntity::getBody)
        .map(DokumentoversiktFagsakQueryResponse::hentJournalposter)
        .orElseGet(Collections::emptyList);
  }

  @SuppressWarnings("ConstantConditions")
  private Optional<ResponseEntity<DokumentoversiktFagsakQueryResponse>> consumeQuery(String query) {
    LOGGER.info(query.replace(" ", ""));

    return Optional.ofNullable(restTemplate.exchange(
        "/", HttpMethod.POST, new HttpEntity<>(query), DokumentoversiktFagsakQueryResponse.class
    ));
  }
}
