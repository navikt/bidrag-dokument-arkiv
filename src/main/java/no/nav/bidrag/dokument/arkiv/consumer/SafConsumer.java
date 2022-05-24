package no.nav.bidrag.dokument.arkiv.consumer;

import com.netflix.graphql.dgs.client.CustomGraphQLClient;
import com.netflix.graphql.dgs.client.GraphQLError;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.HttpResponse;
import java.util.Arrays;
import java.util.List;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost;
import no.nav.bidrag.dokument.arkiv.model.JournalIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.ReasonToHttpStatus;
import no.nav.bidrag.dokument.arkiv.model.SafException;
import no.nav.bidrag.dokument.arkiv.query.DokumentoversiktFagsakQuery;
import no.nav.bidrag.dokument.arkiv.query.GraphQuery;
import no.nav.bidrag.dokument.arkiv.query.JournalpostQuery;
import no.nav.bidrag.dokument.arkiv.query.TilknyttedeJournalposterQuery;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

public class SafConsumer {

  private final RestTemplate restTemplate;

  public SafConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public ResponseEntity<byte[]> hentDokument(Long journalpostId, Long dokumentReferanse){
      return this.restTemplate.exchange(String.format("/rest/hentdokument/%s/%s/ARKIV", journalpostId, dokumentReferanse), HttpMethod.GET, HttpEntity.EMPTY, byte[].class);
  }

  public Journalpost hentJournalpost(Long journalpostId) {
    return consumeEnkelJournalpostQuery(new JournalpostQuery(journalpostId));
  }

  public List<Journalpost> finnJournalposter(String saksnummer, String fagomrade) {
    var response = consumeQuery(new DokumentoversiktFagsakQuery(saksnummer, fagomrade), this::journalIkkeFunnetException);
    return Arrays.asList(response.extractValueAsObject("dokumentoversiktFagsak.journalposter", Journalpost[].class));
  }

  public List<TilknyttetJournalpost> finnTilknyttedeJournalposter(String dokumentInfoId){
    var response = consumeQuery(new TilknyttedeJournalposterQuery(dokumentInfoId), this::journalIkkeFunnetException);
    return Arrays.asList(response.extractValueAsObject("tilknyttedeJournalposter", TilknyttetJournalpost[].class));
  }

  private RuntimeException journalIkkeFunnetException(String message) {
    return new JournalIkkeFunnetException(message);
  }

  private Journalpost consumeEnkelJournalpostQuery(GraphQuery query) {
    GraphQLResponse response = consumeQuery(query, this::journalpostIkkeFunnetException);
    return response.extractValueAsObject("journalpost", Journalpost.class);
  }

  private RuntimeException journalpostIkkeFunnetException(String message) {
    return new JournalpostIkkeFunnetException(message);
  }

  private GraphQLResponse consumeQuery(GraphQuery query, NotFoundException notFoundException) {
    var queryString = query.getQuery();
    var graphQLClient = new CustomGraphQLClient("", (url, headers, body) -> {
      ResponseEntity<String> exchange = restTemplate.exchange("/graphql", HttpMethod.POST, new HttpEntity<>(body), String.class);
      return new HttpResponse(exchange.getStatusCodeValue(), exchange.getBody());
    });

    var response = graphQLClient.executeQuery(queryString, query.getVariables());

    if (response.hasErrors()) {
      var message = response.getErrors().stream()
          .findFirst()
          .map(GraphQLError::getMessage)
          .orElseThrow();

      var errorReason = response.getParsed().read("errors[0].extensions.code");
      var reasonToHttpStatus = new ReasonToHttpStatus(errorReason);

      if (reasonToHttpStatus.getStatus() == HttpStatus.NOT_FOUND) {
        throw notFoundException.init(message);
      }

      throw new SafException(String.format("Query %s (%s) feilet med feilmelding: %s", query.getQuery(), query.getVariables(), message), reasonToHttpStatus.getStatus());
    }

    return response;
  }

  @FunctionalInterface
  private interface NotFoundException {

    RuntimeException init(String message);
  }

  public void leggTilInterceptor(ClientHttpRequestInterceptor requestInterceptor) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      restTemplate.getInterceptors().add(requestInterceptor);
    }
  }
}
