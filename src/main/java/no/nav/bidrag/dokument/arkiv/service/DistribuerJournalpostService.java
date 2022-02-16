package no.nav.bidrag.dokument.arkiv.service;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;
import static no.nav.bidrag.dokument.arkiv.dto.DistribusjonKt.validerAdresse;
import static no.nav.bidrag.dokument.arkiv.dto.DistribusjonKt.validerKanDistribueres;

import no.nav.bidrag.dokument.arkiv.consumer.DokdistFordelingConsumer;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequestInternal;
import no.nav.bidrag.dokument.arkiv.dto.DistribuertTilAdresseDo;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DistribuerJournalpostService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DistribuerJournalpostService.class);

  @Value("${ALLOW_REDISTRIBUTION_JOURNALPOST_IDS:}")
  private String allowReDisributionJournalpostIds;
  private final JournalpostService journalpostService;
  private final DokdistFordelingConsumer dokdistFordelingConsumer;

  public DistribuerJournalpostService(ResourceByDiscriminator<JournalpostService> journalpostServices, DokdistFordelingConsumer dokdistFordelingConsumer) {
    this.journalpostService = journalpostServices.get(Discriminator.REGULAR_USER);
    this.dokdistFordelingConsumer = dokdistFordelingConsumer;
  }

  public DistribuerJournalpostResponse distribuerJournalpost(Long journalpostId, DistribuerJournalpostRequestInternal distribuerJournalpostRequest, String enhet){
    var adresse = distribuerJournalpostRequest.getAdresse();
    LOGGER.info("Forsøker å distribuere journalpost {}", journalpostId);
    SECURE_LOGGER.info("Forsøker å distribuere journalpost {} med foreslått adresse {}", journalpostId, adresse);
    var journalpostOptional = journalpostService.hentJournalpost(journalpostId);
    if (journalpostOptional.isEmpty()){
      throw new JournalpostIkkeFunnetException(String.format("Fant ingen journalpost med id %s", journalpostId));
    }

    var journalpost = journalpostOptional.get();
    validerKanDistribueres(journalpost, allowReDisributionJournalpostIds);
    validerAdresse(distribuerJournalpostRequest.getAdresse());

    lagreAdresse(journalpostId, distribuerJournalpostRequest.getAdresseDo(), enhet, journalpost);

    //TODO: Lagre bestillingsid når bd-arkiv er koblet mot database
    var distribuerResponse = dokdistFordelingConsumer.distribuerJournalpost(journalpost, adresse);
    LOGGER.info("Bestillt distribusjon av journalpost {} med bestillingsId {}", journalpostId, distribuerResponse.getBestillingsId());
    journalpostService.oppdaterJournalpostDistribusjonBestiltStatus(journalpostId, journalpost);
    return distribuerResponse;
  }

  public void kanDistribuereJournalpost(Long journalpostId){
    LOGGER.info("Sjekker om distribuere journalpost {} kan distribueres", journalpostId);
    var journalpostOptional = journalpostService.hentJournalpost(journalpostId);
    if (journalpostOptional.isEmpty()){
      throw new JournalpostIkkeFunnetException(String.format("Fant ingen journalpost med id %s", journalpostId));
    }

    var journalpost = journalpostOptional.get();
    validerKanDistribueres(journalpost, allowReDisributionJournalpostIds);
  }

  public void lagreAdresse(Long journalpostId, DistribuertTilAdresseDo distribuertTilAdresseDo, String enhet, Journalpost journalpost){
    if (distribuertTilAdresseDo != null){
      journalpostService.lagreJournalpost(journalpostId, new EndreJournalpostCommandIntern(distribuertTilAdresseDo, enhet), journalpost);
    }
  }
}
