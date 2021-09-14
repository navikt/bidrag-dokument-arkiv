package no.nav.bidrag.dokument.arkiv.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.dokument.arkiv.model.JournalpostHendelse;
import no.nav.bidrag.dokument.arkiv.model.JournalpostHendelseException;
import org.springframework.kafka.core.KafkaTemplate;

public class HendelserProducer {

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
      kafkaTemplate.send(topic, journalpostHendelse.getJournalpostId(), objectMapper.writeValueAsString(journalpostHendelse));
    } catch (JsonProcessingException e) {
      throw new JournalpostHendelseException(e.getMessage(), e);
    }
  }
}
