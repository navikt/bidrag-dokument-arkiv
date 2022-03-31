package no.nav.bidrag.dokument.arkiv.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.dokument.arkiv.FeatureToggle;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class BidragDokumentArkivKafkaConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(HendelseListener.class);

  @Bean
  @Lazy
  public HendelserProducer hendelserProducer(
      KafkaTemplate<String, String> kafkaTemplate,
      ObjectMapper objectMapper,
      SaksbehandlerInfoManager saksbehandlerInfoManager,
      @Value("${TOPIC_JOURNALPOST}") String topic,
      FeatureToggle featureToggle,
      ResourceByDiscriminator<JournalpostService> journalpostServices
  ) {
    return new HendelserProducer(
        journalpostServices.get(Discriminator.SERVICE_USER),
        kafkaTemplate,
        objectMapper,
        topic,
        featureToggle, saksbehandlerInfoManager
    );
  }

  @Bean
  public DefaultErrorHandler defaultErrorHandler() {
    return new DefaultErrorHandler((rec, e) -> {
      var key = rec.key();
      var value = rec.value();
      var offset = rec.offset();
      var topic =  rec.topic();
      var partition =  rec.topic();
      LOGGER.error("Kafka melding med nøkkel {}, partition {} og topic {} feilet på offset {}. Melding som feilet: {}", key, partition, topic, offset, value, e);
    }, new ExponentialBackOff());
  }
}
