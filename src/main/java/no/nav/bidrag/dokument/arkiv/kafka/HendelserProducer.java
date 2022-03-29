package no.nav.bidrag.dokument.arkiv.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.dokument.arkiv.FeatureToggle;
import no.nav.bidrag.dokument.arkiv.FeatureToggle.Feature;
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

public class HendelserProducer {
  private static final Logger LOGGER = LoggerFactory.getLogger(HendelserProducer.class);
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final JournalpostService journalpostService;
  private final String topic;
  private final FeatureToggle featureToggle;
  private final SaksbehandlerInfoManager saksbehandlerInfoManager;

  public HendelserProducer(JournalpostService journalpostService, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
      String topic,
      FeatureToggle featureToggle, SaksbehandlerInfoManager saksbehandlerInfoManager) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.journalpostService = journalpostService;
    this.topic = topic;
    this.featureToggle = featureToggle;
    this.saksbehandlerInfoManager = saksbehandlerInfoManager;
  }

  public void publishJournalpostUpdated(Long journalpostId){
    var journalpostHendelse = createJournalpostHendelse(journalpostId);
    publish(journalpostHendelse);
  }

  public void publishJournalpostUpdated(Journalpost journalpost){
    var journalpostHendelse = createJournalpostHendelse(journalpost);
    publish(journalpostHendelse);
  }

  private JournalpostHendelse createJournalpostHendelse(Long journalpostId) {
    var journalpostOptional = journalpostService.hentJournalpostMedAktorId(journalpostId);
    if (journalpostOptional.isEmpty()) {
      throw new JournalpostIkkeFunnetException(String.format("Fant ikke journalpost med id %s", journalpostId));
    }
    var journalpost = journalpostOptional.get();
    return createJournalpostHendelse(journalpost);
  }

  private JournalpostHendelse createJournalpostHendelse(Journalpost journalpost) {
    var saksbehandler = saksbehandlerInfoManager.hentSaksbehandler().orElse(new Saksbehandler("ukjent", "kunne ikke hente saksbehandler"));
    return new JournalpostHendelseIntern(journalpost, saksbehandler).hentJournalpostHendelse();
  }

  @Retryable(value = Exception.class, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 12000, multiplier = 2.0))
  private void publish(JournalpostHendelse journalpostHendelse){
    try {
      var message = objectMapper.writeValueAsString(journalpostHendelse);
      if (!featureToggle.isFeatureEnabled(Feature.KAFKA_ARBEIDSFLYT)) {
        LOGGER.info("Sender ikke hendelse {} da feature toggle KAFKA_ARBEIDSFLYT ikke er skrudd på", message);
        return;
      }
      LOGGER.info("Publiserer hendelse {}", message);
      kafkaTemplate.send(topic, journalpostHendelse.getJournalpostId(), message);
    } catch (JsonProcessingException e) {
      throw new JournalpostHendelseException(e.getMessage(), e);
    }
  }
}
