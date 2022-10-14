package no.nav.bidrag.dokument.arkiv.kafka;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_KAFKA_TEST;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_LIVE;

import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal;
import no.nav.bidrag.dokument.arkiv.model.JournalpostTema;
import no.nav.bidrag.dokument.arkiv.model.OppgaveHendelse;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Profile({PROFILE_KAFKA_TEST, PROFILE_LIVE})
public class HendelseListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(HendelseListener.class);

  private final BehandleJournalforingHendelseService behandleJournalforingHendelseService;
  private final BehandleOppgaveHendelseService behandleOppgaveHendelseService;
  private final JsonMapperService jsonMapperService;

  public HendelseListener(BehandleJournalforingHendelseService behandleJournalforingHendelseService,
      BehandleOppgaveHendelseService behandleOppgaveHendelseService, JsonMapperService jsonMapperService){
    this.behandleJournalforingHendelseService = behandleJournalforingHendelseService;
    this.behandleOppgaveHendelseService = behandleOppgaveHendelseService;
    this.jsonMapperService = jsonMapperService;
  }

  @KafkaListener(containerFactory="oppgaveKafkaListenerContainerFactory", topics = "${TOPIC_OPPGAVE_OPPRETTET}")
  public void lesOppgaveOpprettetHendelse(ConsumerRecord<String, String> consumerRecord) {
    OppgaveHendelse oppgaveOpprettetHendelse = jsonMapperService.mapOppgaveHendelse(consumerRecord.value());

    if (oppgaveOpprettetHendelse.erTemaBIDEllerFAR() && oppgaveOpprettetHendelse.erReturOppgave()) {
      LOGGER.info("Mottatt retur oppgave opprettet hendelse {}", oppgaveOpprettetHendelse);
      behandleOppgaveHendelseService.behandleReturOppgaveOpprettetHendelse(oppgaveOpprettetHendelse);
    } else if (oppgaveOpprettetHendelse.erTemaBIDEllerFAR() && oppgaveOpprettetHendelse.erJournalforingOppgave()){
      LOGGER.info("Mottatt journalforing oppgave opprettet hendelse {}", oppgaveOpprettetHendelse);
      behandleOppgaveHendelseService.behandleJournalforingOppgaveOpprettetHendelse(oppgaveOpprettetHendelse);
    }
  }

  @KafkaListener(groupId = "bidrag-dokument-arkiv", topics = "${TOPIC_JOURNALFOERING}")
  public void listenJournalforingHendelse(@Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    JournalpostTema journalpostTema = new JournalpostTema(journalfoeringHendelseRecord);
    if (!journalpostTema.erOmhandlingAvBidrag()) {
      LOGGER.debug("JournalpostTema omhandler ikke bidrag");
      return;
    }

    if (erOpprettetAvNKS(journalfoeringHendelseRecord)){
      LOGGER.debug("Journalpost er opprettet av NKS. Stopper videre behandling");
      return;
    }

    LOGGER.info("Mottok journalføringshendelse {}", journalfoeringHendelseRecord);

    behandleJournalforingHendelseService.behandleJournalforingHendelse(journalfoeringHendelseRecord);
  }

  private boolean erOpprettetAvNKS(JournalfoeringHendelseRecord record) {
    return JournalpostKanal.NAV_NO_CHAT.name().equals(record.getMottaksKanal());
  }
}
