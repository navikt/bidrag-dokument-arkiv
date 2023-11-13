package no.nav.bidrag.dokument.arkiv.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig
import no.nav.bidrag.dokument.arkiv.SECURE_LOGGER
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.JournalpostHarIkkeKommetIRetur
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager
import no.nav.bidrag.dokument.arkiv.service.JournalpostService
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.env.Environment
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.util.backoff.ExponentialBackOff
import java.time.Duration

@Configuration
class BidragDokumentArkivKafkaConfig {
    @Bean
    @Lazy
    fun hendelserProducer(
        kafkaTemplate: KafkaTemplate<String, String>,
        objectMapper: ObjectMapper,
        saksbehandlerInfoManager: SaksbehandlerInfoManager,
        @Value("\${TOPIC_JOURNALPOST}") topic: String,
        journalpostServices: ResourceByDiscriminator<JournalpostService>,
    ): HendelserProducer {
        return HendelserProducer(
            journalpostServices.get(Discriminator.SERVICE_USER),
            kafkaTemplate,
            objectMapper,
            topic,
            saksbehandlerInfoManager,
        )
    }

    @Bean
    fun defaultErrorHandler(@Value("\${KAFKA_MAX_RETRY:-1}") maxRetry: Int): DefaultErrorHandler {
        // Max retry should not be set in production
        val backoffPolicy =
            if (maxRetry == -1) ExponentialBackOff() else ExponentialBackOffWithMaxRetries(maxRetry)
        backoffPolicy.multiplier = 2.0
        backoffPolicy.maxInterval = 1800000L // 30 mins
        LOGGER.info(
            "Initializing Kafka errorhandler with backoffpolicy {}, maxRetry={}",
            backoffPolicy,
            maxRetry,
        )
        val errorHandler = DefaultErrorHandler(
            { rec: ConsumerRecord<*, *>, e: Exception? ->
                val key = rec.key()
                val value = rec.value()
                val offset = rec.offset()
                val topic = rec.topic()
                val partition = rec.topic()
                SECURE_LOGGER.error(
                    "Kafka melding med nøkkel {}, partition {} og topic {} feilet på offset {}. Melding som feilet: {}",
                    key,
                    partition,
                    topic,
                    offset,
                    value,
                    e,
                )
            },
            backoffPolicy,
        )
        errorHandler.addNotRetryableExceptions(JournalpostHarIkkeKommetIRetur::class.java)
        return errorHandler
    }

    @Bean
    fun oppgaveKafkaListenerContainerFactory(
        oppgaveConsumerFactory: ConsumerFactory<Long, String>,
        defaultErrorHandler: DefaultErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<Long, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<Long, String>()
        factory.consumerFactory = oppgaveConsumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        // Retry consumer/listener even if authorization fails
        factory.setContainerCustomizer { container: ConcurrentMessageListenerContainer<Long, String> ->
            container.containerProperties.authExceptionRetryInterval = Duration.ofSeconds(10)
        }
        factory.setCommonErrorHandler(defaultErrorHandler)
        return factory
    }

    @Bean
    fun oppgaveConsumerFactory(
        @Value("\${KAFKA_BROKERS}") boostrapServer: String,
        @Value("\${KAFKA_KEYSTORE_PATH}") keystorePath: String,
        @Value("\${KAFKA_TRUSTSTORE_PATH}") trustStorePath: String,
        @Value("\${KAFKA_CREDSTORE_PASSWORD}") credstorePassword: String,
        environment: Environment,
    ): ConsumerFactory<Long, String> {
        val props = mutableMapOf<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = boostrapServer
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            ErrorHandlingDeserializer::class.java
        props["spring.deserializer.key.delegate.class"] = LongDeserializer::class.java
        props["spring.deserializer.value.delegate.class"] = StringDeserializer::class.java
        if (environment.activeProfiles.none { it.contains(BidragDokumentArkivConfig.PROFILE_KAFKA_TEST) }) {
            props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
            props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = trustStorePath
            props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credstorePassword

            props[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = keystorePath
            props[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
            props[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credstorePassword
        }
        return DefaultKafkaConsumerFactory(props)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(HendelseListener::class.java)
    }
}
