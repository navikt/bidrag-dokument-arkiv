package no.nav.bidrag.dokument.arkiv.consumer;

import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class DokarkivConsumer {

  private static final String URL_JOURNALPOSTAPI_V1 = "/rest/journalpostapi/v1/journalpost";
  private static final String URL_JOURNALPOSTAPI_V1_FEILREGISTRER = "/rest/journalpostapi/v1/journalpost/%s/feilregistrer";

  private final RestTemplate restTemplate;

  public DokarkivConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public HttpResponse<OppdaterJournalpostResponse> endre(OppdaterJournalpostRequest oppdaterJournalpostRequest) {
    var oppdaterJoarnalpostApiUrl = URL_JOURNALPOSTAPI_V1 + '/' + oppdaterJournalpostRequest.hentJournalpostId();
    var oppdaterJournalpostResponseEntity = restTemplate.exchange(
        oppdaterJoarnalpostApiUrl, HttpMethod.PUT, new HttpEntity<>(oppdaterJournalpostRequest), OppdaterJournalpostResponse.class
    );

    return new HttpResponse<>(oppdaterJournalpostResponseEntity);
  }

  public void ferdigstill(FerdigstillJournalpostRequest ferdigstillJournalpostRequest) {
    var oppdaterJoarnalpostApiUrl = URL_JOURNALPOSTAPI_V1 + '/' + ferdigstillJournalpostRequest.getJournalpostId() + "/ferdigstill";
    restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, new HttpEntity<>(ferdigstillJournalpostRequest), Void.class);
  }

  public void settStatusUtgaar(Long journalpostId) {
    var oppdaterJoarnalpostApiUrl = String.format(URL_JOURNALPOSTAPI_V1_FEILREGISTRER + "/settStatusUtgår", journalpostId);
    restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, null, Void.class);
  }

  public void feilregistrerSakstilknytning(Long journalpostId) {
    var oppdaterJoarnalpostApiUrl = String.format(URL_JOURNALPOSTAPI_V1_FEILREGISTRER + "/feilregistrerSakstilknytning", journalpostId);
    restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, null, Void.class);
  }
}
