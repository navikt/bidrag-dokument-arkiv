package no.nav.bidrag.dokument.arkiv.kafka;

import no.nav.bidrag.commons.CorrelationId;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Profile("!test")
public class HendelseListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(HendelseListener.class);
  public static final String BEHANDLINGSTEMA_BIDRAG = "BID";


  public HendelseListener() {

  }

  @KafkaListener(
      topics = "${JOURNALFOERINGHENDELSE_V1_TOPIC_URL}",
      errorHandler = "hendelseErrorHandler")
  public void listen(@Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    CorrelationId correlationId = CorrelationId.generateTimestamped("journalfoeringshendelse");
    MDC.put("correlationId", correlationId.get());
    Oppgavetema oppgavetema = new Oppgavetema(journalfoeringHendelseRecord);

    if (oppgavetema.erOmhandlingAvBidrag()) {
      registrerOppgaveForHendelse(journalfoeringHendelseRecord);
    } else {
      LOGGER.debug("Oppgavetema omhandler ikke bidrag ");
    }

    MDC.clear();
  }

  private void registrerOppgaveForHendelse(
      @Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    Optional<HendelsesType> muligType = HendelsesType.from(journalfoeringHendelseRecord.getHendelsesType());
    LOGGER.info("Ny hendelse: {}", muligType);

    muligType.ifPresent(
        hendelsesType -> {
          switch (hendelsesType) {
            case MIDLERTIDIG_JOURNALFORT:
              LOGGER.info("Ny journalpost: {}", journalfoeringHendelseRecord);
              break;
            case TEMA_ENDRET:
              LOGGER.info("Endring i tema: {}", journalfoeringHendelseRecord);
          }

        }
    );

    if (muligType.isEmpty()) {
      LOGGER.error("Ingen implementasjon for {}", journalfoeringHendelseRecord.getHendelsesType());
    }
  }
}
