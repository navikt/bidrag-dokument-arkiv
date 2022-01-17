package no.nav.bidrag.dokument.arkiv.service;

import no.nav.bidrag.dokument.arkiv.consumer.DokdistFordelingConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.model.JournalpostManglerMottakerIdException;

public class DistribuerJournalpostService {
  private final SafConsumer safConsumer;
  private final DokdistFordelingConsumer dokdistFordelingConsumer;

  public DistribuerJournalpostService(SafConsumer safConsumer, DokdistFordelingConsumer dokdistFordelingConsumer) {
    this.safConsumer = safConsumer;
    this.dokdistFordelingConsumer = dokdistFordelingConsumer;
  }

  public DistribuerJournalpostResponse distribuerJournalpost(Long journalpostId, DistribuerJournalpostRequest distribuerJournalpostRequest){
    var journalpost = safConsumer.hentJournalpost(journalpostId);
    if (!journalpost.hasMottakerId() && !distribuerJournalpostRequest.hasAdresse()){
      throw new JournalpostManglerMottakerIdException("Adresse må oppgis når det ikke er satt mottaker id på Journalpost");
    }
    //TODO: Lagre bestillingsid når bd-arkiv er koblet mot database
    return dokdistFordelingConsumer.distribuerJournalpost(journalpostId, distribuerJournalpostRequest);
  }
}
