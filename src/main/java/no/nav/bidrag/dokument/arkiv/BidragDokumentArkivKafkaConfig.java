package no.nav.bidrag.dokument.arkiv;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@Profile("!" + PROFILE_TEST)
public class BidragDokumentArkivKafkaConfig {

  @Bean
  public HendelserProducer hendelserProducer(
      KafkaTemplate<String, String> kafkaTemplate,
      ObjectMapper objectMapper,
      @Value("${TOPIC_JOURNALPOST}") String topic
  ) {
    return new HendelserProducer(kafkaTemplate, objectMapper, topic);
  }
}
