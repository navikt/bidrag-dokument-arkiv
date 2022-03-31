package no.nav.bidrag.dokument.arkiv.hendelser

import com.fasterxml.jackson.databind.ObjectMapper
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_KAFKA_TEST
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
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
import java.util.Collections

@SpringBootTest(classes = [BidragDokumentArkivLocal::class])
@ActiveProfiles(value = [PROFILE_KAFKA_TEST, PROFILE_TEST, BidragDokumentArkivLocal.PROFILE_INTEGRATION])
@DisplayName("OppgaveEndretHendelseListenerTest")
@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
@EmbeddedKafka(partitions = 1, brokerProperties = ["listeners=PLAINTEXT://localhost:9092", "port=9092"], topics = ["topic_joark", "topic_journalpost"])
abstract class BaseKafkaHendelseTest {

    @Autowired
    lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    @Autowired
    lateinit var objectMapper: ObjectMapper;

    @Value("\${TOPIC_JOURNALPOST}")
    protected val topicJournalpost: String? = null
    @Value("\${TOPIC_JOURNALFOERING}")
    protected val topicJoark: String? = null

    fun sendMessageToJoarkTopic(joarkHendelseRecord: JournalfoeringHendelseRecord){
        val producer = configureProducer()
        producer!!.send(ProducerRecord(topicJoark, joarkHendelseRecord))
        producer.flush()
    }

    fun readFromJournalpostTopic(): JournalpostHendelse? {
        return try {
            val singleRecord = KafkaTestUtils.getSingleRecord(configureConsumer(topicJournalpost!!), topicJournalpost)
            Assertions.assertThat(singleRecord).isNotNull
            objectMapper.readValue(singleRecord.value(), JournalpostHendelse::class.java)
        } catch (e: Exception) {
            null
        }

    }

    fun configureConsumer(topic: String): Consumer<Int, String>? {
        val consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker)
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"
        val consumer: Consumer<Int, String> = DefaultKafkaConsumerFactory<Int, String>(consumerProps)
            .createConsumer()
        consumer.subscribe(Collections.singleton(topic))
        return consumer
    }

    private fun configureProducer(): Producer<Int, JournalfoeringHendelseRecord>? {
        val props = KafkaTestUtils.producerProps(embeddedKafkaBroker)
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringSerializer"
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = "io.confluent.kafka.serializers.KafkaAvroSerializer"
        props[KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://testUrl";
        props[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = "true";
        val producerProps: Map<String, Any> = HashMap(props)
        return DefaultKafkaProducerFactory<Int, JournalfoeringHendelseRecord>(producerProps).createProducer()
    }

}