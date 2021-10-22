package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.security.TokenForBasicAuthenticationGenerator.HEADER_NAV_CONSUMER_TOKEN;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class DokarkivConsumer {

  public static final String URL_JOURNALPOSTAPI_V1 = "/rest/journalpostapi/v1/journalpost";
  public static final String URL_JOURNALPOSTAPI_V1_FEILREGISTRER = "/rest/journalpostapi/v1/journalpost/%s/feilregistrer";

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

  public HttpResponse<Void> ferdigstill(FerdigstillJournalpostRequest ferdigstillJournalpostRequest) {
    var oppdaterJoarnalpostApiUrl = URL_JOURNALPOSTAPI_V1 + '/' + ferdigstillJournalpostRequest.getJournalpostId() + "/ferdigstill";
    var response = restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, new HttpEntity<>(ferdigstillJournalpostRequest), Void.class);
    return new HttpResponse<>(response);

  }

  public HttpResponse<Void> settStatusUtgaar(Long journalpostId) {
    var oppdaterJoarnalpostApiUrl = String.format(URL_JOURNALPOSTAPI_V1_FEILREGISTRER + "/settStatusUtgår", journalpostId);
    var response = restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, null, Void.class);
    return new HttpResponse<>(response);
  }

  public HttpResponse<Void> feilregistrerSakstilknytning(Long journalpostId) {
    var oppdaterJoarnalpostApiUrl = String.format(URL_JOURNALPOSTAPI_V1_FEILREGISTRER + "/feilregistrerSakstilknytning", journalpostId);
    var response = restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, null, Void.class);
    return new HttpResponse<>(response);
  }

  public void leggTilAuthorizationToken(HttpHeaderRestTemplate.ValueGenerator valueGenerator) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      ((HttpHeaderRestTemplate) restTemplate).addHeaderGenerator(HttpHeaders.AUTHORIZATION, valueGenerator);
    }
  }

  public void leggTilNavConsumerToken(HttpHeaderRestTemplate.ValueGenerator valueGenerator) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      ((HttpHeaderRestTemplate) restTemplate).addHeaderGenerator(HEADER_NAV_CONSUMER_TOKEN, valueGenerator);
    }
  }
}
