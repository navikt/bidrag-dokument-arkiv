package no.nav.bidrag.dokument.arkiv.kafka;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_KAFKA_TEST;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_LIVE;

import com.google.common.base.Strings;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.MottaksKanal;
import no.nav.bidrag.dokument.arkiv.dto.OverforEnhetRequest;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.HendelsesType;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.Oppgavetema;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
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
  private static final String HENDELSE_COUNTER_NAME = "joark_hendelse";

  private final MeterRegistry meterRegistry;
  private final HendelserProducer producer;
  private final BidragOrganisasjonConsumer bidragOrganisasjonConsumer;
  private final DokarkivConsumer dokarkivConsumer;
  private final JournalpostService journalpostService;

  public HendelseListener(HendelserProducer producer, MeterRegistry registry,
      BidragOrganisasjonConsumer bidragOrganisasjonConsumer, ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers,
      ResourceByDiscriminator<JournalpostService> journalpostServices) {
    this.producer = producer;
    this.meterRegistry = registry;
    this.bidragOrganisasjonConsumer = bidragOrganisasjonConsumer;
    this.dokarkivConsumer = dokarkivConsumers.get(Discriminator.SERVICE_USER);
    this.journalpostService = journalpostServices.get(Discriminator.SERVICE_USER);
  }

  @KafkaListener(groupId = "bidrag-dokument-arkiv", topics = "${TOPIC_JOURNALFOERING}")
  public void listen(@Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    Oppgavetema oppgavetema = new Oppgavetema(journalfoeringHendelseRecord);
    if (!oppgavetema.erOmhandlingAvBidrag()) {
      LOGGER.debug("Oppgavetema omhandler ikke bidrag");
      return;
    }
    if (erOpprettetAvNKS(journalfoeringHendelseRecord)){
      LOGGER.debug("Journalpost er opprettet av NKS. Stopper videre behandling");
      return;
    }

    LOGGER.debug("Mottok journalføringshendelse {}", journalfoeringHendelseRecord);
    behandleJournalforingHendelse(journalfoeringHendelseRecord);
  }

  private void behandleJournalforingHendelse(@Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    var hendelsesType = HendelsesType.Companion.from(journalfoeringHendelseRecord.getHendelsesType()).orElse(HendelsesType.UKJENT);
    this.meterRegistry.counter(HENDELSE_COUNTER_NAME,
        "hendelse_type", hendelsesType.toString(),
        "tema", journalfoeringHendelseRecord.getTemaNytt(),
        "kanal", journalfoeringHendelseRecord.getMottaksKanal()).increment();

    behandleHendelse(journalfoeringHendelseRecord);
  }

  private void behandleHendelse(JournalfoeringHendelseRecord record){
      var journalpostId = record.getJournalpostId();
      var journalpost = hentJournalpost(journalpostId);
      if (erOpprettetAvNKS(journalpost)){
        LOGGER.info("Journalpost er opprettet av NKS opprettetAvNavn={}. Stopper videre behandling", journalpost.getOpprettetAvNavn());
        return;
      }

      LOGGER.info("Behandler journalføringshendelse {} med journalpostId={}, kanal={}, journalpostStatus={} og tema={}", record.getHendelsesType(), record.getJournalpostId(), record.getMottaksKanal(), record.getJournalpostStatus(), record.getTemaNytt());
      behandleJournalpostFraHendelse(journalpost);
  }

  private void behandleJournalpostFraHendelse(Journalpost journalpost){
    oppdaterJournalpostMedPersonGeografiskEnhet(journalpost);
    producer.publishJournalpostUpdated(journalpost);
  }

  private String hentGeografiskEnhet(String personId){
    if (Strings.isNullOrEmpty(personId)){
      return null;
    }

    var response = bidragOrganisasjonConsumer.hentGeografiskEnhet(personId);
    if (!response.is2xxSuccessful() && response.fetchBody().isEmpty()){
      return null;
    }
    return response.fetchBody().get().getEnhetIdent();
  }

  private Journalpost hentJournalpost(Long journalpostId){
    var journalpostOptional = journalpostService.hentJournalpostMedAktorId(journalpostId);
    if (journalpostOptional.isEmpty()) {
      throw new JournalpostIkkeFunnetException(String.format("Fant ikke journalpost med id %s", journalpostId));
    }
    return journalpostOptional.get();
  }

  private String hentBrukerId(Journalpost journalpost){
    return  Optional.of(journalpost)
        .map((Journalpost::getBruker))
        .map(Bruker::getId)
        .orElse(null);
  }

  private void oppdaterJournalpostMedPersonGeografiskEnhet(Journalpost journalpost){
    var brukerId = hentBrukerId(journalpost);
    var geografiskEnhet = hentGeografiskEnhet(brukerId);
    if (geografiskEnhet != null && journalpost.isStatusMottatt() && !journalpost.harJournalforendeEnhetLik(geografiskEnhet)){
      LOGGER.info("Oppdaterer journalpost enhet fra {} til {}", journalpost.getJournalforendeEnhet(), geografiskEnhet);
      var response = dokarkivConsumer.endre(new OverforEnhetRequest(journalpost.hentJournalpostIdLong(), geografiskEnhet));
      if (response.is2xxSuccessful()) {
        journalpost.setJournalforendeEnhet(geografiskEnhet);
      }
    }

  }

  private boolean erOpprettetAvNKS(JournalfoeringHendelseRecord record) {
    return MottaksKanal.NAV_NO_CHAT.name().equals(record.getMottaksKanal());
  }

  private boolean erOpprettetAvNKS(Journalpost journalpost){
    var erKanalNavNoChat = MottaksKanal.NAV_NO_CHAT.name().equals(journalpost.getKanal());
    var opprettetAvSalesforce = "NKSsalesforce".equals(journalpost.getOpprettetAvNavn());
    var brevkodeCRM = journalpost.getDokumenter().stream().anyMatch(dokument -> "CRM_MELDINGSKJEDE".equals(dokument.getBrevkode()));
    return brevkodeCRM || opprettetAvSalesforce || erKanalNavNoChat;
  }
}
