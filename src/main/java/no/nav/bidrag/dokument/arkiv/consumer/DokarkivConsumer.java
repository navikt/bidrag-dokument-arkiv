package no.nav.bidrag.dokument.arkiv.consumer;

import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.model.OppdaterJournalpostFeiletFunksjoneltException;
import no.nav.bidrag.dokument.arkiv.model.OppdaterJournalpostFeiletTekniskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class DokarkivConsumer extends AbstractConsumer{
  private static final Logger LOGGER = LoggerFactory.getLogger(DokarkivConsumer.class);

  public static final String URL_JOURNALPOSTAPI_V1 = "/rest/journalpostapi/v1/journalpost";
  public static final String URL_JOURNALPOSTAPI_V1_FEILREGISTRER = "/rest/journalpostapi/v1/journalpost/%s/feilregistrer";

  public DokarkivConsumer(RestTemplate restTemplate) {
    super(restTemplate);
  }

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

  public HttpResponse<Void> ferdigstill(FerdigstillJournalpostRequest ferdigstillJournalpostRequest) {
    var oppdaterJoarnalpostApiUrl = URL_JOURNALPOSTAPI_V1 + '/' + ferdigstillJournalpostRequest.getJournalpostId() + "/ferdigstill";
    var response = restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, new HttpEntity<>(ferdigstillJournalpostRequest), Void.class);
    return new HttpResponse<>(response);

  }

  public HttpResponse<Void> feilregistrerSakstilknytning(Long journalpostId) {
    try {
      var oppdaterJoarnalpostApiUrl = String.format(URL_JOURNALPOSTAPI_V1_FEILREGISTRER + "/feilregistrerSakstilknytning", journalpostId);
      var response = restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, null, Void.class);
      return new HttpResponse<>(response);
    } catch (HttpStatusCodeException e){
      var erSakstilknytningAlleredeFeilregistrert = e.getStatusCode().equals(HttpStatus.BAD_REQUEST);
      if (erSakstilknytningAlleredeFeilregistrert){
        return HttpResponse.from(HttpStatus.OK);
      }
      throw e;
    }
  }

  public HttpResponse<Void> opphevFeilregistrerSakstilknytning(Long journalpostId) {
    var oppdaterJoarnalpostApiUrl = String.format(URL_JOURNALPOSTAPI_V1_FEILREGISTRER + "/opphevFeilregistrertSakstilknytning", journalpostId);
    var response = restTemplate.exchange(oppdaterJoarnalpostApiUrl, HttpMethod.PATCH, null, Void.class);
    return new HttpResponse<>(response);
  }

}
