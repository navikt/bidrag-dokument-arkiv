package no.nav.bidrag.dokument.arkiv.service;

import static no.nav.bidrag.dokument.arkiv.dto.DistribusjonKt.validerKanDistribueres;

import no.nav.bidrag.dokument.arkiv.consumer.DokdistFordelingConsumer;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.JournalpostManglerMottakerIdException;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DistribuerJournalpostService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DistribuerJournalpostService.class);

  private final JournalpostService journalpostService;
  private final DokdistFordelingConsumer dokdistFordelingConsumer;

  public DistribuerJournalpostService(ResourceByDiscriminator<JournalpostService> journalpostServices, DokdistFordelingConsumer dokdistFordelingConsumer) {
    this.journalpostService = journalpostServices.get(Discriminator.REGULAR_USER);
    this.dokdistFordelingConsumer = dokdistFordelingConsumer;
  }

  public DistribuerJournalpostResponse distribuerJournalpost(Long journalpostId, DistribuerJournalpostRequest distribuerJournalpostRequest){
    var adresse = distribuerJournalpostRequest != null ? distribuerJournalpostRequest.getAdresse() : null;
    LOGGER.info("Forsøker å distribuerer journalpost {} med foreslått adresse {}", journalpostId, adresse);
    var journalpostOptional = journalpostService.hentJournalposMedTilknyttedeSaker(journalpostId);
    if (journalpostOptional.isEmpty()){
      throw new JournalpostIkkeFunnetException(String.format("Fant ingen journalpost med id %s", journalpostId));
    }

    var journalpost = journalpostOptional.get();
    validerKanDistribueres(journalpost, distribuerJournalpostRequest);

    //TODO: Lagre bestillingsid når bd-arkiv er koblet mot database
    var distribuerResponse = dokdistFordelingConsumer.distribuerJournalpost(journalpostId, distribuerJournalpostRequest);
    LOGGER.info("Bestillt distribusjon av journalpost {} med bestillingsId {}", journalpostId, distribuerResponse.getBestillingsId());
    return distribuerResponse;

  }
}
