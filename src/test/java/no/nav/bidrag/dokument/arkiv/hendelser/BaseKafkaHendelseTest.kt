package no.nav.bidrag.dokument.arkiv.hendelser

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.Companion.PROFILE_KAFKA_TEST
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.Companion.PROFILE_TEST
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivTest
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.internals.MockRebalanceListener
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.ActiveProfiles
import java.time.Duration

@SpringBootTest(classes = [BidragDokumentArkivTest::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = [PROFILE_KAFKA_TEST, PROFILE_TEST, BidragDokumentArkivTest.PROFILE_INTEGRATION])
@DisplayName("OppgaveEndretHendelseListenerTest")
@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"],
    topics = ["topic_joark", "topic_journalpost", "oppgave-hendelse"],
)
abstract class BaseKafkaHendelseTest {
    private val LOGGER = LoggerFactory.getLogger(BaseKafkaHendelseTest::class.java)

    @Autowired
    lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Value("\${TOPIC_JOURNALPOST}")
    protected val topicJournalpost: String? = null

    @Value("\${TOPIC_JOURNALFOERING}")
    protected val topicJoark: String? = null

    @AfterEach
    fun resetMocks() {
        WireMock.reset()
        WireMock.resetToDefault()
    }

    fun sendMessageToJoarkTopic(joarkHendelseRecord: JournalfoeringHendelseRecord) {
        val producer = configureProducer()
        producer!!.send(ProducerRecord(topicJoark, joarkHendelseRecord))
        producer.flush()
    }

    fun readFromJournalpostTopic(): JournalpostHendelse? {
        val consumer = configureConsumer(topicJournalpost!!)
        return try {
            val singleRecord =
                KafkaTestUtils.getSingleRecord(consumer, topicJournalpost, Duration.ofMillis(4000))
            Assertions.assertThat(singleRecord).isNotNull
            objectMapper.readValue(singleRecord.value(), JournalpostHendelse::class.java)
        } catch (e: Exception) {
            LOGGER.error("Det skjedde en feil ved lesing av kafka melding", e)
            null
        } finally {
            consumer.close()
        }
    }

    fun configureConsumer(topic: String): Consumer<Int, String> {
        val consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker)
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] =
            "org.apache.kafka.common.serialization.StringDeserializer"
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            "org.apache.kafka.common.serialization.StringDeserializer"
        val consumer: Consumer<Int, String> =
            DefaultKafkaConsumerFactory<Int, String>(consumerProps)
                .createConsumer()
        consumer.subscribe(listOf(topic), MockRebalanceListener())
        return consumer
    }

    private fun configureProducer(): Producer<Int, JournalfoeringHendelseRecord>? {
        val props = KafkaTestUtils.producerProps(embeddedKafkaBroker)
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] =
            "org.apache.kafka.common.serialization.StringSerializer"
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] =
            "io.confluent.kafka.serializers.KafkaAvroSerializer"
        props[KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://testUrl"
        props[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = "true"
        val producerProps: Map<String, Any> = HashMap(props)
        return DefaultKafkaProducerFactory<Int, JournalfoeringHendelseRecord>(producerProps).createProducer()
    }
}
