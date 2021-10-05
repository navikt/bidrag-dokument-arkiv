package no.nav.bidrag.dokument.arkiv.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.dokument.arkiv.model.JournalpostHendelse;
import no.nav.bidrag.dokument.arkiv.model.JournalpostHendelseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class HendelserProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger(HendelserProducer.class);

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final String topic;

  public HendelserProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.topic = topic;
  }

  public void publish(JournalpostHendelse journalpostHendelse) {
    try {
      LOGGER.info("Publiserer hendelse {}", objectMapper.writeValueAsString(journalpostHendelse));
      kafkaTemplate.send(topic, journalpostHendelse.getJournalpostId(), objectMapper.writeValueAsString(journalpostHendelse));
    } catch (JsonProcessingException e) {
      throw new JournalpostHendelseException(e.getMessage(), e);
    }
  }
}
