package no.nav.bidrag.dokument.arkiv.kafka

import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal
import no.nav.bidrag.dokument.arkiv.model.JournalpostTema
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
@Profile(BidragDokumentArkivConfig.PROFILE_KAFKA_TEST, BidragDokumentArkivConfig.PROFILE_LIVE)
class HendelseListener(
    private val behandleJournalforingHendelseService: BehandleJournalforingHendelseService,
    private val behandleOppgaveHendelseService: BehandleOppgaveHendelseService,
    private val jsonMapperService: JsonMapperService
) {
    @KafkaListener(
        containerFactory = "oppgaveKafkaListenerContainerFactory",
        groupId = "\${NAIS_APP_NAME}",
        topics = ["\${TOPIC_OPPGAVE_HENDELSE}"]
    )
    fun lesOppgaveOpprettetHendelse(consumerRecord: ConsumerRecord<String?, String?>) {
        val oppgaveOpprettetHendelse =
            jsonMapperService.mapOppgaveHendelse(consumerRecord.value()!!)
        if (oppgaveOpprettetHendelse.erTemaBIDEllerFAR() &&
            oppgaveOpprettetHendelse.erReturOppgave() &&
            oppgaveOpprettetHendelse.erOppgaveOpprettetHendelse
        ) {
            LOGGER.info("Mottatt retur oppgave opprettet hendelse {}", oppgaveOpprettetHendelse)
            behandleOppgaveHendelseService.behandleReturOppgaveOpprettetHendelse(
                oppgaveOpprettetHendelse
            )
        }
    }

    @KafkaListener(groupId = "\${NAIS_APP_NAME}", topics = ["\${TOPIC_JOURNALFOERING}"])
    fun listenJournalforingHendelse(@Payload journalfoeringHendelseRecord: JournalfoeringHendelseRecord) {
        val journalpostTema = JournalpostTema(journalfoeringHendelseRecord)
        if (!journalpostTema.erOmhandlingAvBidrag()) {
            LOGGER.debug("JournalpostTema omhandler ikke bidrag")
            return
        }
        if (erOpprettetAvNKS(journalfoeringHendelseRecord)) {
            LOGGER.debug("Journalpost er opprettet av NKS. Stopper videre behandling")
            return
        }
        LOGGER.info("Mottok journalføringshendelse {}", journalfoeringHendelseRecord)
        behandleJournalforingHendelseService.behandleJournalforingHendelse(
            journalfoeringHendelseRecord
        )
    }

    private fun erOpprettetAvNKS(record: JournalfoeringHendelseRecord): Boolean {
        return JournalpostKanal.NAV_NO_CHAT.name == record.mottaksKanal
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(HendelseListener::class.java)
    }
}
