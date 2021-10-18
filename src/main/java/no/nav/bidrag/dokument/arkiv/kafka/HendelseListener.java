package no.nav.bidrag.dokument.arkiv.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import no.nav.bidrag.commons.CorrelationId;
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
  private static final String HENDELSE_COUNTER_NAME = "joark_hendelse";

  private final MeterRegistry meterRegistry;
  private final HendelserProducer producer;

  public HendelseListener(HendelserProducer producer, MeterRegistry registry) {
    this.producer = producer;
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
    var hendelsesType = muligType.orElse(HendelsesType.ENDRING);
    if (hendelsesType == HendelsesType.JOURNALPOST_MOTTATT) {
      LOGGER.info("Journalpost hendelse {} med data {}", hendelsesType, journalfoeringHendelseRecord);
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
    producer.publishJournalpostUpdated(journalfoeringHendelseRecord.getJournalpostId());
  }
}
