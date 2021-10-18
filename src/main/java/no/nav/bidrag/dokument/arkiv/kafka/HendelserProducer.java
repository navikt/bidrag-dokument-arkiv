package no.nav.bidrag.dokument.arkiv.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import no.nav.bidrag.dokument.arkiv.model.JournalpostHendelseException;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.bidrag.dokument.dto.JournalpostHendelse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class HendelserProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger(HendelserProducer.class);

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final JournalpostService journalpostService;
  private final String topic;
  private final Boolean enableProducer;

  public HendelserProducer(JournalpostService journalpostService, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
      String topic, Boolean enableProducer) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.journalpostService = journalpostService;
    this.topic = topic;
    this.enableProducer = enableProducer;
  }

  private JournalpostHendelse createJournalpostHendelse(Long journalpostId) {
    var journalpostOptional = journalpostService.hentJournalpostMedAktorId(journalpostId);
    if (journalpostOptional.isEmpty()) {
      throw new JournalpostIkkeFunnetException(String.format("Fant ikke journalpost med id %s", journalpostId));
    }

    var journalpost = journalpostOptional.get();
    JournalpostHendelse journalpostHendelse = new JournalpostHendelse();
    journalpostHendelse.setJournalpostId(journalpost.hentJournalpostIdMedPrefix());
    journalpostHendelse.setJournalstatus(journalpost.getJournalstatus());
    journalpostHendelse.setEnhet(journalpost.getJournalforendeEnhet());
    journalpostHendelse.setFagomrade(journalpost.getTema());

    if (Objects.nonNull(journalpost.getBruker()) && Objects.nonNull(journalpost.getBruker().getId())) {
      var bruker = journalpost.getBruker();
      journalpostHendelse.setAktorId(bruker.getId());
    }
    return journalpostHendelse;

  }

  public void publishJournalpostUpdated(Long journalpostId){
      var journalpostHendelse = createJournalpostHendelse(journalpostId);
      publish(journalpostHendelse);
  }

  public void publish(JournalpostHendelse journalpostHendelse){
    try {
      LOGGER.info("Publiserer hendelse {}", objectMapper.writeValueAsString(journalpostHendelse));
      if (!enableProducer) {
        LOGGER.info("Sender ikke hendelse da ENABLE_HENDELSE_PRODUCER er satt til false");
        return;
      }
      kafkaTemplate.send(topic, journalpostHendelse.getJournalpostId(), objectMapper.writeValueAsString(journalpostHendelse));
    } catch (JsonProcessingException e) {
      throw new JournalpostHendelseException(e.getMessage(), e);
    }
  }
}
