package no.nav.bidrag.dokument.arkiv.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.BrukerType;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.LagreJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.arkiv.model.FerdigstillFeiletException;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.LagreJournalpostFeiletException;
import no.nav.bidrag.dokument.arkiv.model.PersonException;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalpostService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JournalpostService.class);

  private final SafConsumer safConsumer;
  private final PersonConsumer personConsumer;
  private final DokarkivConsumer dokarkivConsumer;

  public JournalpostService(SafConsumer safConsumer, PersonConsumer personConsumer, DokarkivConsumer dokarkivConsumer) {
    this.safConsumer = safConsumer;
    this.personConsumer = personConsumer;
    this.dokarkivConsumer = dokarkivConsumer;
  }

  public Optional<Journalpost> hentJournalpost(Long journalpostId) {
    return hentJournalpost(journalpostId, null);
  }

  public Optional<Journalpost> hentJournalpost(Long journalpostId, String saksnummer) {
    var journalpost = safConsumer.hentJournalpost(journalpostId);

    if (Objects.isNull(journalpost) || journalpost.erIkkeTilknyttetSakNarOppgitt(saksnummer)) {
      return Optional.empty();
    }

    return Optional.of(journalpost);
  }

  public Optional<Journalpost> hentJournalpostMedFnr(Long journalpostId, String saksummer) {
    var journalpost = hentJournalpost(journalpostId, saksummer);
    return journalpost.map(this::convertAktoerIdToFnr);
  }

  public Optional<Journalpost> hentJournalpostMedAktorId(Long journalpostId) {
    var journalpost = hentJournalpost(journalpostId);
    return journalpost.map(this::convertFnrToAktorId);
  }

  public List<JournalpostDto> finnJournalposter(String saksnummer, String fagomrade) {
    var journalposterResponse = safConsumer.finnJournalposter(saksnummer, fagomrade);
    return journalposterResponse.stream()
        .map((this::convertAktoerIdToFnr))
        .map(Journalpost::tilJournalpostDto)
        .collect(Collectors.toList());
  }

  private Journalpost convertAktoerIdToFnr(Journalpost journalpost) {
    var bruker = journalpost.getBruker();
    if (Objects.isNull(bruker) || !journalpost.getBruker().isAktoerId()) {
      return journalpost;
    }

    var personResponse = hentPerson(bruker.getId());
    var brukerId = personResponse.getIdent();
    journalpost.setBruker(new Bruker(brukerId, BrukerType.FNR.name()));
    return journalpost;
  }

  private Journalpost convertFnrToAktorId(Journalpost journalpost) {
    var bruker = journalpost.getBruker();
    if (Objects.isNull(bruker) || journalpost.getBruker().isAktoerId()) {
      return journalpost;
    }

    var personResponse = hentPerson(bruker.getId());
    var brukerId = personResponse.getAktoerId();
    journalpost.setBruker(new Bruker(brukerId, BrukerType.AKTOERID.name()));
    return journalpost;
  }

  private PersonResponse hentPerson(String personId) {
    var personResponse = personConsumer.hentPerson(personId);
    if (!personResponse.is2xxSuccessful()) {
      throw new PersonException("Det skjedde en feil ved henting av person");
    }
    return personResponse.getResponseEntity().getBody();
  }

  public HttpResponse<Void> endre(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand) {
    var oppdatertJournalpostResponse = lagreJournalpost(journalpostId, endreJournalpostCommand);

    if (endreJournalpostCommand.skalJournalfores()) {
      journalfoerJournalpost(journalpostId, endreJournalpostCommand.getEnhet());
    }

    return HttpResponse.from(
        oppdatertJournalpostResponse.fetchHeaders(),
        oppdatertJournalpostResponse.getResponseEntity().getStatusCode()
    );
  }

  private HttpResponse<OppdaterJournalpostResponse> lagreJournalpost(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand){
    var journalpost = hentJournalpost(journalpostId).orElseThrow(
        () -> new JournalpostIkkeFunnetException("Kunne ikke finne journalpost med id: " + journalpostId)
    );
    var oppdaterJournalpostRequest = new LagreJournalpostRequest(journalpostId, endreJournalpostCommand, journalpost);
    var oppdatertJournalpostResponse = dokarkivConsumer.endre(oppdaterJournalpostRequest);

    oppdatertJournalpostResponse.fetchBody().ifPresent(response -> LOGGER.info("endret: {}", response));

    if (!oppdatertJournalpostResponse.is2xxSuccessful()){
      throw new LagreJournalpostFeiletException(String.format("Lagre journalpost feilet for journalpostid %s", journalpostId));
    }

    return oppdatertJournalpostResponse;
  }

  public void journalfoerJournalpost(Long journalpostId, String enhet){
    var journalforRequest = new FerdigstillJournalpostRequest(journalpostId, enhet);
    var ferdigstillResponse = dokarkivConsumer.ferdigstill(journalforRequest);
    if (!ferdigstillResponse.is2xxSuccessful()){
      throw new FerdigstillFeiletException(String.format("Ferdigstill journalpost feilet med feilmelding %s", ferdigstillResponse.fetchBody().orElse("")));
    }
    LOGGER.info("Journalpost med id {} er ferdigstillt", journalpostId);
  }
}
