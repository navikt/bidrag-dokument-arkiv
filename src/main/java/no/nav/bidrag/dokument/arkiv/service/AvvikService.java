package no.nav.bidrag.dokument.arkiv.service;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;
import static no.nav.bidrag.dokument.arkiv.dto.ViolationKt.validateTrue;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker;
import no.nav.bidrag.dokument.arkiv.dto.AvvikshendelseIntern;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal;
import no.nav.bidrag.dokument.arkiv.dto.LagreAvsenderNavnRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OpphevEndreFagomradeJournalfortJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OpprettJournalpost;
import no.nav.bidrag.dokument.arkiv.dto.RegistrerReturRequest;
import no.nav.bidrag.dokument.arkiv.dto.ReturDetaljerLogDO;
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer;
import no.nav.bidrag.dokument.arkiv.model.AvvikDetaljException;
import no.nav.bidrag.dokument.arkiv.model.AvvikNotSupportedException;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.model.UgyldigAvvikException;
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager;
import no.nav.bidrag.dokument.dto.AvvikType;
import no.nav.bidrag.dokument.dto.BehandleAvvikshendelseResponse;
import no.nav.bidrag.dokument.dto.JournalpostIkkeFunnetException;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AvvikService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AvvikService.class);

  public final JournalpostService journalpostService;
  public final HendelserProducer hendelserProducer;
  public final EndreJournalpostService endreJournalpostService;
  public final DistribuerJournalpostService distribuerJournalpostService;
  public final OppgaveService oppgaveService;
  private final DokarkivConsumer dokarkivConsumer;
  private final PersonConsumer personConsumer;
  private final BidragOrganisasjonConsumer bidragOrganisasjonConsumer;
  private final SaksbehandlerInfoManager saksbehandlerInfoManager;

  private final OpprettJournalpostService opprettJournalpostService;

  public AvvikService(ResourceByDiscriminator<JournalpostService> journalpostService, HendelserProducer hendelserProducer,
      EndreJournalpostService endreJournalpostService, DistribuerJournalpostService distribuerJournalpostService, OppgaveService oppgaveService,
      ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers,
      ResourceByDiscriminator<PersonConsumer> personConsumers, BidragOrganisasjonConsumer bidragOrganisasjonConsumer,
      SaksbehandlerInfoManager saksbehandlerInfoManager, OpprettJournalpostService opprettJournalpostService) {
    this.journalpostService = journalpostService.get(Discriminator.REGULAR_USER);
    this.hendelserProducer = hendelserProducer;
    this.endreJournalpostService = endreJournalpostService;
    this.distribuerJournalpostService = distribuerJournalpostService;
    this.oppgaveService = oppgaveService;
    this.personConsumer = personConsumers.get(Discriminator.REGULAR_USER);
    this.bidragOrganisasjonConsumer = bidragOrganisasjonConsumer;
    this.saksbehandlerInfoManager = saksbehandlerInfoManager;
    this.dokarkivConsumer = dokarkivConsumers.get(Discriminator.REGULAR_USER);
    this.opprettJournalpostService = opprettJournalpostService;
  }

  public List<AvvikType> hentAvvik(Long jpid) {
    return journalpostService.hentJournalpost(jpid).get().tilAvvik();
  }

  public Optional<BehandleAvvikshendelseResponse> behandleAvvik(AvvikshendelseIntern behandleAvvikRequest) {
    return journalpostService.hentJournalpost(behandleAvvikRequest.getJournalpostId())
        .map(jp -> behandleAvvik(jp, behandleAvvikRequest))
        .orElseThrow(() -> new JournalpostIkkeFunnetException("Fant ikke journalpost med id lik " + behandleAvvikRequest.getJournalpostId()));
  }

  public Optional<BehandleAvvikshendelseResponse> behandleAvvik(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern) {
    if (!erGyldigAvviksBehandling(journalpost, avvikshendelseIntern.getAvvikstype())) {
      throw new UgyldigAvvikException(String.format("Ikke gyldig avviksbehandling %s for journalpost %s", avvikshendelseIntern.getAvvikstype(),
          avvikshendelseIntern.getJournalpostId()));
    }

    switch (avvikshendelseIntern.getAvvikstype()) {
      case KOPIER_FRA_ANNEN_FAGOMRADE -> kopierFraAnnenFagomrade(journalpost, avvikshendelseIntern);
      case OVERFOR_TIL_ANNEN_ENHET -> oppdater(avvikshendelseIntern.toOverforEnhetRequest());
      case ENDRE_FAGOMRADE -> endreFagomrade(journalpost, avvikshendelseIntern);
      case SEND_TIL_FAGOMRADE -> onlyLogging();
      case TREKK_JOURNALPOST -> trekkJournalpost(journalpost, avvikshendelseIntern);
      case FEILFORE_SAK -> feilregistrerSakstilknytning(avvikshendelseIntern.getJournalpostId());
      case REGISTRER_RETUR -> registrerRetur(journalpost, avvikshendelseIntern);
      case BESTILL_NY_DISTRIBUSJON -> bestillNyDistribusjon(journalpost, avvikshendelseIntern);
      case MANGLER_ADRESSE -> manglerAdresse(journalpost);
      default -> throw new AvvikNotSupportedException("Avvik %s ikke støttet".formatted(avvikshendelseIntern.getAvvikstype()));
    }

    publiserHendelse(journalpost, avvikshendelseIntern.getSaksbehandlersEnhet());
    SECURE_LOGGER.info("Avvik {} ble utført på journalpost {} av bruker {} og enhet {} med beskrivelse {} - avvik {}",
        avvikshendelseIntern.getAvvikstype(), avvikshendelseIntern.getJournalpostId(), saksbehandlerInfoManager.hentSaksbehandlerBrukerId(),
        avvikshendelseIntern.getSaksbehandlersEnhet(), avvikshendelseIntern.getBeskrivelse(), avvikshendelseIntern);

    return Optional.of(new BehandleAvvikshendelseResponse(avvikshendelseIntern.getAvvikstype()));
  }

  private void publiserHendelse(Journalpost journalpost, String enhet) {
    if (journalpost.isInngaaendeDokument()) {
      hendelserProducer.publishJournalpostUpdated(journalpost.hentJournalpostIdLong(), enhet);
    }
  }

  public void kopierFraAnnenFagomrade(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern) {
    if (journalpost.isUtgaaendeDokument()) {
      throw new UgyldigAvvikException("Kan ikke kopiere en utgående dokument fra annen fagområde");
    }

    if (journalpost.isTemaBidrag()) {
      throw new UgyldigAvvikException("Kan ikke kopiere journalpost som allerede tilhører Bidrag");
    }

    if (avvikshendelseIntern.getDokumenter() != null && avvikshendelseIntern.getDokumenter().isEmpty()) {
      throw new UgyldigAvvikException("Kopiert journalpost må inneholde minst en dokument");
    }

    var knyttTilSaker = avvikshendelseIntern.getKnyttTilSaker();

    if (knyttTilSaker.isEmpty()) {
      throw new UgyldigAvvikException("Fant ingen saker i request. Journalpost må knyttes til minst en sak");
    }

    var hoveddokumentTittel = avvikshendelseIntern.getDokumenter().get(0).getTittel();
    var nyJournalpostTittel = hoveddokumentTittel != null ? hoveddokumentTittel : journalpost.hentTittel();
    var request = new OpprettJournalpost()
        .kopierFra(journalpost)
        .medJournalforendeEnhet(avvikshendelseIntern.getSaksbehandlersEnhet());

    avvikshendelseIntern.getDokumenter().forEach((dok) -> {
      var dokumentByte = Strings.isNotEmpty(dok.getDokument()) ? Base64.getDecoder().decode(dok.getDokument()) : null;
      request.medDokument(dok.getDokumentreferanse(), dokumentByte, dok.getTittel(), dok.getBrevkode());
    });

    request.medTittel(String.format("%s (Kopiert fra dokument: %s)", nyJournalpostTittel, journalpost.hentTittel()));

    var response = opprettJournalpostService.opprettOgJournalforJournalpost(request, avvikshendelseIntern.getKnyttTilSaker());
    LOGGER.info("Kopiert journalpost {} til Bidrag, ny journalpostId {}", journalpost.getJournalpostId(), response.getJournalpostId());

    oppgaveService.ferdigstillVurderDokumentOppgaver(journalpost.hentJournalpostIdLong(), avvikshendelseIntern.getSaksbehandlersEnhet());

  }

  public void manglerAdresse(Journalpost journalpost) {
    oppdaterDistribusjonsInfoIngenDistribusjon(journalpost);
  }

  public void bestillNyDistribusjon(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern) {
    LOGGER.info("Bestiller ny distribusjon for journalpost {}", journalpost.getJournalpostId());
    if (avvikshendelseIntern.getAdresse() == null) {
      throw new UgyldigAvvikException("Adresse må settes ved bestilling av ny distribusjon");
    }

    distribuerJournalpostService.bestillNyDistribusjon(journalpost, avvikshendelseIntern.getAdresse());
  }

  /**
   * Used when avvikshåndtering is not triggering any action but only used for logging
   */
  public void onlyLogging() {
    // noop
  }

  public void sendTilFagomrade(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern) {
    if (journalpost.isTemaEqualTo(avvikshendelseIntern.getNyttFagomrade())) {
      return;
    }

    if (avvikshendelseIntern.isBidragFagomrade()) {
      throw new UgyldigAvvikException("Kan ikke sende journalpost mellom FAR og BID tema.");
    }

    var journalforendeEnhet = bidragOrganisasjonConsumer.hentGeografiskEnhet(journalpost.hentGjelderId(), avvikshendelseIntern.getNyttFagomrade());
    var nyJournalpostId = endreJournalpostService.tilknyttTilGenerellSak(avvikshendelseIntern.getNyttFagomrade(), journalpost);
    oppgaveService.opprettVurderDokumentOppgave(journalpost, nyJournalpostId, journalforendeEnhet, avvikshendelseIntern.getNyttFagomrade(),
        avvikshendelseIntern.getBeskrivelse());
  }

  private void knyttTilSakPaaNyttFagomrade(AvvikshendelseIntern avvikshendelseIntern, Journalpost journalpost) {
    var saksnummer = journalpost.getSak().getFagsakId();
    hentFeilregistrerteDupliserteJournalposterMedSakOgTema(saksnummer, avvikshendelseIntern.getNyttFagomrade(), journalpost)
        .findFirst()
        .ifPresentOrElse(
            jp -> {
              opphevFeilregistrerSakstilknytning(jp.getJournalpostId());
              oppdater(new OpphevEndreFagomradeJournalfortJournalpostRequest(jp.hentJournalpostIdLong(), jp));
            },
            () -> endreJournalpostService.tilknyttTilSak(saksnummer, avvikshendelseIntern.getNyttFagomrade(), journalpost)
        );
  }

  private Stream<Journalpost> hentFeilregistrerteDupliserteJournalposterMedSakOgTema(String saksnummer, String tema, Journalpost journalpost) {
    return journalpostService.finnJournalposterForSaksnummer(saksnummer, tema).stream()
        .filter(Journalpost::isStatusFeilregistrert)
        .filter(jp -> harSammeDokumenter(jp, journalpost));
  }

  private boolean harSammeDokumenter(Journalpost journalpost1, Journalpost journalpost2) {
    return journalpost1.getDokumenter().stream().allMatch(dokument1 -> journalpost2.getDokumenter().stream()
        .anyMatch(dokument2 -> Objects.equals(dokument2.getDokumentInfoId(), dokument1.getDokumentInfoId())));
  }

  public void endreFagomradeJournalfortJournalpost(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern) {
    if (journalpost.isTemaEqualTo(avvikshendelseIntern.getNyttFagomrade())) {
      return;
    }

    if (avvikshendelseIntern.isBidragFagomrade()) {
      knyttTilSakPaaNyttFagomrade(avvikshendelseIntern, journalpost);
    } else {
      sendTilFagomrade(journalpost, avvikshendelseIntern);
    }

    oppdater(avvikshendelseIntern.toEndreFagomradeJournalfortJournalpostRequest(journalpost));
    feilregistrerSakstilknytning(avvikshendelseIntern.getJournalpostId());
  }

  public void endreFagomrade(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern) {
    if (journalpost.isInngaaendeJournalfort()) {
      endreFagomradeJournalfortJournalpost(journalpost, avvikshendelseIntern);
    } else {
      endreFagomradeMottattJournalpost(journalpost, avvikshendelseIntern);
    }
  }

  private void endreFagomradeMottattJournalpost(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern) {
    if (journalpost.hasSak()) {
      oppdater(avvikshendelseIntern.toEndreFagomradeOgKnyttTilSakRequest(journalpost.getBruker()));
    } else {
      oppdater(avvikshendelseIntern.toEndreFagomradeRequest());
    }
  }

  public void registrerRetur(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern) {
    var returDato = LocalDate.parse(avvikshendelseIntern.getReturDato());
    if (journalpost.hasReturDetaljerWithDate(returDato)) {
      throw new UgyldigAvvikException("Journalpost har allerede registrert retur med samme dato");
    }
    var beskrivelse = Strings.isNotEmpty(avvikshendelseIntern.getBeskrivelse()) ? avvikshendelseIntern.getBeskrivelse() : "";
    var tilleggsOpplysninger = journalpost.getTilleggsopplysninger();
    tilleggsOpplysninger.addReturDetaljLog(new ReturDetaljerLogDO(beskrivelse, returDato, false));
    oppdater(new RegistrerReturRequest(journalpost.hentJournalpostIdLong(), returDato, tilleggsOpplysninger));
  }

  public void trekkJournalpost(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern) {
    // Journalfør på GENERELL_SAK og feilfør sakstilknytning
    validateTrue(journalpost.getBruker() != null, new AvvikDetaljException("Kan ikke trekke journalpost uten bruker"));
    validateTrue(journalpost.getTema() != null, new AvvikDetaljException("Kan ikke trekke journalpost uten tilhørende fagområde"));

    knyttTilGenerellSak(avvikshendelseIntern, journalpost);
    klargjorForFerdigstilling(journalpost);

    dokarkivConsumer.ferdigstill(
        new FerdigstillJournalpostRequest(avvikshendelseIntern.getJournalpostId(), avvikshendelseIntern.getSaksbehandlersEnhet()));

    leggTilBegrunnelsePaaTittel(avvikshendelseIntern, journalpost);

    feilregistrerSakstilknytning(avvikshendelseIntern.getJournalpostId());
  }

  private void klargjorForFerdigstilling(Journalpost journalpost) {
    settAvsenderNavnLikBrukernavnHvisMangler(journalpost);
  }

  public void settAvsenderNavnLikBrukernavnHvisMangler(Journalpost journalpost) {
    if (!journalpost.harAvsenderMottaker()) {
      var brukerNavn = personConsumer.hentPerson(journalpost.getBruker().getId())
          .orElseThrow(() -> new UgyldigAvvikException("Fant ikke person"))
          .getNavn();
      dokarkivConsumer.endre(new LagreAvsenderNavnRequest(journalpost.hentJournalpostIdLong(), brukerNavn));
      journalpost.setAvsenderMottaker(new AvsenderMottaker(brukerNavn, null, null));
    }
  }

  public void knyttTilGenerellSak(AvvikshendelseIntern avvikshendelseIntern, Journalpost journalpost) {
    oppdater(avvikshendelseIntern.toKnyttTilGenerellSakRequest(journalpost.getTema(), journalpost.getBruker()));
  }

  public void leggTilBegrunnelsePaaTittel(AvvikshendelseIntern avvikshendelseIntern, Journalpost journalpost) {
    if (Strings.isNotEmpty(avvikshendelseIntern.getBeskrivelse())) {
      validateTrue(avvikshendelseIntern.getBeskrivelse().length() < 100, new AvvikDetaljException("Beskrivelse kan ikke være lengre enn 100 tegn"));
      oppdater(avvikshendelseIntern.toLeggTilBegrunnelsePaaTittelRequest(journalpost));
    }
  }

  public void feilregistrerSakstilknytning(Long journalpostId) {
    dokarkivConsumer.feilregistrerSakstilknytning(journalpostId);
  }

  public void opphevFeilregistrerSakstilknytning(String journalpostId) {
    dokarkivConsumer.opphevFeilregistrerSakstilknytning(Long.valueOf(journalpostId));
  }

  public void oppdater(OppdaterJournalpostRequest oppdaterJournalpostRequest) {
    dokarkivConsumer.endre(oppdaterJournalpostRequest);
  }

  public Boolean erGyldigAvviksBehandling(Journalpost journalpost, AvvikType avvikType) {
    return journalpost.tilAvvik().contains(avvikType);
  }

  public void oppdaterDistribusjonsInfoIngenDistribusjon(Journalpost journalpost) {
    var tilknyttedeJournalpost = journalpostService.hentTilknyttedeJournalposter(journalpost);
    tilknyttedeJournalpost
        .forEach((jp) -> dokarkivConsumer.oppdaterDistribusjonsInfo(jp.getJournalpostId(), false, JournalpostKanal.INGEN_DISTRIBUSJON));
  }
}
