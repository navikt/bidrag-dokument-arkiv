package no.nav.bidrag.dokument.arkiv.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler
import no.nav.bidrag.dokument.arkiv.model.JournalpostHendelseException
import no.nav.bidrag.dokument.arkiv.model.JournalpostHendelseIntern
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager
import no.nav.bidrag.dokument.arkiv.service.JournalpostService
import no.nav.bidrag.dokument.dto.HendelseType
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable

open class HendelserProducer(
    open val journalpostService: JournalpostService,
    open val kafkaTemplate: KafkaTemplate<String, String>,
    open val objectMapper: ObjectMapper,
    open val topic: String,
    open val saksbehandlerInfoManager: SaksbehandlerInfoManager
) {
    fun publishJournalpostUpdated(journalpostId: Long, saksbehandlersEnhet: String?) {
        val journalpostHendelse = createJournalpostHendelse(journalpostId, saksbehandlersEnhet)
        publish(journalpostHendelse.copy(hendelseType = HendelseType.ENDRING))
    }

    fun publishJournalpostHendelse(journalpostHendelse: JournalpostHendelse) {
        publish(journalpostHendelse)
    }

    private fun createJournalpostHendelse(journalpostId: Long, saksbehandlersEnhet: String?): JournalpostHendelse {
        val journalpost = journalpostService.hentJournalpostMedTilknyttedeSaker(journalpostId)
            .orElseThrow { JournalpostIkkeFunnetException(String.format("Fant ikke journalpost med id %s", journalpostId)) }
        return createJournalpostHendelse(journalpost, saksbehandlersEnhet)
    }

    private fun createJournalpostHendelse(journalpost: Journalpost, saksbehandlersEnhet: String?): JournalpostHendelse {
        val saksbehandler = saksbehandlerInfoManager.hentSaksbehandler()
            ?: journalpost.hentJournalfortAvIdent()?.let { Saksbehandler(it, journalpost.journalfortAvNavn) }
            ?: Saksbehandler(null, "bidrag-dokument-arkiv")

        val saksbehandlerMedEnhet = saksbehandler.tilEnhet(saksbehandlersEnhet)
        return JournalpostHendelseIntern(journalpost, saksbehandlerMedEnhet, null).hentJournalpostHendelse()
    }

    @Retryable(value = [Exception::class], maxAttempts = 10, backoff = Backoff(delay = 1000, maxDelay = 12000, multiplier = 2.0))
    private fun publish(journalpostHendelse: JournalpostHendelse) {
        try {
            val message = objectMapper.writeValueAsString(journalpostHendelse)
            BidragDokumentArkiv.SECURE_LOGGER.info("Publiserer hendelse {}", message)
            LOGGER.info("Publiserer hendelse med journalpostId={}", journalpostHendelse.journalpostId)
            kafkaTemplate.send(topic, journalpostHendelse.journalpostId, message)
        } catch (e: JsonProcessingException) {
            throw JournalpostHendelseException(e.message!!, e)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(HendelserProducer::class.java)
    }
}
