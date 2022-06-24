package no.nav.bidrag.dokument.arkiv.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.model.DistribusjonFeiletFunksjoneltException;
import no.nav.bidrag.dokument.arkiv.model.DistribusjonFeiletTekniskException;
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse;
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class DokdistFordelingConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DokarkivConsumer.class);

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public DokdistFordelingConsumer(RestTemplate restTemplate, ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
  }


  public DistribuerJournalpostResponse distribuerJournalpost(Journalpost journalpost, String batchId, DistribuerTilAdresse adresse) {
    var journalpostId = journalpost.hentJournalpostIdLong();
    var request = new DokDistDistribuerJournalpostRequest(journalpostId, journalpost.hentBrevkode(), journalpost.hentTittel(), adresse, batchId);
    LOGGER.info("Bestiller distribusjon for journalpost {} med distribusjonstype {} og distribusjonstidspunkt {}{}",
        request.getJournalpostId(),
        request.getDistribusjonstype(),
        request.getDistribusjonstidspunkt(),
        Strings.isNotEmpty(batchId) ? String.format(" og batchId %s", batchId) : ""
    );

    try {
      var response = new HttpResponse<>(restTemplate.exchange("/rest/v1/distribuerjournalpost", HttpMethod.POST, new HttpEntity<>(request),
          DokDistDistribuerJournalpostResponse.class));
      var responseBody = response.getResponseEntity().getBody();
      if (!response.is2xxSuccessful() || responseBody == null) {
        return null;
      }
      return responseBody.toDistribuerJournalpostResponse(journalpostId);
    } catch (HttpStatusCodeException e) {
      var status = e.getStatusCode();
      var errorMessage = parseErrorMessage(e);
      if (HttpStatus.CONFLICT.equals(status)) {
        LOGGER.warn("Distribusjon er allerede bestillt for journalpost {}. Fortsetter behandling.", journalpostId);
        return conflictExceptionToResponse(journalpostId, e);
      }

      if (HttpStatus.BAD_REQUEST.equals(status) || HttpStatus.NOT_FOUND.equals(status)) {
        throw new DistribusjonFeiletFunksjoneltException(
            String.format("Distribusjon feilet for JOARK journalpost %s med status %s og feilmelding: %s", journalpostId, e.getStatusCode(),
                errorMessage));
      }

      throw new DistribusjonFeiletTekniskException(
          String.format("Distribusjon feilet teknisk for JOARK journalpost %s med status %s og feilmelding: %s", journalpostId, e.getStatusCode(),
              errorMessage), e);
    }
  }

  private DistribuerJournalpostResponse conflictExceptionToResponse(Long journalpostId, HttpStatusCodeException e){
    try {
      var response = objectMapper.readValue(e.getResponseBodyAsString(), DokDistDistribuerJournalpostResponse.class);
      return response.toDistribuerJournalpostResponse(journalpostId);
    } catch (Exception ex) {
      return new DistribuerJournalpostResponse(journalpostId.toString(), null);
    }
  }

  private String parseErrorMessage(HttpStatusCodeException e) {
    try {
      var jsonNode = objectMapper.readValue(e.getResponseBodyAsString(), JsonNode.class);
      if (jsonNode.has("message")) {
        return jsonNode.get("message").asText();
      }
      return e.getMessage();
    } catch (Exception ex) {
      return e.getMessage();
    }
  }

  public void leggTilInterceptor(ClientHttpRequestInterceptor requestInterceptor) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      restTemplate.getInterceptors().add(requestInterceptor);
    }
  }
}
