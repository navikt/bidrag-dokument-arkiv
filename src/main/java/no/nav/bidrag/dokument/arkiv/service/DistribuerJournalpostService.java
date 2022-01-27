package no.nav.bidrag.dokument.arkiv.service;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;
import static no.nav.bidrag.dokument.arkiv.dto.DistribusjonKt.validerAdresse;
import static no.nav.bidrag.dokument.arkiv.dto.DistribusjonKt.validerKanDistribueres;

import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokdistFordelingConsumer;
import no.nav.bidrag.dokument.arkiv.dto.AvvikshendelseIntern;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequestInternal;
import no.nav.bidrag.dokument.arkiv.dto.DistribuertTilAdresseDo;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DistribuerJournalpostService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DistribuerJournalpostService.class);

  private final JournalpostService journalpostService;
  private final DokdistFordelingConsumer dokdistFordelingConsumer;
  private final DokarkivConsumer dokarkivConsumer;

  public DistribuerJournalpostService(ResourceByDiscriminator<JournalpostService> journalpostServices, DokdistFordelingConsumer dokdistFordelingConsumer, DokarkivConsumer dokarkivConsumer) {
    this.journalpostService = journalpostServices.get(Discriminator.REGULAR_USER);
    this.dokdistFordelingConsumer = dokdistFordelingConsumer;
    this.dokarkivConsumer = dokarkivConsumer;
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
    validerKanDistribueres(journalpost, distribuerJournalpostRequest);
    validerAdresse(distribuerJournalpostRequest.getAdresse());

    lagreAdresse(journalpostId, distribuerJournalpostRequest.getAdresseDo(), enhet, journalpost);

    //TODO: Lagre bestillingsid når bd-arkiv er koblet mot database
    var distribuerResponse = dokdistFordelingConsumer.distribuerJournalpost(journalpostId, adresse);
    LOGGER.info("Bestillt distribusjon av journalpost {} med bestillingsId {}", journalpostId, distribuerResponse.getBestillingsId());
    return distribuerResponse;
  }

  public void lagreAdresse(Long journalpostId, DistribuertTilAdresseDo distribuertTilAdresseDo, String enhet, Journalpost journalpost){
    if (distribuertTilAdresseDo != null){
      journalpostService.lagreJournalpost(journalpostId, new EndreJournalpostCommandIntern(distribuertTilAdresseDo, enhet), journalpost);
    }
  }

  public void oppdaterDistribusjonsInfo(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern){
    var tilknyttedeJournalpost = journalpostService.hentTilknyttedeJournalposter(journalpost);
    tilknyttedeJournalpost.stream()
        .filter((jp)-> jp.getJournalstatus() != JournalStatus.EKSPEDERT)
        .forEach((jp)-> dokarkivConsumer.oppdaterDistribusjonsInfo(jp.getJournalpostId(), avvikshendelseIntern.getSettStatusEkspedert(), avvikshendelseIntern.getUtsendingsKanal()));
  }
}
