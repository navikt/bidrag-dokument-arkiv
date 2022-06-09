package no.nav.bidrag.dokument.arkiv.kafka;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;

import com.google.common.base.Strings;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.stream.Collectors;
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.Dokument;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal;
import no.nav.bidrag.dokument.arkiv.dto.OverforEnhetRequest;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.HendelsesType;
import no.nav.bidrag.dokument.arkiv.model.JournalforingHendelseIntern;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.PersonException;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BehandleJournalforingHendelseService {

  private static final Logger LOGGER = LoggerFactory.getLogger(BehandleJournalforingHendelseService.class);
  private static final String HENDELSE_COUNTER_NAME = "joark_hendelse";
  private static final String HENDELSE_NUMBER_OF_DOCS_COUNTER_NAME = "joark_antall_dokumenter";

  private final MeterRegistry meterRegistry;
  private final HendelserProducer producer;
  private final JournalpostService journalpostService;

  private final DistributionSummary numberOfDocsDistribution;

  public BehandleJournalforingHendelseService(HendelserProducer producer, MeterRegistry registry, ResourceByDiscriminator<JournalpostService> journalpostServices) {
    this.producer = producer;
    this.meterRegistry = registry;
    this.journalpostService = journalpostServices.get(Discriminator.SERVICE_USER);
    this.numberOfDocsDistribution = DistributionSummary.builder(HENDELSE_NUMBER_OF_DOCS_COUNTER_NAME)
        .publishPercentileHistogram()
        .publishPercentiles(0.1, 0.3, 0.5, 0.95, 0.99)
        .description("Antall dokumenter som blir sendt inn via Ditt Nav/Skanning")
        .register(this.meterRegistry);
  }

  public void behandleJournalforingHendelse(JournalfoeringHendelseRecord record){
    var journalpostId = record.getJournalpostId();
    var journalpost = hentJournalpost(journalpostId);
    if (erOpprettetAvNKS(journalpost)){
      var brevKoder = journalpost.getDokumenter().stream().map(Dokument::getBrevkode).collect(Collectors.joining(","));
      LOGGER.warn("Journalpost {} er opprettet av NKS. Stopper videre behandling. opprettetAvNavn={}, brevkoder={}", record.getJournalpostId(), journalpost.getOpprettetAvNavn(), brevKoder);
      return;
    }

    JournalforingHendelseIntern journalforingHendelseIntern = new JournalforingHendelseIntern(record);

    producer.publishJournalpostHendelse(journalforingHendelseIntern.toJournalpostHendelse(journalpost));
    measureHendelse(record, journalpost);
    loggHendelse(record, journalpost);
  }

  private void measureHendelse(JournalfoeringHendelseRecord record, Journalpost journalpost){
    try {
      var hendelsesType = HendelsesType.Companion.from(record.getHendelsesType()).orElse(HendelsesType.UKJENT);
      var tema = Strings.isNullOrEmpty(journalpost.getTema()) ? record.getTemaNytt() : journalpost.getTema();
      this.meterRegistry.counter(HENDELSE_COUNTER_NAME,
          "hendelse_type", hendelsesType.toString(),
          "temaGammelt", Strings.isNullOrEmpty(record.getTemaGammelt()) ? "NULL" : record.getTemaGammelt(),
          "tema", tema,
          "kanal", record.getMottaksKanal()).increment();

      this.numberOfDocsDistribution.record(journalpost.hentAntallDokumenter());
    } catch (Exception e){
      LOGGER.error("Det skjedde en feil ved måling av hendelse", e);
    }

  }
  private void loggHendelse(JournalfoeringHendelseRecord hendelseRecord, Journalpost journalpost){
    try {
      var antallDokumenter = journalpost.hentAntallDokumenter();
      SECURE_LOGGER.info("Behandlet journalføringshendelse {}, bruker={}, avsender={}, journalfortAvNavn={}, opprettetAvNavn={}, brevkoder={} og antall dokumenter {}",
          hendelseRecord,
          journalpost.hentGjelderId(),
          journalpost.hentAvsenderMottakerId(),
          journalpost.getJournalfortAvNavn(),
          journalpost.getOpprettetAvNavn(),
          journalpost.getDokumenter().stream().map(Dokument::getBrevkode).collect(Collectors.joining(",")),
          antallDokumenter
      );
      LOGGER.info("Behandlet journalføringshendelse {} med journalpostId={}, journalforendeEnhet={}, kanal={}, journalpostStatus={}, temaGammelt={}, temaNytt={}, opprettetAvNavn={} og antall dokumenter {}",
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
    } catch (Exception e){
      LOGGER.error("Det skjedde en feil ved logging av hendelse", e);
    }
  }

  private Journalpost hentJournalpost(Long journalpostId){
    try {
      return journalpostService.hentJournalpostMedAktorId(journalpostId)
          .orElseThrow(()->new JournalpostIkkeFunnetException(String.format("Fant ikke journalpost med id %s", journalpostId)));
    } catch (PersonException e){
      return journalpostService.hentJournalpost(journalpostId)
          .orElseThrow(()->new JournalpostIkkeFunnetException(String.format("Fant ikke journalpost med id %s", journalpostId)));
    }
  }

  private boolean erOpprettetAvNKS(Journalpost journalpost){
    var erKanalNavNoChat = JournalpostKanal.NAV_NO_CHAT.name().equals(journalpost.getKanal());
    var opprettetAvSalesforce = "NKSsalesforce".equals(journalpost.getOpprettetAvNavn());
    var brevkodeCRM = journalpost.getDokumenter().stream().anyMatch(dokument -> "CRM_MELDINGSKJEDE".equals(dokument.getBrevkode()) || "CRM_CHAT".equals(dokument.getBrevkode()));
    return brevkodeCRM || opprettetAvSalesforce || erKanalNavNoChat;
  }
}
