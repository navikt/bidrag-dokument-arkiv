package no.nav.bidrag.dokument.arkiv.kafka;

import com.google.common.base.Strings;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.dto.OpprettNyReturLoggRequest;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.JournalpostHarIkkeKommetIRetur;
import no.nav.bidrag.dokument.arkiv.model.OppgaveHendelse;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class BehandleOppgaveHendelseService {

  private static final Logger LOGGER = LoggerFactory.getLogger(BehandleOppgaveHendelseService.class);

  private final DokarkivConsumer dokarkivConsumer;
  private final JournalpostService journalpostService;

  public BehandleOppgaveHendelseService(ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers,
      ResourceByDiscriminator<JournalpostService> journalpostServices) {
    this.dokarkivConsumer = dokarkivConsumers.get(Discriminator.SERVICE_USER);
    this.journalpostService = journalpostServices.get(Discriminator.SERVICE_USER);
  }


  // Returoppgave opprettes før journalpost retur attributter oppdateres. Det kan derfor hende at journalpost ikke er markert at det har kommet i retur og må derfor prøves flere ganger
  @Retryable(value = JournalpostHarIkkeKommetIRetur.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, maxDelay = 12000, multiplier = 2.0))
  public void behandleReturOppgaveOpprettetHendelse(OppgaveHendelse oppgaveHendelse) {
    if (!oppgaveHendelse.erReturOppgave()) {
      LOGGER.warn("Oppgave {} er ikke returoppgave. Avslutter behandling", oppgaveHendelse.getId());
      return;
    }
    if (Strings.isNullOrEmpty(oppgaveHendelse.getJournalpostId())) {
      LOGGER.warn("Returoppgave {} har ingen journalpostid. Avslutter behandling", oppgaveHendelse.getId());
      return;
    }
    LOGGER.info("Legger til ny returlogg på journalpost {}", oppgaveHendelse.getJournalpostId());

    journalpostService.hentJournalpost(Long.valueOf(oppgaveHendelse.getJournalpostId()))
        .ifPresentOrElse((journalpost) -> {
              if (journalpost.manglerReturDetaljForSisteRetur()) {
                dokarkivConsumer.endre(new OpprettNyReturLoggRequest(journalpost));
                LOGGER.info("Lagt til ny returlogg på journalpost {}", journalpost.getJournalpostId());
              } else if (!journalpost.isDistribusjonKommetIRetur()) {
                LOGGER.error("Journalpost {} har ikke kommet i retur", oppgaveHendelse.getJournalpostId());
                throw new JournalpostHarIkkeKommetIRetur(String.format("Journalpost %s har ikke kommet i retur", oppgaveHendelse.getJournalpostId()));
              }
            },
            () -> LOGGER.error("Fant ingen journalpost med id {}", oppgaveHendelse.getJournalpostId())
        );

  }
}
