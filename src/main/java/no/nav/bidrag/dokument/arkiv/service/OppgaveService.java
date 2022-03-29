package no.nav.bidrag.dokument.arkiv.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.nav.bidrag.dokument.arkiv.consumer.OppgaveConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.dto.EndreForNyttDokumentRequest;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.OppgaveData;
import no.nav.bidrag.dokument.arkiv.dto.OpprettBehandleDokumentOppgaveRequest;
import no.nav.bidrag.dokument.arkiv.dto.OpprettOppgaveRequest;
import no.nav.bidrag.dokument.arkiv.dto.OpprettVurderKonsekvensYtelseOppgaveRequest;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler;
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerMedEnhet;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.OppgaveSokParametre;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager;
import no.nav.bidrag.dokument.dto.JournalpostBeskrivelseException;
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

  public void opprettOverforJournalpostOppgave(Journalpost journalpost, String tema, String kommentar) {
    var aktorId = hentAktorId(journalpost.hentGjelderId());
    opprettOppgave(new OpprettVurderKonsekvensYtelseOppgaveRequest(
        journalpost,
        tema,
        aktorId,
        hentSaksbehandlerMedEnhet(journalpost),
        kommentar
    ));
  }

  public void behandleDokument(Journalpost journalpost) {
    var oppgaver = finnBehandlingsoppgaverForSaker(journalpost.hentTilknyttetSaker(), journalpost.getTema());

    if (!oppgaver.isEmpty()) {
      endreSamtOpprettBehandlingsoppgaver(journalpost, oppgaver, journalpost.getJournalforendeEnhet());
    } else {
      opprettOppgaveForBehandleDokument(journalpost, journalpost.hentTilknyttetSaker());
    }
  }

  private SaksbehandlerMedEnhet hentSaksbehandlerMedEnhet(Journalpost journalpost){
    return saksbehandlerInfoManager.hentSaksbehandler()
        .map(saksbehandler -> saksbehandler.tilEnhet(journalpost.getJournalforendeEnhet()))
        .orElseGet(() -> new SaksbehandlerMedEnhet(new Saksbehandler(), journalpost.getJournalforendeEnhet()));
  }

  private void opprettOppgaveForBehandleDokument(Journalpost journalpost, Set<String> unikeSaksnummer) {
    var aktorId = hentAktorId(journalpost.hentGjelderId());

    unikeSaksnummer.forEach(
        saksnummer -> opprettOppgave(new OpprettBehandleDokumentOppgaveRequest(
            journalpost,
            aktorId,
            saksnummer,
            hentSaksbehandlerMedEnhet(journalpost)
        ))
    );
  }

  private long opprettOppgave(OpprettOppgaveRequest request){
    return oppgaveConsumers.get(Discriminator.REGULAR_USER).opprett(request);
  }

  private void endreOppgaverForBehandleDokument(Journalpost journalpost, List<OppgaveData> oppgaverMedBeskrivelse, String enhet) {
    var saksbehandlerInfo = saksbehandlerInfoManager.hentSaksbehandler()
        .map(saksbehandler -> saksbehandler.hentSaksbehandlerInfo(journalpost.getJournalforendeEnhet()))
        .orElse(String.format(
            "Ingen informasjon for saksbehandler (%s, %s)", saksbehandlerInfoManager.hentSaksbehandlerBrukerId(), journalpost.getJournalforendeEnhet()
        ));
    LOGGER.info("Antall behandle dokument oppgaver: {}", oppgaverMedBeskrivelse.size());

    for (var oppgaveData : oppgaverMedBeskrivelse) {
      var request = new EndreForNyttDokumentRequest(oppgaveData, saksbehandlerInfo, journalpost, enhet);
      LOGGER.info("Oppgave (id: {}) har beskrivelse: {}", oppgaveData.getId(), request.getBeskrivelse());

      oppgaveConsumers.get(Discriminator.SERVICE_USER).patchOppgave(request);
    }
  }

  private void endreSamtOpprettBehandlingsoppgaver(Journalpost journalpost, List<OppgaveData> oppgaver, String enhet) {
    endreOppgaverForBehandleDokument(journalpost, oppgaver, enhet);
    var saksreferanser = oppgaver.stream().map(OppgaveData::getSaksreferanse).collect(Collectors.toSet());
    var unikeSaksnummer = journalpost.hentTilknyttetSaker();
    unikeSaksnummer.removeAll(saksreferanser);

    if (!unikeSaksnummer.isEmpty()) {
      opprettOppgaveForBehandleDokument(journalpost, unikeSaksnummer);
    }
  }

  private String hentAktorId(String gjelder) {
    var personResponse = personConsumers.get(Discriminator.REGULAR_USER).hentPerson(gjelder);
    if (personResponse.getResponseEntity() == null){
      return gjelder;
    }
    var muligIdent = personResponse.fetchBody();
    var ident = muligIdent.orElseGet(() -> {
      LOGGER.warn("Fant ikke identinfo for {}.", gjelder);
      return new PersonResponse(gjelder, gjelder);
    });
    return ident.getAktoerId();
  }

  private String notNull(String fagomrade) {
    return fagomrade != null ? fagomrade : "BID";
  }

  private List<OppgaveData> finnBehandlingsoppgaverForSaker(Set<String> saksnumre, String fagomrade) {
    var parametre = new OppgaveSokParametre()
        .leggTilFagomrade(notNull(fagomrade))
        .brukBehandlingSomOppgaveType();

    saksnumre.forEach(parametre::leggTilSaksreferanse);

    return oppgaveConsumers.get(Discriminator.SERVICE_USER).finnOppgaver(parametre).getOppgaver();
  }
}
