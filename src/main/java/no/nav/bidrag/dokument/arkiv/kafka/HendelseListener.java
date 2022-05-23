package no.nav.bidrag.dokument.arkiv.kafka;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_KAFKA_TEST;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_LIVE;

import com.google.common.base.Strings;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.stream.Collectors;
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.Dokument;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.MottaksKanal;
import no.nav.bidrag.dokument.arkiv.dto.OverforEnhetRequest;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.HendelsesType;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.JournalpostTema;
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
  private static final String HENDELSE_NUMBER_OF_DOCS_COUNTER_NAME = "joark_antall_dokumenter";
  private static final String DEFAULT_ENHET = "4833";

  private final MeterRegistry meterRegistry;
  private final HendelserProducer producer;
  private final BidragOrganisasjonConsumer bidragOrganisasjonConsumer;
  private final DokarkivConsumer dokarkivConsumer;
  private final JournalpostService journalpostService;

  public HendelseListener(
      HendelserProducer producer,
      MeterRegistry registry,
      BidragOrganisasjonConsumer bidragOrganisasjonConsumer,
      ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers,
      ResourceByDiscriminator<JournalpostService> journalpostServices) {
    this.producer = producer;
    this.meterRegistry = registry;
    this.bidragOrganisasjonConsumer = bidragOrganisasjonConsumer;
    this.dokarkivConsumer = dokarkivConsumers.get(Discriminator.SERVICE_USER);
    this.journalpostService = journalpostServices.get(Discriminator.SERVICE_USER);
  }

  @KafkaListener(groupId = "bidrag-dokument-arkiv", topics = "${TOPIC_JOURNALFOERING}")
  public void listen(@Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    JournalpostTema journalpostTema = new JournalpostTema(journalfoeringHendelseRecord);
    if (!journalpostTema.erOmhandlingAvBidrag()) {
      LOGGER.debug("JournalpostTema omhandler ikke bidrag");
      return;
    }

    SECURE_LOGGER.info("Mottok journalføringshendelse {}", journalfoeringHendelseRecord);

    if (erOpprettetAvNKS(journalfoeringHendelseRecord)){
      LOGGER.debug("Journalpost er opprettet av NKS. Stopper videre behandling");
      return;
    }

    behandleJournalforingHendelse(journalfoeringHendelseRecord);
  }

  private void behandleJournalforingHendelse(JournalfoeringHendelseRecord record){
      var journalpostId = record.getJournalpostId();
      var journalpost = hentJournalpost(journalpostId);
      if (erOpprettetAvNKS(journalpost)){
        LOGGER.info("Journalpost {} er opprettet av NKS. Stopper videre behandling", record.getJournalpostId());
        return;
      }

      loggHendelse(record, journalpost);
      behandleJournalpostFraHendelse(journalpost);
  }

  private void loggHendelse(JournalfoeringHendelseRecord hendelseRecord, Journalpost journalpost){
    try {
      var hendelsesType = HendelsesType.Companion.from(hendelseRecord.getHendelsesType()).orElse(HendelsesType.UKJENT);
      this.meterRegistry.counter(HENDELSE_COUNTER_NAME,
          "hendelse_type", hendelsesType.toString(),
          "tema", hendelseRecord.getTemaNytt(),
          "kanal", hendelseRecord.getMottaksKanal()).increment();
      SECURE_LOGGER.info("Behandler journalføringshendelse {}, bruker={}, avsender={}, journalfortAvNavn={}, opprettetAvNavn={}, brevkoder={}",
          hendelseRecord,
          journalpost.hentGjelderId(),
          journalpost.hentAvsenderMottakerId(),
          journalpost.getJournalfortAvNavn(),
          journalpost.getOpprettetAvNavn(),
          journalpost.getDokumenter().stream().map(Dokument::getBrevkode).collect(Collectors.joining(","))
      );
      var antallDokumenter = journalpost.getDokumenter().size();
      LOGGER.info("Behandler journalføringshendelse {} med journalpostId={}, journalforendeEnhet={}, kanal={}, journalpostStatus={}, temaGammelt={}, temaNytt={}, opprettetAvNavn={} og antall dokumenter {}",
          hendelseRecord.getHendelsesType(),
          hendelseRecord.getJournalpostId(),
          journalpost.getJournalforendeEnhet(),
          hendelseRecord.getMottaksKanal(),
          hendelseRecord.getJournalpostStatus(),
          hendelseRecord.getTemaGammelt(),
          hendelseRecord.getTemaNytt(),
          journalpost.getOpprettetAvNavn(),
          antallDokumenter
      );
      this.meterRegistry.summary(HENDELSE_NUMBER_OF_DOCS_COUNTER_NAME).record(antallDokumenter);
    } catch (Exception e){
      LOGGER.error("Det skjedde en feil ved logging av hendelse", e);
    }

  }

  private void behandleJournalpostFraHendelse(Journalpost journalpost){
    if (journalpost.isStatusMottatt() && journalpost.isTemaBidrag()){
      oppdaterJournalpostMedPersonGeografiskEnhet(journalpost);
    }
    producer.publishJournalpostUpdated(journalpost);
  }

  private String hentGeografiskEnhet(String personId){
    var geografiskEnhet = bidragOrganisasjonConsumer.hentGeografiskEnhet(personId);
    if (Strings.isNullOrEmpty(geografiskEnhet)){
      LOGGER.warn("Fant ingen geografisk enhet for person, bruker enhet {}", DEFAULT_ENHET);
      SECURE_LOGGER.warn("Fant ingen geografisk enhet for person {}, bruker enhet {}", personId, DEFAULT_ENHET);
      return DEFAULT_ENHET;
    }
    return geografiskEnhet;
  }

  private Journalpost hentJournalpost(Long journalpostId){
    return journalpostService.hentJournalpostMedAktorId(journalpostId).orElseThrow(()->new JournalpostIkkeFunnetException(String.format("Fant ikke journalpost med id %s", journalpostId)));
  }

  private String hentBrukerId(Journalpost journalpost){
    return Optional.of(journalpost)
        .map(Journalpost::getBruker)
        .map(Bruker::getId)
        .orElseGet(() -> Optional.of(journalpost)
            .map(Journalpost::getAvsenderMottaker)
            .map(AvsenderMottaker::getId)
            .orElse(null));
  }

  private void oppdaterJournalpostMedPersonGeografiskEnhet(Journalpost journalpost){
    var brukerId = hentBrukerId(journalpost);
    var geografiskEnhet = hentGeografiskEnhet(brukerId);
    if (!journalpost.harJournalforendeEnhetLik(geografiskEnhet)){
      LOGGER.info("Oppdaterer journalpost {} enhet fra {} til {}", journalpost.getJournalpostId(), journalpost.getJournalforendeEnhet(), geografiskEnhet);
      SECURE_LOGGER.info("Oppdaterer journalpost {} enhet fra {} til {} for person {}", journalpost.getJournalpostId(), journalpost.getJournalforendeEnhet(), geografiskEnhet, brukerId);
      dokarkivConsumer.endre(new OverforEnhetRequest(journalpost.hentJournalpostIdLong(), geografiskEnhet));
      journalpost.setJournalforendeEnhet(geografiskEnhet);
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
