package no.nav.bidrag.dokument.arkiv.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.OverforEnhetRequest;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
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
@Profile("!local")
public class HendelseListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(HendelseListener.class);
  private static final String HENDELSE_COUNTER_NAME = "joark_hendelse";

  private final MeterRegistry meterRegistry;
  private final HendelserProducer producer;
  private final BidragOrganisasjonConsumer bidragOrganisasjonConsumer;
  private final DokarkivConsumer dokarkivConsumer;
  private final JournalpostService journalpostService;
  private static final String DEFAULT_ENHET = "4833";

  public HendelseListener(HendelserProducer producer, MeterRegistry registry,
      BidragOrganisasjonConsumer bidragOrganisasjonConsumer, ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers,
      ResourceByDiscriminator<JournalpostService> journalpostServices) {
    this.producer = producer;
    this.meterRegistry = registry;
    this.bidragOrganisasjonConsumer = bidragOrganisasjonConsumer;
    this.dokarkivConsumer = dokarkivConsumers.get(Discriminator.SERVICE_USER);
    this.journalpostService = journalpostServices.get(Discriminator.SERVICE_USER);
  }

  @KafkaListener(topics = "${TOPIC_JOURNALFOERING}", errorHandler = "hendelseErrorHandler")
  public void listen(@Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    Oppgavetema oppgavetema = new Oppgavetema(journalfoeringHendelseRecord);
    if (!oppgavetema.erOmhandlingAvBidrag()) {
      LOGGER.debug("Oppgavetema omhandler ikke bidrag");
      return;
    }

    registrerOppgaveForHendelse(journalfoeringHendelseRecord);
  }

  private void registrerOppgaveForHendelse(@Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    var hendelsesType = HendelsesType.from(journalfoeringHendelseRecord.getHendelsesType()).orElse(null);
    if (hendelsesType == HendelsesType.JOURNALPOST_MOTTATT) {
      LOGGER.info("Behandler journalpost hendelse {} med data {}", hendelsesType, journalfoeringHendelseRecord);
      behandleHendelse(hendelsesType, journalfoeringHendelseRecord);
    } else {
      LOGGER.info("Ignorer hendelse {} med data {}", hendelsesType, journalfoeringHendelseRecord);
    }
  }

  private void behandleHendelse(HendelsesType hendelsesType, JournalfoeringHendelseRecord journalfoeringHendelseRecord){
    this.meterRegistry.counter(HENDELSE_COUNTER_NAME,
        "hendelse_type", hendelsesType.toString(),
        "tema", journalfoeringHendelseRecord.getTemaNytt(),
        "kanal", journalfoeringHendelseRecord.getMottaksKanal()).increment();
    var journalpostId = journalfoeringHendelseRecord.getJournalpostId();
    var journalpost = oppdaterJournalpostMedPersonGeografiskEnhet(journalpostId);
    producer.publishJournalpostUpdated(journalpost);
  }

  // TODO: Error handling. Should not use DEFAULT_ENHET
  private String hentGeografiskEnhet(String personId){
    if (Objects.isNull(personId)){
      return DEFAULT_ENHET;
    }

    var response = bidragOrganisasjonConsumer.hentGeografiskEnhet(personId);
    if (!response.is2xxSuccessful() && response.fetchBody().isEmpty()){
      return DEFAULT_ENHET;
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

  private Journalpost oppdaterJournalpostMedPersonGeografiskEnhet(Long journalpostId){
    var journalpost = hentJournalpost(journalpostId);
    var brukerId = hentBrukerId(journalpost);
    var geografiskEnhet = hentGeografiskEnhet(brukerId);
    var response = dokarkivConsumer.endre(new OverforEnhetRequest(journalpostId, geografiskEnhet));
    if (response.is2xxSuccessful()) {
      journalpost.setJournalforendeEnhet(geografiskEnhet);
    }
    return journalpost;
  }
}
