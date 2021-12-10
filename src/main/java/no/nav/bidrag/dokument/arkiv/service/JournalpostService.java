package no.nav.bidrag.dokument.arkiv.service;

import java.util.ArrayList;
import java.util.HashSet;
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
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer;
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
  private final HendelserProducer hendelserProducer;

  public JournalpostService(SafConsumer safConsumer, PersonConsumer personConsumer, DokarkivConsumer dokarkivConsumer,
      DokarkivProxyConsumer dokarkivProxyConsumer, HendelserProducer hendelserProducer) {
    this.safConsumer = safConsumer;
    this.personConsumer = personConsumer;
    this.dokarkivConsumer = dokarkivConsumer;
    this.dokarkivProxyConsumer = dokarkivProxyConsumer;
    this.hendelserProducer = hendelserProducer;
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

  public List<TilknyttetJournalpost> hentTilknyttedeJournalposter(Journalpost journalpost){
    if (journalpost.getDokumenter().isEmpty() || journalpost.getSak() == null){
      return List.of();
    }
    var dokumentInfoId = journalpost.getDokumenter().get(0).getDokumentInfoId();
    LOGGER.info(String.format("Henter tilknyttede journalposter for journalpost %s med dokumentinfoId %s", journalpost.getJournalpostId(), dokumentInfoId));
    return Optional.ofNullable(dokumentInfoId).map(safConsumer::finnTilknyttedeJournalposter).orElse(new ArrayList<>());
  }

  public Journalpost populerMedTilknyttedeSaker(Journalpost journalpost){
      var journalpostFagsakId = journalpost.getSak() != null ? journalpost.getSak().getFagsakId() : "";
      var tilknytteteJournalposter = hentTilknyttedeJournalposter(journalpost);
      var saker = tilknytteteJournalposter.stream()
          .map(TilknyttetJournalpost::getSak)
          .filter(Objects::nonNull)
          .map(Sak::getFagsakId)
          .filter(Objects::nonNull)
          .filter(fagsakId -> !Objects.equals(fagsakId, journalpostFagsakId))
          .collect(Collectors.toList());
      var sakerNoDuplicates = new HashSet<>(saker).stream().toList();
      LOGGER.info("Fant {} saker for journalpost {}. Journalposten har {} saker etter fjerning av duplikater", saker.size() + 1, journalpost.getJournalpostId(), sakerNoDuplicates.size() + 1);
      journalpost.setTilknyttedeSaker(sakerNoDuplicates);
      return journalpost;
  }

  public Optional<Journalpost> hentJournalpostMedFnr(Long journalpostId, String saksummer) {
    var journalpost = hentJournalpost(journalpostId, saksummer);
    return journalpost.map(this::konverterAktoerIdTilFnr);
  }

  public Optional<Journalpost> hentJournalpostMedAktorId(Long journalpostId) {
    var journalpost = hentJournalpost(journalpostId);
    return journalpost.map(this::konverterFnrTilAktorId);
  }

  public List<Journalpost> finnJournalposterForSaksnummer(String saksnummer, String fagomrade) {
    return safConsumer.finnJournalposter(saksnummer, fagomrade);
  }

  public List<JournalpostDto> finnJournalposter(String saksnummer, String fagomrade) {
    return finnJournalposterForSaksnummer(saksnummer, fagomrade).stream()
        .map((this::konverterAktoerIdTilFnr))
        .map(Journalpost::tilJournalpostDto)
        .collect(Collectors.toList());
  }

  public HttpResponse<Void> endre(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand) {
    var journalpost = hentJournalpost(journalpostId).orElseThrow(
        () -> new JournalpostIkkeFunnetException("Kunne ikke finne journalpost med id: " + journalpostId)
    );
    var oppdatertJournalpostResponse = lagreJournalpost(journalpostId, endreJournalpostCommand, journalpost);

    journalfoerJournalpostNarMottaksregistrert(endreJournalpostCommand, journalpost);
    tilknyttSakerTilJournalfoertJournalpost(endreJournalpostCommand, journalpost);

    return HttpResponse.from(
        oppdatertJournalpostResponse.fetchHeaders(),
        oppdatertJournalpostResponse.getResponseEntity().getStatusCode()
    );
  }

  private Journalpost konverterAktoerIdTilFnr(Journalpost journalpost) {
    var bruker = journalpost.getBruker();
    if (Objects.isNull(bruker) || !journalpost.getBruker().isAktoerId()) {
      return journalpost;
    }

    var personResponse = hentPerson(bruker.getId());
    var brukerId = personResponse.getIdent();
    journalpost.setBruker(new Bruker(brukerId, BrukerType.FNR.name()));
    return journalpost;
  }

  private Journalpost konverterFnrTilAktorId(Journalpost journalpost) {
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

  private HttpResponse<OppdaterJournalpostResponse> lagreJournalpost(Long journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost){
    var oppdaterJournalpostRequest = new LagreJournalpostRequest(journalpostId, endreJournalpostCommand, journalpost);
    var oppdatertJournalpostResponse = dokarkivConsumer.endre(oppdaterJournalpostRequest);

    oppdatertJournalpostResponse.fetchBody().ifPresent(response -> LOGGER.info("endret: {}", response));

    if (!oppdatertJournalpostResponse.is2xxSuccessful()){
      throw new LagreJournalpostFeiletException(String.format("Lagre journalpost feilet for journalpostid %s", journalpostId));
    }

    if (Objects.nonNull(oppdaterJournalpostRequest.getSak())){
      journalpost.setSak(new Sak(oppdaterJournalpostRequest.getSak().getFagsakId()));
    }

    if (endreJournalpostCommand.harGjelder()){
      hendelserProducer.publishJournalpostUpdated(journalpostId);
    }

    return oppdatertJournalpostResponse;
  }

  private void journalfoerJournalpostNarMottaksregistrert(EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost){
    if (endreJournalpostCommand.skalJournalfores() && journalpost.isStatusMottatt()) {
      var journalpostId = journalpost.hentJournalpostIdLong();
      journalfoerJournalpost(journalpostId, endreJournalpostCommand);
      journalpost.setJournalstatus(JournalStatus.JOURNALFOERT);
    }
  }

  private void tilknyttSakerTilJournalfoertJournalpost(EndreJournalpostCommandIntern endreJournalpostCommand, Journalpost journalpost){
    if (journalpost.isStatusJournalfort() || journalpost.isStatusFerdigsstilt()) {
      populerMedTilknyttedeSaker(journalpost);
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
