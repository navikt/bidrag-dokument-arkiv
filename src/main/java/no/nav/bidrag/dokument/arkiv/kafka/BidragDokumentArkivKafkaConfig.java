package no.nav.bidrag.dokument.arkiv.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv;
import no.nav.bidrag.dokument.arkiv.FeatureToggle;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.security.SecurityConfig.SaksbehandlerOidcTokenManager;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;

@Configuration
@EnableKafka
public class BidragDokumentArkivKafkaConfig {

  @Bean
  public HendelserProducer hendelserProducer(
      KafkaTemplate<String, String> kafkaTemplate,
      ObjectMapper objectMapper,
      SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManager,
      @Value("${TOPIC_JOURNALPOST}") String topic,
      FeatureToggle featureToggle,
      ResourceByDiscriminator<JournalpostService> journalpostServices
  ) {
    return new HendelserProducer(
        journalpostServices.get(Discriminator.SERVICE_USER),
        kafkaTemplate,
        objectMapper,
        topic,
        featureToggle, saksbehandlerOidcTokenManager
    );
  }

  @Bean
  public ConsumerFactory<String, Object> consumerFactory(KafkaProperties properties) {
    return new DefaultKafkaConsumerFactory<>(properties.buildConsumerProperties());
  }

  @Bean
  public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> kafkaListenerContainerFactory(
          ConsumerFactory<String, Object> consumerFactory
  ) {
    var concurrentKafkaListenerContainerFactory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
    concurrentKafkaListenerContainerFactory.setConsumerFactory(consumerFactory);

    return concurrentKafkaListenerContainerFactory;
  }

  @Bean
  public KafkaListenerErrorHandler hendelseErrorHandler() {
    return new HendelseErrorHandler(new ExceptionLogger(BidragDokumentArkiv.class.getSimpleName()));
  }
}
