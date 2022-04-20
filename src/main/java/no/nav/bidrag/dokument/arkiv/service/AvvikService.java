package no.nav.bidrag.dokument.arkiv.service;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.dto.AvvikshendelseIntern;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
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
import org.springframework.stereotype.Service;

@Service
public class AvvikService {

  public final JournalpostService journalpostService;
  public final HendelserProducer hendelserProducer;
  public final EndreJournalpostService endreJournalpostService;
  public final OppgaveService oppgaveService;
  private final DokarkivConsumer dokarkivConsumer;
  private final SaksbehandlerInfoManager saksbehandlerInfoManager;

  public AvvikService(ResourceByDiscriminator<JournalpostService> journalpostService, HendelserProducer hendelserProducer,
      EndreJournalpostService endreJournalpostService, OppgaveService oppgaveService,
      ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers, SaksbehandlerInfoManager saksbehandlerInfoManager) {
    this.journalpostService = journalpostService.get(Discriminator.REGULAR_USER);
    this.hendelserProducer = hendelserProducer;
    this.endreJournalpostService = endreJournalpostService;
    this.oppgaveService = oppgaveService;
    this.saksbehandlerInfoManager = saksbehandlerInfoManager;
    this.dokarkivConsumer = dokarkivConsumers.get(Discriminator.REGULAR_USER);
  }

  public List<AvvikType> hentAvvik(Long jpid){
    return journalpostService.hentJournalpost(jpid).get().tilAvvik();
  }

  public Optional<BehandleAvvikshendelseResponse> behandleAvvik(AvvikshendelseIntern behandleAvvikRequest) {
    return journalpostService.hentJournalpost(behandleAvvikRequest.getJournalpostId())
        .map(jp -> behandleAvvik(jp, behandleAvvikRequest))
        .orElseThrow(() -> new JournalpostIkkeFunnetException("Fant ikke journalpost med id lik " + behandleAvvikRequest.getJournalpostId()));
  }

  public Optional<BehandleAvvikshendelseResponse> behandleAvvik(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern){
    if (!erGyldigAvviksBehandling(journalpost, avvikshendelseIntern.getAvvikstype())){
        throw new UgyldigAvvikException(String.format("Ikke gyldig avviksbehandling %s for journalpost %s", avvikshendelseIntern.getAvvikstype(), avvikshendelseIntern.getJournalpostId()));
    }

    switch (avvikshendelseIntern.getAvvikstype()){
      case OVERFOR_TIL_ANNEN_ENHET -> oppdater(avvikshendelseIntern.toOverforEnhetRequest());
      case ENDRE_FAGOMRADE -> endreFagomrade(journalpost, avvikshendelseIntern);
      case SEND_TIL_FAGOMRADE -> sendTilFagomrade(journalpost, avvikshendelseIntern);
      case TREKK_JOURNALPOST -> trekkJournalpost(journalpost, avvikshendelseIntern);
      case FEILFORE_SAK -> feilregistrerSakstilknytning(avvikshendelseIntern.getJournalpostId());
      case REGISTRER_RETUR -> registrerRetur(journalpost, avvikshendelseIntern);
      default -> throw new AvvikNotSupportedException("Avvik %s ikke støttet".formatted(avvikshendelseIntern.getAvvikstype()));
    }

    hendelserProducer.publishJournalpostUpdated(journalpost.hentJournalpostIdLong());
    SECURE_LOGGER.info("Avvik {} ble utført på journalpost {} av bruker {} og enhet {} med beskrivelse {}", avvikshendelseIntern.getAvvikstype(), avvikshendelseIntern.getJournalpostId(), saksbehandlerInfoManager.hentSaksbehandlerBrukerId(), avvikshendelseIntern.getSaksbehandlersEnhet(), avvikshendelseIntern.getBeskrivelse());

    return Optional.of(new BehandleAvvikshendelseResponse(avvikshendelseIntern.getAvvikstype()));
  }

  public void sendTilFagomrade(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern){
    if (journalpost.isTemaEqualTo(avvikshendelseIntern.getNyttFagomrade())) {
      return;
    }

    if (avvikshendelseIntern.isBidragFagomrade()){
      throw new UgyldigAvvikException("Kan ikke sende journalpost mellom FAR og BID tema.");
    }

    oppgaveService.opprettOverforJournalpostOppgave(journalpost, avvikshendelseIntern.getNyttFagomrade(), avvikshendelseIntern.getBeskrivelse());

  }

  private void knyttTilSakEllerOpphevEksisterendeFeilregistrertSakstilknytning(AvvikshendelseIntern avvikshendelseIntern, Journalpost journalpost){
    var saksnummer = journalpost.getSak().getFagsakId();
    hentFeilregistrerteDupliserteJournalposterMedSakOgTema(saksnummer, avvikshendelseIntern.getNyttFagomrade(), journalpost)
        .findFirst()
        .ifPresentOrElse(
          jp -> opphevFeilregistrerSakstilknytning(jp.getJournalpostId()),
          () -> endreJournalpostService.tilknyttTilSak(saksnummer, avvikshendelseIntern.getNyttFagomrade(), journalpost)
        );

  }

  private Stream<Journalpost> hentFeilregistrerteDupliserteJournalposterMedSakOgTema(String saksnummer, String tema, Journalpost journalpost){
    return journalpostService.finnJournalposterForSaksnummer(saksnummer, tema).stream()
        .filter(Journalpost::isStatusFeilregistrert)
        .filter(jp -> harSammeDokumenter(jp, journalpost));
  }

  private boolean harSammeDokumenter(Journalpost journalpost1, Journalpost journalpost2){
    return journalpost1.getDokumenter().stream().allMatch(dokument1 -> journalpost2.getDokumenter().stream().anyMatch(dokument2-> Objects.equals(dokument2.getDokumentInfoId(), dokument1.getDokumentInfoId())));
  }

  public void endreFagomradeJournalfortJournalpost(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern){
    if (journalpost.isTemaEqualTo(avvikshendelseIntern.getNyttFagomrade())){
      return;
    }

    if (avvikshendelseIntern.isBidragFagomrade()){
      knyttTilSakEllerOpphevEksisterendeFeilregistrertSakstilknytning(avvikshendelseIntern, journalpost);
    } else {
      sendTilFagomrade(journalpost, avvikshendelseIntern);
    }

    feilregistrerSakstilknytning(avvikshendelseIntern.getJournalpostId());
  }

  public void endreFagomrade(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern){
    if (journalpost.isInngaaendeJournalfort()){
      endreFagomradeJournalfortJournalpost(journalpost, avvikshendelseIntern);
    } else {
      oppdater(avvikshendelseIntern.toEndreFagomradeRequest());
//      oppgaveService.oppdaterTemaJournalforingsoppgaver(journalpost.hentJournalpostIdLong(), journalpost.getTema(), avvikshendelseIntern.getNyttFagomrade());
    }
  }

  public void registrerRetur(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern){
    var returDato = LocalDate.parse(avvikshendelseIntern.getReturDato());
    if (journalpost.hasReturDetaljerWithDate(returDato)){
      throw new UgyldigAvvikException("Journalpost har allerede registrert retur med samme dato");
    }
    var beskrivelse = Strings.isNotEmpty(avvikshendelseIntern.getBeskrivelse()) ? avvikshendelseIntern.getBeskrivelse() : "";
    var tilleggsOpplysninger = journalpost.getTilleggsopplysninger();
    tilleggsOpplysninger.addReturDetaljLog(new ReturDetaljerLogDO(beskrivelse, returDato));
    oppdater(new RegistrerReturRequest(journalpost.hentJournalpostIdLong(), returDato, tilleggsOpplysninger));
  }

  public void trekkJournalpost(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern){
    // Journalfør på GENERELL_SAK og kanskje feilfør sakstilknytning
    validateTrue(journalpost.getBruker() != null, new AvvikDetaljException("Kan ikke trekke journalpost uten bruker"));
    validateTrue(journalpost.getTema() != null, new AvvikDetaljException("Kan ikke trekke journalpost uten tilhørende fagområde"));

    knyttTilGenerellSak(avvikshendelseIntern, journalpost);
    leggTilBegrunnelsePaaTittel(avvikshendelseIntern, journalpost);

    dokarkivConsumer.ferdigstill(new FerdigstillJournalpostRequest(avvikshendelseIntern.getJournalpostId(), avvikshendelseIntern.getSaksbehandlersEnhet()));

    if (avvikshendelseIntern.getSkalFeilregistreres()){
      feilregistrerSakstilknytning(avvikshendelseIntern.getJournalpostId());
    }
  }

  public void knyttTilGenerellSak(AvvikshendelseIntern avvikshendelseIntern, Journalpost journalpost){
    oppdater(avvikshendelseIntern.toKnyttTilGenerellSakRequest(journalpost.getTema(), journalpost.getBruker()));
  }

  public void leggTilBegrunnelsePaaTittel(AvvikshendelseIntern avvikshendelseIntern, Journalpost journalpost){
    if (Strings.isNotEmpty(avvikshendelseIntern.getBeskrivelse())){
      validateTrue(avvikshendelseIntern.getBeskrivelse().length() < 100, new AvvikDetaljException("Beskrivelse kan ikke være lengre enn 100 tegn"));
      oppdater(avvikshendelseIntern.toLeggTilBegrunnelsePaaTittelRequest(journalpost.getTittel()));
    }
  }

  public void feilregistrerSakstilknytning(Long journalpostId){
    dokarkivConsumer.feilregistrerSakstilknytning(journalpostId);
  }

  public void opphevFeilregistrerSakstilknytning(String journalpostId){
    dokarkivConsumer.opphevFeilregistrerSakstilknytning(Long.valueOf(journalpostId));
  }

  public void oppdater(OppdaterJournalpostRequest oppdaterJournalpostRequest) {
    dokarkivConsumer.endre(oppdaterJournalpostRequest);
  }

  public Boolean erGyldigAvviksBehandling(Journalpost journalpost, AvvikType avvikType){
    return journalpost.tilAvvik().contains(avvikType);
  }

  public void validateTrue(Boolean expression, RuntimeException throwable){
    if (!expression){
      throw throwable;
    }
  }
}
