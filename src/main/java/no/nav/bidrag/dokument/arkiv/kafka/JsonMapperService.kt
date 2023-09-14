package no.nav.bidrag.dokument.arkiv.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.arkiv.kafka.dto.OppgaveKafkaHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JsonMapperService(private val objectMapper: ObjectMapper) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(JsonMapperService::class.java)
    }

    fun mapOppgaveHendelse(hendelse: String): OppgaveKafkaHendelse {
        return try {
            objectMapper.readValue(hendelse, OppgaveKafkaHendelse::class.java)
        } finally {
            LOGGER.debug("Leser hendelse: {}", hendelse)
        }
    }
}
