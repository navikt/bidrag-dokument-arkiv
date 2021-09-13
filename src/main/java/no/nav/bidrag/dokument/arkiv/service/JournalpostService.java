package no.nav.bidrag.dokument.arkiv.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JournalpostService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JournalpostService.class);

  private final SafConsumer safConsumer;
  private final DokarkivConsumer dokarkivConsumer;

  public JournalpostService(SafConsumer safConsumer, DokarkivConsumer dokarkivConsumer) {
    this.safConsumer = safConsumer;
    this.dokarkivConsumer = dokarkivConsumer;
  }

  public Optional<Journalpost> hentJournalpost(Integer journalpostId) {
    return hentJournalpost(journalpostId, null);
  }

  public Optional<Journalpost> hentJournalpost(Integer journalpostId, String saksnummer) {
    var journalpost = safConsumer.hentJournalpost(journalpostId);

    if (journalpost.erIkkeTilknyttetSakNarOppgitt(saksnummer)) {
      return Optional.empty();
    }

    return Optional.of(journalpost);
  }

  public List<JournalpostDto> finnJournalposter(String saksnummer, String fagomrade) {
    var journalposterResponse = safConsumer.finnJournalposter(saksnummer, fagomrade);
    return journalposterResponse.stream()
        .map(Journalpost::tilJournalpostDto)
        .collect(Collectors.toList());

  }

  public HttpResponse<Void> endre(Integer journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand) {
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
