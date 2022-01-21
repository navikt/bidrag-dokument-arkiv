package no.nav.bidrag.dokument.arkiv.consumer;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostResponse;
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse;
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class DokdistFordelingConsumer {
  private final RestTemplate restTemplate;

  public DokdistFordelingConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  public DistribuerJournalpostResponse distribuerJournalpost(Long journalpostId, DistribuerTilAdresse adresse){
      var request = new DokDistDistribuerJournalpostRequest(journalpostId, adresse);
      var response = new HttpResponse<>(restTemplate.exchange("/rest/v1/distribuerjournalpost", HttpMethod.POST, new HttpEntity<>(request), DokDistDistribuerJournalpostResponse.class));
      var responseBody = response.getResponseEntity().getBody();
      if (!response.is2xxSuccessful() || responseBody == null){
        return null;
      }
      return responseBody.toDistribuerJournalpostResponse(journalpostId);
  }
  public void leggTilAuthorizationToken(HttpHeaderRestTemplate.ValueGenerator valueGenerator) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      ((HttpHeaderRestTemplate) restTemplate).addHeaderGenerator(HttpHeaders.AUTHORIZATION, valueGenerator);
    }
  }
}
