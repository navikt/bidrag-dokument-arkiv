package no.nav.bidrag.dokument.arkiv.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivProxyConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.BrukerType;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakRequest;
import no.nav.bidrag.dokument.arkiv.dto.LagreJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.arkiv.dto.Sak;
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost;
import no.nav.bidrag.dokument.arkiv.model.FerdigstillFeiletException;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.LagreJournalpostFeiletException;
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
  private final DokarkivProxyConsumer dokarkivProxyConsumer;

  public JournalpostService(SafConsumer safConsumer, PersonConsumer personConsumer, DokarkivConsumer dokarkivConsumer,
      DokarkivProxyConsumer dokarkivProxyConsumer) {
    this.safConsumer = safConsumer;
    this.personConsumer = personConsumer;
    this.dokarkivConsumer = dokarkivConsumer;
    this.dokarkivProxyConsumer = dokarkivProxyConsumer;
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

  public Journalpost populateWithTilknyttedeSaker(Journalpost journalpost){
    if (journalpost.getDokumenter().isEmpty() || journalpost.getSak() == null){
      return journalpost;
    }
    var dokumentInfoId = journalpost.getDokumenter().get(0).getDokumentInfoId();
    if (dokumentInfoId != null){
      var tilknytteteJournalposter = safConsumer.finnTilknyttedeJournalposter(dokumentInfoId);
      var saker = tilknytteteJournalposter.stream()
          .map(TilknyttetJournalpost::getSak)
          .filter(Objects::nonNull)
          .map(Sak::getFagsakId)
          .filter(Objects::nonNull)
          .filter(fagsakId -> !Objects.equals(fagsakId, journalpost.getSak().getFagsakId()))
          .collect(Collectors.toList());
      journalpost.setTilknyttedeSaker(saker);
    }
    return journalpost;
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
      throw new PersonException("Det skjedde en feil ved henting av person", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return personResponse.getResponseEntity().getBody();
  }

  public HttpResponse<Void> endre(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand) {
    var journalpost = hentJournalpost(journalpostId).orElseThrow(
        () -> new JournalpostIkkeFunnetException("Kunne ikke finne journalpost med id: " + journalpostId)
    );
    var oppdatertJournalpostResponse = lagreJournalpost(journalpostId, endreJournalpostCommand, journalpost);

    if (endreJournalpostCommand.skalJournalfores()) {
      journalfoerJournalpost(journalpostId, endreJournalpostCommand);
      journalpost.setJournalstatus(JournalStatus.JOURNALFOERT);
    }

    tilknyttSaker(endreJournalpostCommand, journalpost);

    return HttpResponse.from(
        oppdatertJournalpostResponse.fetchHeaders(),
        oppdatertJournalpostResponse.getResponseEntity().getStatusCode()
    );
  }

  private HttpResponse<OppdaterJournalpostResponse> lagreJournalpost(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost){
    var oppdaterJournalpostRequest = new LagreJournalpostRequest(journalpostId, endreJournalpostCommand, journalpost);
    var oppdatertJournalpostResponse = dokarkivConsumer.endre(oppdaterJournalpostRequest);

    oppdatertJournalpostResponse.fetchBody().ifPresent(response -> LOGGER.info("endret: {}", response));

    if (!oppdatertJournalpostResponse.is2xxSuccessful()){
      throw new LagreJournalpostFeiletException(String.format("Lagre journalpost feilet for journalpostid %s", journalpostId));
    }

    return oppdatertJournalpostResponse;
  }

  private void tilknyttSaker(EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost){
    if (journalpost.isStatusJournalfort()) {
      populateWithTilknyttedeSaker(journalpost);
      endreJournalpostCommand.hentTilknyttetSaker().stream()
          .filter(sak -> !journalpost.hentTilknyttetSaker().contains(sak))
          .collect(Collectors.toSet())
          .forEach(saksnummer -> tilknyttTilSak(saksnummer, journalpost));
    }
  }

  private void tilknyttTilSak(String saksnummer, Journalpost journalpost){
    KnyttTilAnnenSakRequest knyttTilAnnenSakRequest = new KnyttTilAnnenSakRequest(saksnummer, journalpost);
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
