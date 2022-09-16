package no.nav.bidrag.dokument.arkiv.kafka;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler;
import no.nav.bidrag.dokument.arkiv.model.JournalpostHendelseException;
import no.nav.bidrag.dokument.arkiv.model.JournalpostHendelseIntern;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.bidrag.dokument.dto.JournalpostHendelse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.Optional;

public class HendelserProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger(HendelserProducer.class);
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final JournalpostService journalpostService;
  private final String topic;
  private final SaksbehandlerInfoManager saksbehandlerInfoManager;

  public HendelserProducer(JournalpostService journalpostService, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
      String topic,
      SaksbehandlerInfoManager saksbehandlerInfoManager) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.journalpostService = journalpostService;
    this.topic = topic;
    this.saksbehandlerInfoManager = saksbehandlerInfoManager;
  }

  public void publishJournalpostUpdated(Long journalpostId, String saksbehandlersEnhet){
    var journalpostHendelse = createJournalpostHendelse(journalpostId, saksbehandlersEnhet);
    publish(journalpostHendelse);
  }

  public void publishJournalpostHendelse(JournalpostHendelse journalpostHendelse){
    publish(journalpostHendelse);
  }

  private JournalpostHendelse createJournalpostHendelse(Long journalpostId, String saksbehandlersEnhet) {
    var journalpost = journalpostService.hentJournalpostMedTilknyttedeSaker(journalpostId)
        .orElseThrow(() -> new JournalpostIkkeFunnetException(String.format("Fant ikke journalpost med id %s", journalpostId)));
    return createJournalpostHendelse(journalpost, saksbehandlersEnhet);
  }

  private JournalpostHendelse createJournalpostHendelse(Journalpost journalpost, String saksbehandlersEnhet) {
    var saksbehandler = saksbehandlerInfoManager.hentSaksbehandler()
            .orElse(Optional.ofNullable(journalpost.hentJournalfortAvIdent())
                    .map((ident) -> new Saksbehandler(ident, journalpost.getJournalfortAvNavn()))
                    .orElse(new Saksbehandler(null, "bidrag-dokument-arkiv")));

    var saksbehandlerMedEnhet = saksbehandler.tilEnhet(saksbehandlersEnhet);
    return new JournalpostHendelseIntern(journalpost, saksbehandlerMedEnhet, null).hentJournalpostHendelse();
  }

  @Retryable(value = Exception.class, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 12000, multiplier = 2.0))
  private void publish(JournalpostHendelse journalpostHendelse){
    try {
      var message = objectMapper.writeValueAsString(journalpostHendelse);
      SECURE_LOGGER.info("Publiserer hendelse {}", message);
      LOGGER.info("Publiserer hendelse med journalpostId={}", journalpostHendelse.getJournalpostId());
      kafkaTemplate.send(topic, journalpostHendelse.getJournalpostId(), message);
    } catch (JsonProcessingException e) {
      throw new JournalpostHendelseException(e.getMessage(), e);
    }
  }
}
