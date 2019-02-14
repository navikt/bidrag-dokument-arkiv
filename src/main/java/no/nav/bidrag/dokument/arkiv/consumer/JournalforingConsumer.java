package no.nav.bidrag.dokument.arkiv.consumer;

import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

public class JournalforingConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalforingConsumer.class);

    private final RestTemplate restTemplate;

    public JournalforingConsumer(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<GetJournalpostResponse> hentJournalforing(Integer id) {
        ResponseEntity<GetJournalpostResponse> journalforingDtoResponseEntity = restTemplate.getForEntity("/journalposter/" + id, GetJournalpostResponse.class);
        HttpStatus httpStatus = journalforingDtoResponseEntity.getStatusCode();

        LOGGER.info("JournalforingDto med id={} har http status {} - {}", id, httpStatus, httpStatus.getReasonPhrase());

        return Optional.ofNullable(journalforingDtoResponseEntity.getBody());
    }
}
