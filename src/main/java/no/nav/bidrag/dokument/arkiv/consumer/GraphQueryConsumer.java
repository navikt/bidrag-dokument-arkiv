package no.nav.bidrag.dokument.arkiv.consumer;

import com.netflix.graphql.dgs.client.DefaultGraphQLClient;
import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQuery;
import no.nav.bidrag.dokument.arkiv.dto.GraphqlException;
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

  public Journalpost hentJournalpost(Integer journalpostId) {
    var journalpostQuery = new JournalpostQuery(journalpostId);
    return consumeEnkelJournalpostQuery(journalpostQuery.getQuery());
  }

  public List<Journalpost> finnJournalposter(String saksnummer, String fagomrade) {
    var dokumentoversiktFagsakQuery = new DokumentoversiktFagsakQuery(saksnummer, fagomrade);
    var response = consumeQuery(dokumentoversiktFagsakQuery.getQuery());
    return Arrays.asList(response.extractValueAsObject("dokumentoversiktFagsak.journalposter", Journalpost[].class));
  }

  private Journalpost consumeEnkelJournalpostQuery(String query) {
    GraphQLResponse response = consumeQuery(query);
    return response.extractValueAsObject("journalpost", Journalpost.class);
  }

  private GraphQLResponse consumeQuery(String query) {
    LOGGER.info(query);
    var graphQLClient = new DefaultGraphQLClient("");
    var response = graphQLClient.executeQuery(query, new HashMap<>(), (url, headers, body) -> {
      ResponseEntity<String> exchange = restTemplate.exchange("/", HttpMethod.POST, new HttpEntity<>(body), String.class);
      return new HttpResponse(exchange.getStatusCodeValue(), exchange.getBody());
    });

    if (response.hasErrors()){
      var error = response.getErrors().get(0);
      var errorReason = response.getParsed().read("errors[0].extensions.code");
      var errorReasonString = errorReason != null ? errorReason.toString() : "ukjent";
      throw new GraphqlException(error.getMessage(), errorReasonString);
    }
    return response;
  }
}
