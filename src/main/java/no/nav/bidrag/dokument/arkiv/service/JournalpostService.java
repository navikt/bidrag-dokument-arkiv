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

    if (journalpost.erIkkeTilknyttetSakNarOppgitt(saksnummer)) {
      return Optional.empty();
    }

    return Optional.of(this.convertAktoerIdToFnr(journalpost));
  }

  public Optional<Journalpost> hentJournalpostMedAktorId(Long journalpostId) {
    var journalpost = safConsumer.hentJournalpost(journalpostId);
    return Optional.of(this.convertFnrToAktorId(journalpost));
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

    var personResponse = hentPerson(journalpost);
    var brukerId = personResponse.getIdent();
    journalpost.setBruker(new Bruker(brukerId, "FNR"));
    return journalpost;
  }

  private Journalpost convertFnrToAktorId(Journalpost journalpost) {
    var bruker = journalpost.getBruker();
    if (Objects.isNull(bruker) || journalpost.getBruker().isAktoerId()) {
      return journalpost;
    }

    var personResponse = hentPerson(journalpost);
    var brukerId = personResponse.getAktoerId();
    journalpost.setBruker(new Bruker(brukerId, "AKTORID"));
    return journalpost;
  }

  private PersonResponse hentPerson(Journalpost journalpost) {
    var bruker = journalpost.getBruker();
    var personResponse = personConsumer.hentPerson(bruker.getId());
    if (!personResponse.is2xxSuccessful()) {
      throw new PersonException("Det skjedde en feil ved konvertering av Aktørid til fødslesnummer", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return personResponse.getResponseEntity().getBody();
  }

  public HttpResponse<Void> endre(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand) {
    var journalpost = hentJournalpost(journalpostId).orElseThrow(
        () -> new JournalpostIkkeFunnetException("Kunne ikke finne journalpost med id: " + journalpostId)
    );

    var oppdaterJournalpostRequest = new OppdaterJournalpostRequest(journalpostId, endreJournalpostCommand, journalpost);
    var oppdatertJournalpostResponse = dokarkivConsumer.endre(oppdaterJournalpostRequest);

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
