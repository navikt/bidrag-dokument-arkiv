package no.nav.bidrag.dokument.arkiv.service;

import no.nav.bidrag.dokument.arkiv.consumer.DokdistFordelingConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.model.JournalpostManglerMottakerIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistribuerJournalpostService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DistribuerJournalpostService.class);

  private final SafConsumer safConsumer;
  private final DokdistFordelingConsumer dokdistFordelingConsumer;

  public DistribuerJournalpostService(SafConsumer safConsumer, DokdistFordelingConsumer dokdistFordelingConsumer) {
    this.safConsumer = safConsumer;
    this.dokdistFordelingConsumer = dokdistFordelingConsumer;
  }

  public DistribuerJournalpostResponse distribuerJournalpost(Long journalpostId, DistribuerJournalpostRequest distribuerJournalpostRequest){
    LOGGER.info("Forsøker å distribuerer journalpost {} med foreslått adresse {}", journalpostId, distribuerJournalpostRequest.getAdresse());
    var journalpost = safConsumer.hentJournalpost(journalpostId);
    if (!journalpost.hasMottakerId() && !distribuerJournalpostRequest.hasAdresse()){
      throw new JournalpostManglerMottakerIdException("Adresse må oppgis når det ikke er satt mottaker id på Journalpost");
    }
    //TODO: Lagre bestillingsid når bd-arkiv er koblet mot database
    var distribuerResponse = dokdistFordelingConsumer.distribuerJournalpost(journalpostId, distribuerJournalpostRequest);
    LOGGER.info("Bestillt distribusjon av journalpost {} med bestillingsId {}", journalpostId, distribuerResponse.getBestillingsId());
    return distribuerResponse;

  }
}
