package no.nav.bidrag.dokument.arkiv.consumer;

import com.netflix.graphql.dgs.client.DefaultGraphQLClient;
import com.netflix.graphql.dgs.client.GraphQLError;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.HttpResponse;
import java.util.Arrays;
import java.util.List;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQuery;
import no.nav.bidrag.dokument.arkiv.dto.GraphQuery;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostQuery;
import no.nav.bidrag.dokument.arkiv.model.JournalIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.ReasonToHttpStatus;
import no.nav.bidrag.dokument.arkiv.model.SafException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class SafConsumer {

  private final static Logger LOGGER = LoggerFactory.getLogger(SafConsumer.class);

  private final RestTemplate restTemplate;

  public SafConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public Journalpost hentJournalpost(Integer journalpostId) {
    return consumeEnkelJournalpostQuery(new JournalpostQuery(journalpostId));
  }

  public List<Journalpost> finnJournalposter(String saksnummer, String fagomrade) {
    var response = consumeQuery(new DokumentoversiktFagsakQuery(saksnummer, fagomrade), this::journalIkkeFunnetException);
    return Arrays.asList(response.extractValueAsObject("dokumentoversiktFagsak.journalposter", Journalpost[].class));
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
    LOGGER.info(queryString);
    var graphQLClient = new DefaultGraphQLClient("");
    var response = graphQLClient.executeQuery(queryString, query.getVariables(), (url, headers, body) -> {
      ResponseEntity<String> exchange = restTemplate.exchange("/", HttpMethod.POST, new HttpEntity<>(body), String.class);
      return new HttpResponse(exchange.getStatusCodeValue(), exchange.getBody());
    });

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

      throw new SafException(message, reasonToHttpStatus.getStatus());
    }

    return response;
  }

  @FunctionalInterface
  private interface NotFoundException {

    RuntimeException init(String message);
  }
}
