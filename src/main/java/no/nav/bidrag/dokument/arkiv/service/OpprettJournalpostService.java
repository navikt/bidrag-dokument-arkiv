package no.nav.bidrag.dokument.arkiv.service;

import static no.nav.bidrag.dokument.arkiv.dto.OpprettJournalpostKt.validerJournalpostKanDupliseres;

import java.util.ArrayList;
import java.util.stream.Collectors;
import kotlin.Pair;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.OpprettJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OpprettJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpprettJournalpostService {
  private static final Logger LOGGER = LoggerFactory.getLogger(OpprettJournalpostService.class);
  private final DokarkivConsumer dokarkivConsumer;
  private final  DokumentService dokumentService;

  public OpprettJournalpostService(ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers, DokumentService dokumentService) {
    this.dokarkivConsumer = dokarkivConsumers.get(Discriminator.REGULAR_USER);
    this.dokumentService = dokumentService;
  }

  public OpprettJournalpostResponse dupliserJournalpost(Journalpost journalpost, boolean removeDistribusjonMetadata){
    validerJournalpostKanDupliseres(journalpost);

    var opprettJournalpostRequest = createOpprettJournalpostRequest(journalpost, removeDistribusjonMetadata);
    opprettJournalpostRequest.setEksternReferanseId(String.format("BID_duplikat_%s", journalpost.getJournalpostId()));
    var opprettJournalpostResponse =  dokarkivConsumer.opprett(opprettJournalpostRequest);
    LOGGER.info("Duplisert journalpost {}, ny journalpostId {}", journalpost.getJournalpostId(), opprettJournalpostResponse.getJournalpostId());
    return opprettJournalpostResponse;
  }

  private OpprettJournalpostRequest createOpprettJournalpostRequest(Journalpost journalpost, boolean removeDistribusjonMetadata){
    var dokumenterByte = journalpost.getDokumenter().stream()
        .map((dokument -> new Pair<>(dokument.getDokumentInfoId(),
            dokumentService.hentDokument(journalpost.hentJournalpostIdLong(), dokument.getDokumentInfoId()).getBody())))
        .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

    var opprettJournalpostRequest = new OpprettJournalpostRequest(journalpost, dokumenterByte);
    if (removeDistribusjonMetadata){
      var tillegssopplysninger = new TilleggsOpplysninger();
      tillegssopplysninger.addAll(journalpost.getTilleggsopplysninger());
      tillegssopplysninger.removeDistribusjonMetadata();
      opprettJournalpostRequest.setTilleggsopplysninger(tillegssopplysninger);
    }
    return opprettJournalpostRequest;
  }

}
