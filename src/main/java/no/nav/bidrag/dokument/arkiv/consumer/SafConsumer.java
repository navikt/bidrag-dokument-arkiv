package no.nav.bidrag.dokument.arkiv.consumer;

import com.netflix.graphql.dgs.client.DefaultGraphQLClient;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQuery;
import no.nav.bidrag.dokument.arkiv.dto.GraphQuery;
import no.nav.bidrag.dokument.arkiv.dto.SafException;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
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
    var response = consumeQuery(new DokumentoversiktFagsakQuery(saksnummer, fagomrade));
    return Arrays.asList(response.extractValueAsObject("dokumentoversiktFagsak.journalposter", Journalpost[].class));
  }

  private Journalpost consumeEnkelJournalpostQuery(GraphQuery query) {
    GraphQLResponse response = consumeQuery(query);
    return response.extractValueAsObject("journalpost", Journalpost.class);
  }

  private GraphQLResponse consumeQuery(GraphQuery query) {
    var queryString = query.getQuery();
    LOGGER.info(queryString);
    var graphQLClient = new DefaultGraphQLClient("");
    var response = graphQLClient.executeQuery(queryString, query.getVariables(), (url, headers, body) -> {
      ResponseEntity<String> exchange = restTemplate.exchange("/", HttpMethod.POST, new HttpEntity<>(body), String.class);
      return new HttpResponse(exchange.getStatusCodeValue(), exchange.getBody());
    });

    if (response.hasErrors()){
      var error = response.getErrors().get(0);
      var errorReason = response.getParsed().read("errors[0].extensions.code");
      var errorReasonString = errorReason != null ? errorReason.toString() : "ukjent";
      throw new SafException(error.getMessage(), errorReasonString);
    }
    return response;
  }
}
