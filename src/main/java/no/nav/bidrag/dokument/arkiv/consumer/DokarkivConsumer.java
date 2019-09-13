package no.nav.bidrag.dokument.arkiv.consumer;

import no.nav.bidrag.commons.web.HttpStatusResponse;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.OpprettJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OpprettJournalpostResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class DokarkivConsumer {

  private static final String URL_JOURNALPOSTAPI_V1 = "/rest/journalpostapi/v1/journalpost";

  private final RestTemplate restTemplate;

  public DokarkivConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public HttpStatusResponse<OppdaterJournalpostResponse> endre(OppdaterJournalpostRequest oppdaterJournalpostRequest) {
    var oppdaterJoarnalpostApiUrl = URL_JOURNALPOSTAPI_V1 + '/' + oppdaterJournalpostRequest.getJournalpostId();

    var oppdaterJournalpostResponseEntity = restTemplate.exchange(
        oppdaterJoarnalpostApiUrl, HttpMethod.PUT, new HttpEntity<>(oppdaterJournalpostRequest.tilJournalpostApi()), OppdaterJournalpostResponse.class
    );

    return new HttpStatusResponse<>(oppdaterJournalpostResponseEntity.getStatusCode(), oppdaterJournalpostResponseEntity.getBody());
  }

  public HttpStatusResponse<OpprettJournalpostResponse> opprett(OpprettJournalpostRequest opprettJournalpostRequest) {
    var opprettJournalpostResponseEntity = restTemplate.exchange(
        URL_JOURNALPOSTAPI_V1,
        HttpMethod.POST,
        new HttpEntity<>(opprettJournalpostRequest),
        OpprettJournalpostResponse.class
    );

    return new HttpStatusResponse<>(opprettJournalpostResponseEntity.getStatusCode(), opprettJournalpostResponseEntity.getBody());
  }
}
