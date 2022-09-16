package no.nav.bidrag.dokument.arkiv.service;

import static no.nav.bidrag.dokument.arkiv.dto.DistribusjonKt.validerAdresse;
import static no.nav.bidrag.dokument.arkiv.dto.DistribusjonKt.validerKanDistribueres;

import com.google.common.base.Strings;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import no.nav.bidrag.dokument.arkiv.consumer.DokdistFordelingConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequestInternal;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.LagreAdresseRequest;
import no.nav.bidrag.dokument.arkiv.dto.LagreReturDetaljForSisteReturRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterFlaggNyDistribusjonBestiltRequest;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.model.UgyldigDistribusjonException;
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse;
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DistribuerJournalpostService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DistribuerJournalpostService.class);
  private static final String DISTRIBUSJON_COUNTER_NAME = "distribuer_journalpost";
  private final JournalpostService journalpostService;
  private final EndreJournalpostService endreJournalpostService;
  private final OpprettJournalpostService opprettJournalpostService;
  private final DokdistFordelingConsumer dokdistFordelingConsumer;
  private final PersonConsumer personConsumer;
  private final MeterRegistry meterRegistry;

  public DistribuerJournalpostService(
      ResourceByDiscriminator<JournalpostService> journalpostServices,
      EndreJournalpostService endreJournalpostService,
      OpprettJournalpostService opprettJournalpostService, DokdistFordelingConsumer dokdistFordelingConsumer, ResourceByDiscriminator<PersonConsumer> personConsumers, MeterRegistry meterRegistry) {
    this.journalpostService = journalpostServices.get(Discriminator.REGULAR_USER);
    this.endreJournalpostService = endreJournalpostService;
    this.opprettJournalpostService = opprettJournalpostService;
    this.dokdistFordelingConsumer = dokdistFordelingConsumer;
    this.personConsumer = personConsumers.get(Discriminator.REGULAR_USER);
    this.meterRegistry = meterRegistry;
  }

  public void bestillNyDistribusjon(Journalpost journalpost, DistribuerTilAdresse distribuerTilAdresse){
    if (journalpost.getTilleggsopplysninger().isNyDistribusjonBestilt()){
      throw new UgyldigDistribusjonException(String.format("Ny distribusjon er allerede bestilt for journalpost %s", journalpost.getJournalpostId()));
    }
    validerAdresse(distribuerTilAdresse);
    oppdaterReturDetaljerHvisNodvendig(journalpost);

    var opprettJournalpostResponse = opprettJournalpostService.dupliserUtgaaendeJournalpost(journalpost, true);
    distribuerJournalpost(opprettJournalpostResponse.getJournalpostId(), null, new DistribuerJournalpostRequestInternal(distribuerTilAdresse));
    endreJournalpostService.lagreJournalpost(new OppdaterFlaggNyDistribusjonBestiltRequest(journalpost.hentJournalpostIdLong(), journalpost));
  }

  private void oppdaterReturDetaljerHvisNodvendig(Journalpost journalpost){
    if (journalpost.manglerReturDetaljForSisteRetur()){
      if (journalpost.hentDatoRetur() == null){
        throw new UgyldigDistribusjonException("Kan ikke bestille distribusjon når det mangler returdetalj for siste returpost");
      }
      endreJournalpostService.lagreJournalpost(new LagreReturDetaljForSisteReturRequest(journalpost));
    }

  }

  public DistribuerJournalpostResponse distribuerJournalpost(Long journalpostId, String batchId, DistribuerJournalpostRequestInternal distribuerJournalpostRequest){
    var journalpost = journalpostService.hentJournalpost(journalpostId).orElseThrow(() -> new JournalpostIkkeFunnetException(String.format("Fant ingen journalpost med id %s", journalpostId)));
    journalpostService.populerMedTilknyttedeSaker(journalpost);
    if (journalpost.getTilleggsopplysninger().isDistribusjonBestilt()){
      LOGGER.warn("Distribusjon er allerede bestillt for journalpostid {}{}. Stopper videre behandling", journalpostId, batchId != null ? String.format(" med batchId %s", batchId) : "");
      return new DistribuerJournalpostResponse("JOARK-"+journalpostId, null);
    }

    validerKanDistribueres(journalpost);

    var adresse = hentAdresse(distribuerJournalpostRequest, journalpost);

    if (adresse != null){
      validerAdresse(adresse);
      lagreAdresse(journalpostId, adresse, journalpost);
    }

    //TODO: Lagre bestillingsid når bd-arkiv er koblet mot database
    var distribuerResponse = dokdistFordelingConsumer.distribuerJournalpost(journalpost, batchId, adresse);
    LOGGER.info("Bestillt distribusjon av journalpost {} med bestillingsId {}", journalpostId, distribuerResponse.getBestillingsId());
    endreJournalpostService.oppdaterJournalpostDistribusjonBestiltStatus(journalpostId, journalpost);
    measureDistribution(batchId);
    return distribuerResponse;
  }

  private DistribuerTilAdresse hentAdresse(DistribuerJournalpostRequestInternal distribuerJournalpostRequestInternal, Journalpost journalpost){
    if (distribuerJournalpostRequestInternal.hasAdresse()){
      return distribuerJournalpostRequestInternal.getAdresse();
    }

    LOGGER.info("Distribusjon av journalpost bestilt uten adresse. Henter adresse for mottaker. JournalpostId {}", journalpost.getJournalpostId());

    var adresseResponse = personConsumer.hentAdresse(journalpost.hentAvsenderMottakerId());

    if (Objects.isNull(adresseResponse)){
      LOGGER.warn("Mottaker i journalpost {} mangler adresse", journalpost.getJournalpostId());
      return null;
    }

    return new DistribuerTilAdresse(
        adresseResponse.getAdresselinje1(),
        adresseResponse.getAdresselinje2(),
        adresseResponse.getAdresselinje3(),
        adresseResponse.getLand(),
        adresseResponse.getPostnummer(),
        adresseResponse.getPoststed()
    );

  }
  public void kanDistribuereJournalpost(Long journalpostId){
    LOGGER.info("Sjekker om distribuere journalpost {} kan distribueres", journalpostId);
    var journalpost = journalpostService.hentJournalpost(journalpostId).orElseThrow(() -> new JournalpostIkkeFunnetException(String.format("Fant ingen journalpost med id %s", journalpostId)));
    validerKanDistribueres(journalpost);
  }

  private void lagreAdresse(Long journalpostId, DistribuerTilAdresse distribuerTilAdresse, Journalpost journalpost){
    if (distribuerTilAdresse != null){
      endreJournalpostService.lagreJournalpost(new LagreAdresseRequest(journalpostId, distribuerTilAdresse, journalpost));
    }
  }

  private void measureDistribution(String batchId){
    try {
      this.meterRegistry.counter(DISTRIBUSJON_COUNTER_NAME,
          "batchId", Strings.isNullOrEmpty(batchId) ? "NONE" : batchId
      ).increment();
    } catch (Exception e){
      LOGGER.error("Det skjedde en feil ved oppdatering av metrikk", e);
    }

  }
}
