package no.nav.bidrag.dokument.arkiv.service;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
import no.nav.bidrag.dokument.arkiv.model.FeilforSakFeiletException;
import no.nav.bidrag.dokument.arkiv.model.OppdaterJournalpostFeiletException;
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
  public final OppgaveService oppgaveService;
  private final DokarkivConsumer dokarkivConsumer;
  private final SaksbehandlerInfoManager saksbehandlerInfoManager;

  public AvvikService(ResourceByDiscriminator<JournalpostService> journalpostService, HendelserProducer hendelserProducer, OppgaveService oppgaveService, ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers, SaksbehandlerInfoManager saksbehandlerInfoManager) {
    this.journalpostService = journalpostService.get(Discriminator.REGULAR_USER);
    this.hendelserProducer = hendelserProducer;
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
      case TREKK_JOURNALPOST -> trekkJournalpost(journalpost, avvikshendelseIntern);
      case FEILFORE_SAK -> feilregistrerSakstilknytning(avvikshendelseIntern);
      case REGISTRER_RETUR -> registrerRetur(journalpost, avvikshendelseIntern);
      default -> throw new AvvikNotSupportedException("Avvik %s ikke støttet".formatted(avvikshendelseIntern.getAvvikstype()));
    }

    hendelserProducer.publishJournalpostUpdated(journalpost.hentJournalpostIdLong());
    SECURE_LOGGER.info("Avvik {} ble utført på journalpost {} av bruker {} og enhet {} med beskrivelse {}", avvikshendelseIntern.getAvvikstype(), avvikshendelseIntern.getJournalpostId(), saksbehandlerInfoManager.hentSaksbehandlerBrukerId(), avvikshendelseIntern.getSaksbehandlersEnhet(), avvikshendelseIntern.getBeskrivelse());

    return Optional.of(new BehandleAvvikshendelseResponse(avvikshendelseIntern.getAvvikstype()));
  }

  public void kopierTilAnnenFagomrade(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern){
    oppgaveService.opprettOverforJournalpostOppgave(journalpost, avvikshendelseIntern.getNyttFagomrade(), avvikshendelseIntern.getBeskrivelse());
  }

  public void endreFagomrade(Journalpost journalpost, AvvikshendelseIntern avvikshendelseIntern){
    if (journalpost.isInngaaendeDokument() && journalpost.isStatusJournalfort()){
      oppgaveService.opprettOverforJournalpostOppgave(journalpost, avvikshendelseIntern.getNyttFagomrade(), avvikshendelseIntern.getBeskrivelse());
      feilregistrerSakstilknytning(avvikshendelseIntern);
    } else {
      oppdater(avvikshendelseIntern.toEndreFagomradeRequest());
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
      feilregistrerSakstilknytning(avvikshendelseIntern);
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

  public void feilregistrerSakstilknytning(AvvikshendelseIntern avvikshendelseIntern){
    var httpResponse = dokarkivConsumer.feilregistrerSakstilknytning(avvikshendelseIntern.getJournalpostId());
    if (!httpResponse.is2xxSuccessful()){
      throw new FeilforSakFeiletException(String.format("Feilregistrer sakstilknytning feilet for journalpostId %s", avvikshendelseIntern.getJournalpostId()));
    }
  }

  public void oppdater(OppdaterJournalpostRequest oppdaterJournalpostRequest) {
    var oppdatertJournalpostResponse = dokarkivConsumer.endre(oppdaterJournalpostRequest);
    if (!oppdatertJournalpostResponse.is2xxSuccessful()){
      throw new OppdaterJournalpostFeiletException(String.format("Oppdater journalpost feilet for journapostId %s", oppdaterJournalpostRequest.hentJournalpostId()));
    }

    oppdatertJournalpostResponse.fetchBody().ifPresent(response -> LOGGER.info("endret: {}", response));
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
