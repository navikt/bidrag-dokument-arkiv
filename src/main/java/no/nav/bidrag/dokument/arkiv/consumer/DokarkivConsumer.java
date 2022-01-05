package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.security.TokenForBasicAuthenticationGenerator.HEADER_NAV_CONSUMER_TOKEN;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterDistribusjonsInfoRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class DokarkivConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger(DokarkivConsumer.class);

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

  public HttpResponse<Void> oppdaterDistribusjonsInfo(Long journalpostId, boolean settStatusEkspedert, String utsendingsKanal) {
    var endpoint = URL_JOURNALPOSTAPI_V1 + "/%s";
    var oppdaterJoarnalpostApiUrl = String.format(endpoint + "/oppdaterDistribusjonsinfo", journalpostId);
    var request = new OppdaterDistribusjonsInfoRequest(settStatusEkspedert, utsendingsKanal);
    try {
      var response = restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, new HttpEntity<>(request), Void.class);
      return new HttpResponse<>(response);
    } catch (HttpClientErrorException clientErrorException){
      // Antar at status allerede er satt til ekspedert. Ignorer feil for å gjøre kallet idempotent
      if (clientErrorException.getStatusCode() == HttpStatus.BAD_REQUEST && clientErrorException.getResponseBodyAsString().contains("Kan ikke ekspedere journalpost med status E")){
        LOGGER.warn("Sett status til EKSPEDERT for journalpost {} feilet med melding {}", journalpostId, clientErrorException.getResponseBodyAsString());
        return HttpResponse.from(HttpStatus.OK);
      }
      throw clientErrorException;
    }
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
