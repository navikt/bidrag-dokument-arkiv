package no.nav.bidrag.dokument.arkiv.consumer;

import java.util.List;
import no.nav.bidrag.commons.web.HttpResponse;
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

  public HttpResponse<Journalpost> hentJournalpost(Integer journalpostId) {
    var journalpostQuery = new JournalpostQuery(journalpostId);
    var queryResponseEntity = consumeQuery(journalpostQuery.writeQuery());
    var httpHeaders = queryResponseEntity.getHeaders();

    if (queryResponseEntity.getBody() != null) {
      var dokumentoversiktFagsakQueryResponse = queryResponseEntity.getBody();
      var journalpost = dokumentoversiktFagsakQueryResponse.hentJournalpost(journalpostId);
      var status = queryResponseEntity.getStatusCode();

      return new HttpResponse<>(new ResponseEntity<>(journalpost, httpHeaders, status));
    }

    return HttpResponse.from(httpHeaders, queryResponseEntity.getStatusCode());
  }

  public HttpResponse<List<Journalpost>> finnJournalposter(String saksnummer, String fagomrade) {
    var dokumentoversiktFagsakQuery = new DokumentoversiktFagsakQuery(saksnummer, fagomrade);
    var queryResponseEntity = consumeQuery(dokumentoversiktFagsakQuery.writeQuery());

    if (queryResponseEntity.getBody() == null) {
      return HttpResponse.from(queryResponseEntity.getHeaders(), queryResponseEntity.getStatusCode());
    }

    return HttpResponse.from(
        queryResponseEntity.getBody().hentJournalposter(),
        queryResponseEntity.getHeaders(),
        queryResponseEntity.getStatusCode()
    );
  }

  private ResponseEntity<DokumentoversiktFagsakQueryResponse> consumeQuery(String query) {
    LOGGER.info(query);

    return restTemplate.exchange(
        "/", HttpMethod.POST, new HttpEntity<>(query), DokumentoversiktFagsakQueryResponse.class
    );
  }
}
