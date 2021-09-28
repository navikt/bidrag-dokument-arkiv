package no.nav.bidrag.dokument.arkiv.kafka;

import java.util.Optional;
import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.dokument.hendelse.HendelsesType;
import no.nav.bidrag.dokument.hendelse.service.OppgaveService;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class HendelseListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(HendelseListener.class);
  static final String BEHANDLINGSTEMA_BIDRAG = "BID";


  public HendelseListener() {

  }

  @KafkaListener(
      id = BidragDokumentHendelseConfig.CLIENT_ID,
      topics = "${JOURNALFOERINGHENDELSE_V1_TOPIC_URL}",
      errorHandler = "hendelseErrorHandler")
  public void listen(@Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    CorrelationId correlationId = CorrelationId.generateTimestamped("journalfoeringshendelse");
    MDC.put("correlationId", correlationId.get());


    MDC.clear();
  }

  private void registrerOppgaveForHendelse(
      @Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
    Optional<HendelsesType> muligType = HendelsesType.from(journalfoeringHendelseRecord.getHendelsesType());

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
