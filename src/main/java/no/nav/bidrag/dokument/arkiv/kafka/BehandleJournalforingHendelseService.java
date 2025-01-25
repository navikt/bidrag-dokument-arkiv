package no.nav.bidrag.dokument.arkiv.kafka;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivKt.SECURE_LOGGER;

import com.google.common.base.Strings;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.stream.Collectors;
import no.nav.bidrag.dokument.arkiv.dto.Dokument;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal;
import no.nav.bidrag.dokument.arkiv.model.*;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

@Service
public class BehandleJournalforingHendelseService {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(BehandleJournalforingHendelseService.class);
  private static final String HENDELSE_COUNTER_NAME = "joark_hendelse";
  private static final String HENDELSE_NUMBER_OF_DOCS_COUNTER_NAME = "joark_antall_dokumenter";

  private final MeterRegistry meterRegistry;
  private final HendelserProducer producer;
  private final JournalpostService journalpostService;
  private final DistributionSummary numberOfDocsDistribution;

  public BehandleJournalforingHendelseService(
      HendelserProducer producer,
      MeterRegistry registry,
      ResourceByDiscriminator<JournalpostService> journalpostServices) {
    this.producer = producer;
    this.meterRegistry = registry;
    this.journalpostService = journalpostServices.get(Discriminator.SERVICE_USER);
    this.numberOfDocsDistribution =
        DistributionSummary.builder(HENDELSE_NUMBER_OF_DOCS_COUNTER_NAME)
            .publishPercentileHistogram()
            .publishPercentiles(0.1, 0.3, 0.5, 0.95, 0.99)
            .description("Antall dokumenter som blir sendt inn via Ditt Nav/Skanning")
            .register(this.meterRegistry);
  }

  public void behandleJournalforingHendelse(JournalfoeringHendelseRecord record) {
    var journalpostId = record.getJournalpostId();
    var journalpost =
        hentJournalpostMedSaksbehandlerIdent(journalpostId, record.getJournalpostStatus());
    SECURE_LOGGER.info("Mottok journalføringshendelse med id {} og journalpost {}", record, journalpost);
    if (erOpprettetAvNKS(journalpost)) {
      var brevKoder =
          journalpost.getDokumenter().stream()
              .map(Dokument::getBrevkode)
              .collect(Collectors.joining(","));
      LOGGER.warn(
          "Journalpost {} er opprettet av NKS. Stopper videre behandling. opprettetAvNavn={}, brevkoder={}",
          record.getJournalpostId(),
          journalpost.getOpprettetAvNavn(),
          brevKoder);
      return;
    }

    JournalforingHendelseIntern journalforingHendelseIntern =
        new JournalforingHendelseIntern(record);

    producer.publishJournalpostHendelse(
        journalforingHendelseIntern.toJournalpostHendelse(journalpost));
    measureHendelse(record, journalpost);
    loggHendelse(record, journalpost);
  }

  private void measureHendelse(JournalfoeringHendelseRecord record, Journalpost journalpost) {
    try {
      var hendelsesType =
          JoarkHendelseType.Companion.from(record.getHendelsesType())
              .orElse(JoarkHendelseType.UKJENT);
      var tema =
          Strings.isNullOrEmpty(journalpost.getTema())
              ? record.getTemaNytt()
              : journalpost.getTema();
      this.meterRegistry
          .counter(
              HENDELSE_COUNTER_NAME,
              "hendelse_type",
              hendelsesType.toString(),
              "temaGammelt",
              Strings.isNullOrEmpty(record.getTemaGammelt()) ? "NULL" : record.getTemaGammelt(),
              "tema",
              tema,
              "kanal",
              record.getMottaksKanal())
          .increment();

      this.numberOfDocsDistribution.record(journalpost.hentAntallDokumenter());
    } catch (Exception e) {
      LOGGER.error("Det skjedde en feil ved måling av hendelse", e);
    }
  }

  private void loggHendelse(JournalfoeringHendelseRecord hendelseRecord, Journalpost journalpost) {
    try {
      var antallDokumenter = journalpost.hentAntallDokumenter();
      SECURE_LOGGER.info(
          "Behandlet journalføringshendelse {}, bruker={}, avsender={}, journalfortAvNavn={}, opprettetAvNavn={}, brevkoder={} og antall dokumenter {}",
          hendelseRecord,
          journalpost.hentGjelderId(),
          journalpost.hentAvsenderMottakerId(),
          journalpost.getJournalfortAvNavn(),
          journalpost.getOpprettetAvNavn(),
          journalpost.getDokumenter().stream()
              .map(Dokument::getBrevkode)
              .collect(Collectors.joining(",")),
          antallDokumenter);
      LOGGER.info(
          "Behandlet journalføringshendelse {} med journalpostId={}, journalforendeEnhet={}, kanal={}, journalpostStatus={}, temaGammelt={}, temaNytt={}, opprettetAvNavn={} og antall dokumenter {}",
          hendelseRecord.getHendelsesType(),
          hendelseRecord.getJournalpostId(),
          journalpost.getJournalforendeEnhet(),
          hendelseRecord.getMottaksKanal(),
          hendelseRecord.getJournalpostStatus(),
          hendelseRecord.getTemaGammelt(),
          hendelseRecord.getTemaNytt(),
          journalpost.getOpprettetAvNavn(),
          antallDokumenter);
    } catch (Exception e) {
      LOGGER.error("Det skjedde en feil ved logging av hendelse", e);
    }
  }

  private Journalpost hentJournalpostMedSaksbehandlerIdent(
      Long journalpostId, String journalpostStatus) {
    if ("JOURNALFOERT".equals(journalpostStatus)) {
      return hentJournalfortJournalpost(journalpostId);
    }

    return hentJournalpost(journalpostId);
  }

  private Journalpost hentJournalfortJournalpost(Long journalpostId) {
    try {
      return retryTemplate()
          .execute(
              arg0 -> {
                Journalpost journalpost = hentJournalpostMedSaker(journalpostId);
                if (journalpost.isStatusJournalfort()
                    && journalpost.hentJournalfortAvIdent() == null) {
                  LOGGER.warn(
                      "Fant ingen saksbehandlerident lagret som tilleggsopplysning på journalført journalpost {}, venter før det forsøkes på nytt",
                      journalpostId);
                  throw new JournalfortJournalpostManglerJournalfortAvIdent(
                      "Journalført journalpost mangler journaført av ident");
                }
                return journalpost;
              });
    } catch (Exception e) {
      LOGGER.error(
          "Fant ingen saksbehandlerident lagret som tilleggsopplysning på journalført journalpost {}. Fortsetter behandling uten saksbehandlerident. Dette vil påvirke videre behandling i bidrag-arbeidsflyt.",
          journalpostId);
      return hentJournalpost(journalpostId);
    }
  }

  private Journalpost hentJournalpost(Long journalpostId) {
    return Optional.ofNullable(journalpostService.hentJournalpost(journalpostId))
        .orElseThrow(
            () ->
                new JournalpostIkkeFunnetException(
                    String.format("Fant ikke journalpost med id %s", journalpostId)));
  }

  private Journalpost hentJournalpostMedSaker(Long journalpostId) {
    return Optional.ofNullable(journalpostService.hentJournalpostMedTilknyttedeSaker(journalpostId))
        .orElseThrow(
            () ->
                new JournalpostIkkeFunnetException(
                    String.format("Fant ikke journalpost med id %s", journalpostId)));
  }

  private boolean erOpprettetAvNKS(Journalpost journalpost) {
    var erKanalNavNoChat = JournalpostKanal.NAV_NO_CHAT.name().equals(journalpost.getKanal());
    var opprettetAvSalesforce = "NKSsalesforce".equals(journalpost.getOpprettetAvNavn());
    var brevkodeCRM =
        journalpost.getDokumenter().stream()
            .anyMatch(
                dokument ->
                    "CRM_MELDINGSKJEDE".equals(dokument.getBrevkode())
                        || "CRM_CHAT".equals(dokument.getBrevkode()));
    return brevkodeCRM || opprettetAvSalesforce || erKanalNavNoChat;
  }

  private RetryTemplate retryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();

    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(2000L);
    retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(3);
    retryTemplate.setRetryPolicy(retryPolicy);

    return retryTemplate;
  }
}
