package no.nav.bidrag.dokument.arkiv.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.BrukerType;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.arkiv.dto.Sak;
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost;
import no.nav.bidrag.dokument.arkiv.model.PersonException;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class JournalpostService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JournalpostService.class);

  private final SafConsumer safConsumer;
  private final PersonConsumer personConsumer;

  public JournalpostService(SafConsumer safConsumer, PersonConsumer personConsumer) {
    this.safConsumer = safConsumer;
    this.personConsumer = personConsumer;
  }

  public Optional<Journalpost> hentJournalpost(Long journalpostId) {
    return hentJournalpost(journalpostId, null);
  }

  public Optional<Journalpost> hentJournalpostMedFnrOgTilknyttedeSaker(Long journalpostId, String saksnummer) {
    var jpOptional = hentJournalpostMedFnr(journalpostId, saksnummer);
    if (jpOptional.isEmpty()){
      return jpOptional;
    }
    return Optional.of(populerMedTilknyttedeSaker(jpOptional.get()));
  }

  public Optional<Journalpost> hentJournalpostMedAktorId(Long journalpostId) {
    var journalpost = hentJournalpost(journalpostId);
    return journalpost.map(this::konverterFnrTilAktorId);
  }

  public List<JournalpostDto> finnJournalposter(String saksnummer, String fagomrade) {
    return finnJournalposterForSaksnummer(saksnummer, fagomrade).stream()
        .map((this::konverterAktoerIdTilFnr))
        .filter((jp)-> !(jp.getTilleggsopplysninger().isEndretTema() || jp.getTilleggsopplysninger().isNyDistribusjonBestilt()) )
        .map(Journalpost::tilJournalpostDto)
        .collect(Collectors.toList());
  }

  private Optional<Journalpost> hentJournalpost(Long journalpostId, String saksnummer) {
    var journalpost = safConsumer.hentJournalpost(journalpostId);

    if (Objects.isNull(journalpost) || journalpost.erIkkeTilknyttetSakNarOppgitt(saksnummer)) {
      return Optional.empty();
    }

    return Optional.of(journalpost);
  }

  protected List<TilknyttetJournalpost> hentTilknyttedeJournalposter(Journalpost journalpost){
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
        .filter(fagsakId -> !Objects.equals(fagsakId, journalpostFagsakId)).toList();
    var sakerNoDuplicates = new HashSet<>(saker).stream().toList();
    LOGGER.info("Fant {} saker for journalpost {}", saker.size() + 1, journalpost.getJournalpostId());
    journalpost.setTilknyttedeSaker(sakerNoDuplicates);
    return journalpost;
  }

  private Optional<Journalpost> hentJournalpostMedFnr(Long journalpostId, String saksummer) {
    var journalpost = hentJournalpost(journalpostId, saksummer);
    return journalpost.map(this::konverterAktoerIdTilFnr);
  }

  public List<Journalpost> finnJournalposterForSaksnummer(String saksnummer, String fagomrade) {
    return safConsumer.finnJournalposter(saksnummer, fagomrade);
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
    var response =  personResponse.getResponseEntity().getBody();
    if (Objects.isNull(response)){
      LOGGER.error("Fant ingen person med id {}", personId);
      return new PersonResponse(personId, personId);
    }
    return response;
  }
}
