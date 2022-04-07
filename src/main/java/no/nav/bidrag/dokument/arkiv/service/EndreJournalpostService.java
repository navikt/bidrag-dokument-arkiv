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
import no.nav.bidrag.dokument.arkiv.dto.Sak;
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

  public HttpResponse<Void> endre(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand) {
    var journalpost = journalpostService.hentJournalpost(journalpostId).orElseThrow(
        () -> new JournalpostIkkeFunnetException("Kunne ikke finne journalpost med id: " + journalpostId)
    );

    endreJournalpostCommand.sjekkGyldigEndring(journalpost);

    lagreJournalpost(journalpostId, endreJournalpostCommand, journalpost);
    journalfoerJournalpostNarMottaksregistrert(endreJournalpostCommand, journalpost);
    tilknyttSakerTilJournalfoertJournalpost(endreJournalpostCommand, journalpost);
    opprettBehandleDokumentOppgaveVedJournalforing(endreJournalpostCommand, journalpost);

    hendelserProducer.publishJournalpostUpdated(journalpostId);
    return HttpResponse.from(HttpStatus.OK);
  }

  private void lagreJournalpost(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost){
    var oppdaterJournalpostRequest = new LagreJournalpostRequest(journalpostId, endreJournalpostCommand, journalpost);

    dokarkivConsumer.endre(oppdaterJournalpostRequest);

    if (Objects.nonNull(oppdaterJournalpostRequest.getSak())){
      journalpost.setSak(new Sak(oppdaterJournalpostRequest.getSak().getFagsakId()));
    }
  }

  public void opprettBehandleDokumentOppgaveVedJournalforing(EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost) {
    if (endreJournalpostCommand.skalJournalfores()) {
      opprettBehandleDokumentOppgave(journalpost);
    }
  }

  private void opprettBehandleDokumentOppgave(Journalpost journalpost) {
    if (journalpost.isInngaaendeDokument() && journalpost.hasSak()) {
      try {
        oppgaveService.behandleDokument(journalpost);
      } catch (HttpStatusCodeException e) {
        LOGGER.error("Det oppstod feil ved opprettelse av behandle dokument for journapost {}", journalpost.getJournalpostId(), e);
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

  public void tilknyttTilSak(String saksnummer, Journalpost journalpost){
    tilknyttTilSak(saksnummer, null, journalpost);
  }

  public void tilknyttTilSak(String saksnummer, String tema, Journalpost journalpost){
    KnyttTilAnnenSakRequest knyttTilAnnenSakRequest = new KnyttTilAnnenSakRequest(saksnummer, journalpost, tema);
    LOGGER.info("Tilknytter sak {} til journalpost {}", saksnummer, journalpost.getJournalpostId());
    var response = dokarkivProxyConsumer.knyttTilSak(journalpost.hentJournalpostIdLong(), knyttTilAnnenSakRequest);
    LOGGER.info("Opprettet journalpost med id {} med tema {} og saksnummer {}", response.getNyJournalpostId(), tema, saksnummer);
    journalpost.leggTilTilknyttetSak(saksnummer);
  }

  private void journalfoerJournalpost(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand){
    var journalforRequest = new FerdigstillJournalpostRequest(journalpostId, endreJournalpostCommand.getEnhet());
    dokarkivConsumer.ferdigstill(journalforRequest);
    LOGGER.info("Journalpost med id {} er ferdigstillt", journalpostId);
  }
}
