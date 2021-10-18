package no.nav.bidrag.dokument.arkiv.service;

import java.util.List;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.dto.AvvikshendelseIntern;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer;
import no.nav.bidrag.dokument.arkiv.model.AvvikNotSupportedException;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.model.UgyldigAvvikException;
import no.nav.bidrag.dokument.dto.AvvikType;
import no.nav.bidrag.dokument.dto.BehandleAvvikshendelseResponse;
import no.nav.bidrag.dokument.dto.JournalpostIkkeFunnetException;
import org.springframework.stereotype.Service;

@Service
public class AvvikService {

  public final JournalpostService journalpostService;
  public final HendelserProducer hendelserProducer;

  public AvvikService(ResourceByDiscriminator<JournalpostService> journalpostService, HendelserProducer hendelserProducer) {
    this.journalpostService = journalpostService.get(Discriminator.REGULAR_USER);
    this.hendelserProducer = hendelserProducer;
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
      case OVERFOR_TIL_ANNEN_ENHET -> journalpostService.oppdater(avvikshendelseIntern.toOverforEnhetRequest());
      case ENDRE_FAGOMRADE -> journalpostService.oppdater(avvikshendelseIntern.toEndreFagomradeRequest());
      case TREKK_JOURNALPOST -> journalpostService.trekkJournalpost(avvikshendelseIntern.getJournalpostId());
      case FEILFORE_SAK -> journalpostService.feilforSak(avvikshendelseIntern.getJournalpostId());
      default -> throw new AvvikNotSupportedException("Avvik %s ikke støttet".formatted(avvikshendelseIntern.getAvvikstype()));
    }

    hendelserProducer.publishJournalpostUpdated(journalpost.hentJournalpostIdLong());

    return Optional.of(new BehandleAvvikshendelseResponse(avvikshendelseIntern.getAvvikstype()));
  }

  public Boolean erGyldigAvviksBehandling(Journalpost journalpost, AvvikType avvikType){
    return journalpost.tilAvvik().contains(avvikType);
  }
}
