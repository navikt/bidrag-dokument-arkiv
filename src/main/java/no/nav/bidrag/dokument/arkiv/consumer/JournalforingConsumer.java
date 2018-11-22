package no.nav.bidrag.dokument.arkiv.consumer;

import no.nav.bidrag.dokument.arkiv.dto.JournalforingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

public class JournalforingConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalforingConsumer.class);

    private final String baseUrl;

    public JournalforingConsumer(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Optional<JournalforingDto> hentJournalforing(Integer id) {
        RestTemplate restTemplate = RestTemplateFactory.create(baseUrl);
        ResponseEntity<JournalforingDto> journalforingDtoResponseEntity = restTemplate.getForEntity("/journalposter/" + id, JournalforingDto.class);
        HttpStatus httpStatus = journalforingDtoResponseEntity.getStatusCode();

        LOGGER.info("JournalforingDto med id={} har http status {} - {}", id, httpStatus, httpStatus.getReasonPhrase());

        return Optional.ofNullable(journalforingDtoResponseEntity.getBody());
    }
}
