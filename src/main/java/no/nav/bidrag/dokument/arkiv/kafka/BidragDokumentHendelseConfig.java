package no.nav.bidrag.dokument.arkiv.kafka;

import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;

@EnableKafka
@Configuration
public class BidragDokumentHendelseConfig {

  public static final String CLIENT_ID = "bidrag-dokument-hendelse";

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
