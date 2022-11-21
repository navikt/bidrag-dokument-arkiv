package no.nav.bidrag.dokument.arkiv.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterDistribusjonsInfoRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.JoarkOpprettJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.JoarkOpprettJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.model.OppdaterJournalpostFeiletFunksjoneltException;
import no.nav.bidrag.dokument.arkiv.model.OppdaterJournalpostFeiletTekniskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class DokarkivConsumer extends AbstractConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger(DokarkivConsumer.class);
  private final ObjectMapper objectMapper;
  public static final String URL_JOURNALPOSTAPI_V1 = "/rest/journalpostapi/v1/journalpost";
  public static final String URL_JOURNALPOSTAPI_V1_FEILREGISTRER = "/rest/journalpostapi/v1/journalpost/%s/feilregistrer";

  public DokarkivConsumer(RestTemplate restTemplate, ObjectMapper objectMapper) {
    super(restTemplate);
    this.objectMapper = objectMapper;
  }
  @Retryable(value = OppdaterJournalpostFeiletTekniskException.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, maxDelay = 5000, multiplier = 2.0))
  public OppdaterJournalpostResponse endre(OppdaterJournalpostRequest oppdaterJournalpostRequest) {
    var oppdaterJoarnalpostApiUrl = URL_JOURNALPOSTAPI_V1 + '/' + oppdaterJournalpostRequest.hentJournalpostId();
    try {
      var response = restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PUT, new HttpEntity<>(oppdaterJournalpostRequest), OppdaterJournalpostResponse.class);
      LOGGER.info("Endret journalpost {} med respons {}", oppdaterJournalpostRequest.hentJournalpostId(), response.getStatusCode());
      return response.getBody();
    } catch (HttpStatusCodeException e){
      var status = e.getStatusCode();
      var errorMessage = parseErrorMessage(e);
      if (HttpStatus.BAD_REQUEST.equals(status) || HttpStatus.NOT_FOUND.equals(status)){
        throw new OppdaterJournalpostFeiletFunksjoneltException(String.format("Oppdatering av journalpost %s feilet med status %s og feilmelding: %s", oppdaterJournalpostRequest.hentJournalpostId(), e.getStatusCode(), errorMessage));
      }
      throw new OppdaterJournalpostFeiletTekniskException(String.format("Oppdatering av journalpost %s feilet med status %s og feilmelding: %s", oppdaterJournalpostRequest.hentJournalpostId(), e.getStatusCode(), errorMessage), e);
    }
  }

  public JoarkOpprettJournalpostResponse opprett(JoarkOpprettJournalpostRequest joarkOpprettJournalpostRequest, boolean ferdigstill){
    try {
      var response = restTemplate.exchange(URL_JOURNALPOSTAPI_V1+ String.format("?forsoekFerdigstill=%s", ferdigstill ? "true" : "false"), HttpMethod.POST, new HttpEntity<>(joarkOpprettJournalpostRequest), JoarkOpprettJournalpostResponse.class);
      var responseBody = response.getBody();
      LOGGER.info("Opprettet journalpost {} med status {}", responseBody.getJournalpostId(), responseBody.getJournalstatus());
      return response.getBody();
    } catch (HttpClientErrorException clientErrorException){
      if (clientErrorException.getStatusCode() == HttpStatus.CONFLICT){
        LOGGER.info("Journalpost med eksternReferanseId {} er allerede arkivert i Joark", joarkOpprettJournalpostRequest.getEksternReferanseId());
        return handleConflictResponse(clientErrorException);
      }
      throw clientErrorException;
    }
  }

  public HttpResponse<Void> ferdigstill(FerdigstillJournalpostRequest ferdigstillJournalpostRequest) {
    var oppdaterJoarnalpostApiUrl = URL_JOURNALPOSTAPI_V1 + '/' + ferdigstillJournalpostRequest.getJournalpostId() + "/ferdigstill";
    var response = restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, new HttpEntity<>(ferdigstillJournalpostRequest), Void.class);
    return new HttpResponse<>(response);

  }

  public HttpResponse<Void> feilregistrerSakstilknytning(Long journalpostId) {
    try {
      var oppdaterJoarnalpostApiUrl = String.format(URL_JOURNALPOSTAPI_V1_FEILREGISTRER + "/feilregistrerSakstilknytning", journalpostId);
      var response = restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, null, Void.class);
      LOGGER.info("Sakstilknytning til journalpost {} ble feilregistrert", journalpostId);
      return new HttpResponse<>(response);
    } catch (HttpStatusCodeException e){
      var erSakstilknytningAlleredeFeilregistrert = e.getStatusCode().equals(HttpStatus.BAD_REQUEST);
      if (erSakstilknytningAlleredeFeilregistrert){
        return HttpResponse.from(HttpStatus.OK);
      }
      throw e;
    }
  }

  public HttpResponse<Void> oppdaterDistribusjonsInfo(Long journalpostId, boolean settStatusEkspedert, JournalpostKanal utsendingsKanal) {
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

  public HttpResponse<Void> opphevFeilregistrerSakstilknytning(Long journalpostId) {
    var oppdaterJoarnalpostApiUrl = String.format(URL_JOURNALPOSTAPI_V1_FEILREGISTRER + "/opphevFeilregistrertSakstilknytning", journalpostId);
    var response = restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, null, Void.class);
    return new HttpResponse<>(response);
  }
  private JoarkOpprettJournalpostResponse handleConflictResponse(HttpClientErrorException clientErrorException){
    return convertStringToResponse(clientErrorException.getResponseBodyAsString());
  }
  private JoarkOpprettJournalpostResponse convertStringToResponse(String responseString){
    try {
      return objectMapper.readValue(responseString, JoarkOpprettJournalpostResponse.class);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

}
