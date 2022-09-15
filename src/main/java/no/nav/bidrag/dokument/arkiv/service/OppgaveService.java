package no.nav.bidrag.dokument.arkiv.service;

import java.util.List;
import no.nav.bidrag.dokument.arkiv.consumer.OppgaveConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.dto.*;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.OppgaveSokParametre;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OppgaveService {
  private static final Logger LOGGER = LoggerFactory.getLogger(OppgaveService.class);

  private final ResourceByDiscriminator<PersonConsumer> personConsumers;
  private final ResourceByDiscriminator<OppgaveConsumer> oppgaveConsumers;
  private final SaksbehandlerInfoManager saksbehandlerInfoManager;

  public OppgaveService(
      ResourceByDiscriminator<PersonConsumer> personConsumers,
      ResourceByDiscriminator<OppgaveConsumer> oppgaveConsumers,
      SaksbehandlerInfoManager saksbehandlerInfoManager
  ) {
    this.personConsumers = personConsumers;
    this.oppgaveConsumers = oppgaveConsumers;
    this.saksbehandlerInfoManager = saksbehandlerInfoManager;
  }

  public void opprettVurderDokumentOppgave(Journalpost journalpost, String journalpostId, String tildeltEnhetsnr, String tema, String kommentar) {
    var aktorId = hentAktorId(journalpost.hentGjelderId());
    opprettOppgave(new OpprettVurderDokumentOppgaveRequest(
        journalpost,
        journalpostId,
        tildeltEnhetsnr,
        tema,
        aktorId,
        hentSaksbehandlerMedEnhet(journalpost),
        kommentar
    ));
  }

  public void ferdigstillVurderDokumentOppgaver(Long journalpostId, String enhetsnr){
    var oppgaver = finnVurderDokumentOppgaverForJournalpost(journalpostId);
    oppgaver.forEach((oppgave)-> ferdigstillOppgave(oppgave, enhetsnr));
  }

  private void ferdigstillOppgave(OppgaveData oppgaveData, String enhetsnr){
    LOGGER.info("Ferdigstiller oppgave {} med oppgavetype {}", oppgaveData.getId(), oppgaveData.getOppgavetype());
    oppgaveConsumers.get(Discriminator.SERVICE_USER).patchOppgave(new FerdigstillOppgaveRequest(oppgaveData, enhetsnr));
  }

  private SaksbehandlerMedEnhet hentSaksbehandlerMedEnhet(Journalpost journalpost){
    return saksbehandlerInfoManager.hentSaksbehandler()
        .map(saksbehandler -> saksbehandler.tilEnhet(journalpost.getJournalforendeEnhet()))
        .orElseGet(() -> new SaksbehandlerMedEnhet(new Saksbehandler(), journalpost.getJournalforendeEnhet()));
  }

  private void opprettOppgave(OpprettOppgaveRequest request){
    oppgaveConsumers.get(Discriminator.REGULAR_USER).opprett(request);
  }

  private String hentAktorId(String gjelder) {
    return personConsumers.get(Discriminator.SERVICE_USER).hentPerson(gjelder)
        .orElseGet(()->new PersonResponse(gjelder, null, gjelder)).getAktoerId();
  }

  private List<OppgaveData> finnVurderDokumentOppgaverForJournalpost(Long journalpostId) {
    var parametre = new OppgaveSokParametre()
            .leggTilFagomrade("BID")
            .leggTilJournalpostId(journalpostId)
            .brukVurderDokumentSomOppgaveType();

    return oppgaveConsumers.get(Discriminator.SERVICE_USER).finnOppgaver(parametre).getOppgaver();
  }
}
