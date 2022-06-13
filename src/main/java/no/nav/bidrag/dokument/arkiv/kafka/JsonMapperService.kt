package no.nav.bidrag.dokument.arkiv.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.arkiv.model.OppgaveHendelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JsonMapperService(private val objectMapper: ObjectMapper) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(JsonMapperService::class.java)
    }

    fun mapOppgaveHendelse(hendelse: String): OppgaveHendelse {
        return try {
            objectMapper.readValue(hendelse, OppgaveHendelse::class.java)
        } finally {
            LOGGER.debug("Leser hendelse: {}", hendelse)
        }
    }
}
