package no.nav.bidrag.dokument.arkiv.kafka;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_KAFKA_TEST;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
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
      ResourceByDiscriminator<JournalpostService> journalpostServices
  ) {
    return new HendelserProducer(
        journalpostServices.get(Discriminator.SERVICE_USER),
        kafkaTemplate,
        objectMapper,
        topic,
        saksbehandlerInfoManager
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
      SECURE_LOGGER.error("Kafka melding med nøkkel {}, partition {} og topic {} feilet på offset {}. Melding som feilet: {}", key, partition, topic, offset, value, e);
    }, new ExponentialBackOff());
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<Long, String> oppgaveKafkaListenerContainerFactory(ConsumerFactory<Long, String> oppgaveConsumerFactory, DefaultErrorHandler defaultErrorHandler) {
    ConcurrentKafkaListenerContainerFactory<Long, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(oppgaveConsumerFactory);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    //Retry consumer/listener even if authorization fails
    factory.setContainerCustomizer(container -> container.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10)));
    factory.setCommonErrorHandler(defaultErrorHandler);
    return factory;
  }

  @Bean
  public ConsumerFactory<Long, String> oppgaveConsumerFactory(@Value("${KAFKA_BOOTSTRAP_SERVERS}")  String bootstrapServers,
      @Value("${KAFKA_GROUP_ID}") String  groupId,
      @Value("${NAV_TRUSTSTORE_PATH}")  String trustStorePath,
      @Value("${NAV_TRUSTSTORE_PASSWORD}")  String trustStorePassword,
      @Value("${SERVICE_USER_USERNAME}") String  username,
      @Value("${SERVICE_USER_PASSWORD}") String password, Environment environment)  {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put("spring.deserializer.key.delegate.class", LongDeserializer.class);
    props.put("spring.deserializer.value.delegate.class",  StringDeserializer.class);
    if (Arrays.stream(environment.getActiveProfiles()).noneMatch(val->val.contains(PROFILE_KAFKA_TEST))){
      props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";");
      props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
      props.put(SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
      props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStorePath);
      props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword);
    }
    return new DefaultKafkaConsumerFactory(props);
  }
}
