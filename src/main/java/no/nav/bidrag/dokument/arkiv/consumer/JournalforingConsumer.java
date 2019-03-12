package no.nav.bidrag.dokument.arkiv.consumer;

import java.util.Optional;
import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class JournalforingConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(JournalforingConsumer.class);

  private final RestTemplate restTemplate;

  public JournalforingConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public Optional<GetJournalpostResponse> hentJournalforing(Integer id) {
    var journalforingDtoResponseEntity = Optional.ofNullable(
        restTemplate.exchange(
            "/journalposter/" + id,
            HttpMethod.GET,
            null,
            GetJournalpostResponse.class
        )
    );

    journalforingDtoResponseEntity.ifPresent(responseEntity -> {
      var httpStatus = responseEntity.getStatusCode();
      LOGGER.info("JournalforingDto med id={} har http status {} - {}", id, httpStatus, httpStatus.getReasonPhrase());
    });

    return journalforingDtoResponseEntity.map(ResponseEntity::getBody);
  }
}
