package no.nav.bidrag.dokument.arkiv.kafka;

import com.google.common.base.Strings;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.OpprettNyReturLoggRequest;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.OppgaveHendelse;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.service.EndreJournalpostService;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BehandleOppgaveHendelseService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BehandleOppgaveHendelseService.class);

  private final DokarkivConsumer dokarkivConsumer;
  private final JournalpostService journalpostService;

  public BehandleOppgaveHendelseService(ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers, ResourceByDiscriminator<JournalpostService> journalpostServices) {
    this.dokarkivConsumer = dokarkivConsumers.get(Discriminator.SERVICE_USER);
    this.journalpostService = journalpostServices.get(Discriminator.SERVICE_USER);
  }

  public void behandleReturOppgaveOpprettetHendelse(OppgaveHendelse oppgaveHendelse){
    if (!oppgaveHendelse.erReturOppgave()){
      LOGGER.warn("Oppgave {} er ikke returoppgave. Avslutter behandling", oppgaveHendelse.getId());
      return;
    }
    if (Strings.isNullOrEmpty(oppgaveHendelse.getJournalpostId())){
      LOGGER.warn("Returoppgave {} har ingen journalpostid. Avslutter behandling", oppgaveHendelse.getId());
      return;
    }
    LOGGER.info("Legger til ny returlogg på journalpost {}", oppgaveHendelse.getJournalpostId());
    Journalpost journalpost = journalpostService.hentJournalpost(Long.valueOf(oppgaveHendelse.getJournalpostId())).orElseThrow(()->new JournalpostIkkeFunnetException(String.format("Fant ikke journalpost %s", oppgaveHendelse.getJournalpostId())));
    if (journalpost.manglerReturDetaljForSisteRetur()) {
      dokarkivConsumer.endre(new OpprettNyReturLoggRequest(journalpost));
      LOGGER.info("Lagt til ny returlogg på journalpost {}", journalpost.getJournalpostId());
    } else {
      LOGGER.info("Kunne ikke legg til ny returlogg på journalpost {}. Dette skyldes at journalpost ikke har kommet i retur eller ikke har blitt distribuert.", oppgaveHendelse.getJournalpostId());
    }

  }
}
