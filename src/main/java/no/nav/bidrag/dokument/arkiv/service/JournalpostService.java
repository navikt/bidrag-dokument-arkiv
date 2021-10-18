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
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.LagreJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.PersonException;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

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
    journalpost.setBruker(new Bruker(brukerId, "FNR"));
    return journalpost;
  }

  private Journalpost convertFnrToAktorId(Journalpost journalpost) {
    var bruker = journalpost.getBruker();
    if (Objects.isNull(bruker) || journalpost.getBruker().isAktoerId()) {
      return journalpost;
    }

    var personResponse = hentPerson(bruker.getId());
    var brukerId = personResponse.getAktoerId();
    journalpost.setBruker(new Bruker(brukerId, "AKTOERID"));
    return journalpost;
  }

  private PersonResponse hentPerson(String personId) {
    var personResponse = personConsumer.hentPerson(personId);
    if (!personResponse.is2xxSuccessful()) {
      throw new PersonException("Det skjedde en feil ved henting av person", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return personResponse.getResponseEntity().getBody();
  }

  public HttpResponse<Void> oppdater(OppdaterJournalpostRequest oppdaterJournalpostRequest) {
    var oppdatertJournalpostResponse = dokarkivConsumer.endre(oppdaterJournalpostRequest);

    oppdatertJournalpostResponse.fetchBody().ifPresent(response -> LOGGER.info("endret: {}", response));

    return HttpResponse.from(
        oppdatertJournalpostResponse.fetchHeaders(),
        oppdatertJournalpostResponse.getResponseEntity().getStatusCode()
    );
  }

  public HttpResponse<Void> trekkJournalpost(Long journalpostId){
    dokarkivConsumer.settStatusUtgaar(journalpostId);
    return HttpResponse.from(HttpStatus.OK);
  }

  public HttpResponse<Void> feilforSak(Long journalpostId){
    dokarkivConsumer.feilregistrerSakstilknytning(journalpostId);
    return HttpResponse.from(HttpStatus.OK);
  }

  public HttpResponse<Void> endre(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand) {
    var journalpost = hentJournalpost(journalpostId).orElseThrow(
        () -> new JournalpostIkkeFunnetException("Kunne ikke finne journalpost med id: " + journalpostId)
    );

    var oppdaterJournalpostRequest = new LagreJournalpostRequest(journalpostId, endreJournalpostCommand, journalpost);
    var oppdatertJournalpostResponse = oppdater(oppdaterJournalpostRequest);

    if (oppdatertJournalpostResponse.is2xxSuccessful() && endreJournalpostCommand.skalJournalfores()) {
      var journalforRequest = new FerdigstillJournalpostRequest(journalpostId, endreJournalpostCommand.getEnhet());
      dokarkivConsumer.ferdigstill(journalforRequest);
      LOGGER.info("Journalpost med id {} er ferdigstillt", journalpostId);
    }

    oppdatertJournalpostResponse.fetchBody().ifPresent(response -> LOGGER.info("endret: {}", response));

    return HttpResponse.from(
        oppdatertJournalpostResponse.fetchHeaders(),
        oppdatertJournalpostResponse.getResponseEntity().getStatusCode()
    );
  }
}
