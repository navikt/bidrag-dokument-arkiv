package no.nav.bidrag.dokument.arkiv.service;

import java.util.Objects;
import java.util.stream.Collectors;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivKnyttTilSakConsumer;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakRequest;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilGenerellSakRequest;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilSakRequest;
import no.nav.bidrag.dokument.arkiv.dto.LagreJournalfortAvIdentRequest;
import no.nav.bidrag.dokument.arkiv.dto.LagreJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostDistribusjonsInfoRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.Sak;
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.LagreSaksbehandlerIdentForJournalfortJournalpostFeilet;
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class EndreJournalpostService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndreJournalpostService.class);

  private final JournalpostService journalpostService;
  private final DokarkivConsumer dokarkivConsumer;
  private final DokarkivKnyttTilSakConsumer dokarkivKnyttTilSakConsumer;
  private final HendelserProducer hendelserProducer;
  private final SaksbehandlerInfoManager saksbehandlerInfoManager;


  public EndreJournalpostService(
          JournalpostService journalpostService,
          DokarkivConsumer dokarkivConsumer,
          DokarkivKnyttTilSakConsumer dokarkivKnyttTilSakConsumer,
      HendelserProducer hendelserProducer, SaksbehandlerInfoManager saksbehandlerInfoManager) {
    this.journalpostService = journalpostService;
    this.dokarkivConsumer = dokarkivConsumer;
    this.dokarkivKnyttTilSakConsumer = dokarkivKnyttTilSakConsumer;
    this.hendelserProducer = hendelserProducer;
    this.saksbehandlerInfoManager = saksbehandlerInfoManager;
  }

  public HttpResponse<Void> endre(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand) {
    var journalpost = hentJournalpost(journalpostId);

    endreJournalpostCommand.sjekkGyldigEndring(journalpost);

    lagreJournalpost(journalpostId, endreJournalpostCommand, journalpost);
    journalfoerJournalpostNarMottaksregistrert(endreJournalpostCommand, journalpost);

    if (journalpost.kanTilknytteSaker() || endreJournalpostCommand.skalJournalfores()){
      journalpost = hentJournalpost(journalpostId);
      tilknyttSakerTilJournalfoertJournalpost(endreJournalpostCommand, journalpost);
    }

    publiserJournalpostEndretHendelse(journalpost, journalpostId, endreJournalpostCommand);

    return HttpResponse.from(HttpStatus.OK);
  }

  private void publiserJournalpostEndretHendelse(Journalpost journalpost, Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand){
    if (journalpost.isInngaaendeDokument()){
      hendelserProducer.publishJournalpostUpdated(journalpostId, endreJournalpostCommand.getEnhet());
    }
  }

  public OppdaterJournalpostResponse lagreJournalpost(OppdaterJournalpostRequest oppdaterJournalpostRequest){
    return dokarkivConsumer.endre(oppdaterJournalpostRequest);
  }

  private void lagreJournalpost(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost){
    var oppdaterJournalpostRequest = new LagreJournalpostRequest(journalpostId, endreJournalpostCommand, journalpost);

    lagreJournalpost(oppdaterJournalpostRequest);

    if (Objects.nonNull(oppdaterJournalpostRequest.getSak())){
      journalpost.setSak(new Sak(oppdaterJournalpostRequest.getSak().getFagsakId()));
    }
  }

  private void journalfoerJournalpostNarMottaksregistrert(EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost){
    if (endreJournalpostCommand.skalJournalfores() && journalpost.isStatusMottatt()) {
      var journalpostId = journalpost.hentJournalpostIdLong();
      journalfoerJournalpost(journalpostId, endreJournalpostCommand.getEnhet(), journalpost);
      journalpost.setJournalstatus(JournalStatus.JOURNALFOERT);
    }
  }

  public void lagreSaksbehandlerIdentForJournalfortJournalpost(Journalpost journalpost){
    try {
      lagreJournalpost(new LagreJournalfortAvIdentRequest(journalpost.hentJournalpostIdLong(), journalpost, saksbehandlerInfoManager.hentSaksbehandlerBrukerId()));
    } catch (Exception e){
      throw new LagreSaksbehandlerIdentForJournalfortJournalpostFeilet(
          String.format("Lagring av saksbehandler ident for journalført journalpost %s feilet", journalpost.getJournalpostId()), e);
    }
  }

  private void tilknyttSakerTilJournalfoertJournalpost(EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost){
    if (journalpost.kanTilknytteSaker()) {
      journalpostService.populerMedTilknyttedeSaker(journalpost);
      endreJournalpostCommand.hentTilknyttetSaker().stream()
          .filter(sak -> !journalpost.hentTilknyttetSaker().contains(sak))
          .collect(Collectors.toSet())
          .forEach(saksnummer -> tilknyttTilSak(saksnummer, journalpost));
    }
  }

  public void tilknyttTilSak(String saksnummer, Journalpost journalpost){
    tilknyttTilSak(saksnummer, journalpost.isBidragTema() ? null : "BID", journalpost);
  }

  public void tilknyttTilSak(String saksnummer, String tema, Journalpost journalpost){
    KnyttTilAnnenSakRequest knyttTilAnnenSakRequest = new KnyttTilSakRequest(saksnummer, journalpost, tema);
    var response = dokarkivKnyttTilSakConsumer.knyttTilSak(journalpost.hentJournalpostIdLong(), knyttTilAnnenSakRequest);
    LOGGER.info("Tilknyttet journalpost {} til sak {} med ny journalpostId {} og tema {}",journalpost.getJournalpostId(), saksnummer, response.getNyJournalpostId(), tema);
    journalpost.leggTilTilknyttetSak(saksnummer);
  }

  public String tilknyttTilGenerellSak(String tema, Journalpost journalpost){
    KnyttTilGenerellSakRequest knyttTilGenerellSakRequest = new KnyttTilGenerellSakRequest(journalpost, tema);
    var response = dokarkivKnyttTilSakConsumer.knyttTilSak(journalpost.hentJournalpostIdLong(), knyttTilGenerellSakRequest);
    LOGGER.info("Tilknyttet journalpost {} til GENERELL_SAK med ny journalpostId {} og tema {}",journalpost.getJournalpostId(), response.getNyJournalpostId(), tema);
    return response.getNyJournalpostId();
  }

  private void journalfoerJournalpost(Long journalpostId, String enhet, Journalpost journalpost){
    var journalforRequest = new FerdigstillJournalpostRequest(journalpostId, enhet);
    dokarkivConsumer.ferdigstill(journalforRequest);
    LOGGER.info("Journalpost med id {} er journalført", journalpostId);
    lagreSaksbehandlerIdentForJournalfortJournalpost(journalpost);
  }

  public void oppdaterJournalpostDistribusjonBestiltStatus(Long journalpostId, Journalpost journalpost){
    lagreJournalpost(new OppdaterJournalpostDistribusjonsInfoRequest(journalpostId, journalpost));
  }

  private Journalpost hentJournalpost(Long journalpostId){
    LOGGER.info("Henter jouranlpost {}", journalpostId);
    return journalpostService.hentJournalpost(journalpostId).orElseThrow(
        () -> new JournalpostIkkeFunnetException("Kunne ikke finne journalpost med id: " + journalpostId)
    );
  }
}
