package no.nav.bidrag.dokument.arkiv.service;

import java.util.Objects;
import java.util.stream.Collectors;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivProxyConsumer;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakRequest;
import no.nav.bidrag.dokument.arkiv.dto.LagreJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.Sak;
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer;
import no.nav.bidrag.dokument.arkiv.model.FerdigstillFeiletException;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.LagreJournalpostFeiletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;

public class EndreJournalpostService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndreJournalpostService.class);

  private final JournalpostService journalpostService;
  private final DokarkivConsumer dokarkivConsumer;
  private final DokarkivProxyConsumer dokarkivProxyConsumer;
  private final OppgaveService oppgaveService;
  private final HendelserProducer hendelserProducer;

  public EndreJournalpostService(JournalpostService journalpostService, DokarkivConsumer dokarkivConsumer,
      DokarkivProxyConsumer dokarkivProxyConsumer, OppgaveService oppgaveService,
      HendelserProducer hendelserProducer) {
    this.journalpostService = journalpostService;
    this.dokarkivConsumer = dokarkivConsumer;
    this.dokarkivProxyConsumer = dokarkivProxyConsumer;
    this.oppgaveService = oppgaveService;
    this.hendelserProducer = hendelserProducer;
  }

  public HttpResponse<OppdaterJournalpostResponse> lagreJournalpost(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost){
    var oppdaterJournalpostRequest = new LagreJournalpostRequest(journalpostId, endreJournalpostCommand, journalpost);
    var oppdatertJournalpostResponse = dokarkivConsumer.endre(oppdaterJournalpostRequest);

    oppdatertJournalpostResponse.fetchBody().ifPresent(response -> LOGGER.debug("endret: {}", response));

    if (!oppdatertJournalpostResponse.is2xxSuccessful()){
      throw new LagreJournalpostFeiletException(String.format("Lagre journalpost feilet for journalpostid %s", journalpostId));
    }

    if (Objects.nonNull(oppdaterJournalpostRequest.getSak())){
      journalpost.setSak(new Sak(oppdaterJournalpostRequest.getSak().getFagsakId()));
    }

    hendelserProducer.publishJournalpostUpdated(journalpostId);
    return oppdatertJournalpostResponse;
  }


  public HttpResponse<Void> endre(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand) {
    var journalpost = journalpostService.hentJournalpost(journalpostId).orElseThrow(
        () -> new JournalpostIkkeFunnetException("Kunne ikke finne journalpost med id: " + journalpostId)
    );
    var oppdatertJournalpostResponse = lagreJournalpost(journalpostId, endreJournalpostCommand, journalpost);

    journalfoerJournalpostNarMottaksregistrert(endreJournalpostCommand, journalpost);
    tilknyttSakerTilJournalfoertJournalpost(endreJournalpostCommand, journalpost);
    opprettBehandleDokumentOppgaveVedJournalforing(endreJournalpostCommand, journalpost);

    return HttpResponse.from(
        oppdatertJournalpostResponse.fetchHeaders(),
        oppdatertJournalpostResponse.getResponseEntity().getStatusCode()
    );
  }

  public void opprettBehandleDokumentOppgaveVedJournalforing(EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost) {
    if (endreJournalpostCommand.skalJournalfores()) {
      behandleDokument(journalpost);
    }
  }

  private void behandleDokument(Journalpost journalpost) {
    if (journalpost.isInngaaendeDokument() && journalpost.hasSak()) {
      try {
        oppgaveService.behandleDokument(journalpost);
      } catch (HttpStatusCodeException e) {
        LOGGER.warn("Feil oppstod i oppgave v1 ved behandling av oppgave, {}: {}", e.getClass().getSimpleName(), e.getMessage());
      }
    }
  }

  private void journalfoerJournalpostNarMottaksregistrert(EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost){
    if (endreJournalpostCommand.skalJournalfores() && journalpost.isStatusMottatt()) {
      var journalpostId = journalpost.hentJournalpostIdLong();
      journalfoerJournalpost(journalpostId, endreJournalpostCommand);
      journalpost.setJournalstatus(JournalStatus.JOURNALFOERT);
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

  private void tilknyttTilSak(String saksnummer, Journalpost journalpost){
    KnyttTilAnnenSakRequest knyttTilAnnenSakRequest = new KnyttTilAnnenSakRequest(saksnummer, journalpost);
    LOGGER.info("Tilknytter sak {} til journalpost {}", saksnummer, journalpost.getJournalpostId());
    dokarkivProxyConsumer.knyttTilSak(journalpost.hentJournalpostIdLong(), knyttTilAnnenSakRequest);
  }

  private void journalfoerJournalpost(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand){
    var journalforRequest = new FerdigstillJournalpostRequest(journalpostId, endreJournalpostCommand.getEnhet());
    var ferdigstillResponse = dokarkivConsumer.ferdigstill(journalforRequest);
    if (!ferdigstillResponse.is2xxSuccessful()){
      throw new FerdigstillFeiletException(String.format("Ferdigstill journalpost feilet for journalpostId %s", journalpostId));
    }
    LOGGER.info("Journalpost med id {} er ferdigstillt", journalpostId);
  }
}