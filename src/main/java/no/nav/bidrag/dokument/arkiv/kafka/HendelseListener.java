package no.nav.bidrag.dokument.arkiv.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.Optional;
import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.JournalpostHendelse;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Profile("!local")
public class HendelseListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(HendelseListener.class);
  public static final String JOURNALPOST_HENDELSE_OPPRETT_OPPGAVE = "OPPRETT_OPPGAVE";
  private static final String HENDELSE_COUNTER_NAME = "joark_hendelse";

  private final MeterRegistry meterRegistry;
  private final HendelserProducer producer;
  private final ResourceByDiscriminator<JournalpostService> journalpostServices;

  public HendelseListener(HendelserProducer producer, ResourceByDiscriminator<JournalpostService> journalpostServices, MeterRegistry registry) {
    this.producer = producer;
    this.journalpostServices = journalpostServices;
    this.meterRegistry = registry;
  }

  @KafkaListener(
      topics = "${TOPIC_JOURNALFOERING}",
      errorHandler = "hendelseErrorHandler")
  public void listen(@Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    CorrelationId correlationId = CorrelationId.generateTimestamped("journalfoeringshendelse");
    MDC.put("correlationId", correlationId.get());
    Oppgavetema oppgavetema = new Oppgavetema(journalfoeringHendelseRecord);
    if (!oppgavetema.erOmhandlingAvBidrag()) {
      LOGGER.debug("Oppgavetema omhandler ikke bidrag");
      return;
    }

    registrerOppgaveForHendelse(journalfoeringHendelseRecord);
    MDC.clear();
  }

  private void registrerOppgaveForHendelse(
      @Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    Optional<HendelsesType> muligType = HendelsesType.from(journalfoeringHendelseRecord.getHendelsesType());
    Optional<MottaksKanal> muligMottaksKanal = MottaksKanal.from(journalfoeringHendelseRecord.getMottaksKanal());
    muligType.ifPresent(
        hendelsesType -> muligMottaksKanal.ifPresent(mottaksKanal -> {
          LOGGER.info("Ny hendelse: {}", hendelsesType);
          if (hendelsesType == HendelsesType.JOURNALPOST_MOTTAT && mottaksKanal == MottaksKanal.NAV_NO) {
            this.meterRegistry.counter(HENDELSE_COUNTER_NAME,
                "hendelse_type", hendelsesType.name(),
                "tema", journalfoeringHendelseRecord.getTemaNytt(),
                "kanal", mottaksKanal.name()).count();
            LOGGER.info("Journalpost hendelse {} med data {}", hendelsesType, journalfoeringHendelseRecord);
            producer.publish(createJournalpostHendelse(journalfoeringHendelseRecord));
          } else {
            LOGGER.info("Ignorer hendelse: {}", hendelsesType);
          }
        })
    );

    if (muligMottaksKanal.isEmpty()) {
      LOGGER.warn("Ingen implementasjon for mottakskanal {}", journalfoeringHendelseRecord.getMottaksKanal());
    }

    if (muligType.isEmpty()) {
      LOGGER.warn("Ingen implementasjon for hendelse {}", journalfoeringHendelseRecord.getHendelsesType());
    }
  }

  private JournalpostHendelse createJournalpostHendelse(JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    var journalpostId = journalfoeringHendelseRecord.getJournalpostId();
    var journalpostOptional = journalpostServices.get(Discriminator.SERVICE_USER).hentJournalpostMedAktorId(journalpostId);
    if (journalpostOptional.isEmpty()) {
      throw new JournalpostIkkeFunnetException(String.format("Fant ikke journalpost med id %s", journalpostId));
    }

    var journalpost = journalpostOptional.get();
    JournalpostHendelse journalpostHendelse = new JournalpostHendelse(
        journalfoeringHendelseRecord.getJournalpostId(),
        JOURNALPOST_HENDELSE_OPPRETT_OPPGAVE
    );
    journalpostHendelse.addFagomrade(journalfoeringHendelseRecord.getTemaNytt());

    if (Objects.nonNull(journalpost.getBruker()) && Objects.nonNull(journalpost.getBruker().getId())) {
      var bruker = journalpost.getBruker();
      journalpostHendelse.addAktoerId(bruker.getId());
    }
    return journalpostHendelse;

  }
}
