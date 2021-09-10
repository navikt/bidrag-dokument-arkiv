package no.nav.bidrag.dokument.arkiv.service;

import java.util.List;
import java.util.stream.Collectors;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
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

  public Journalpost hentJournalpost(Integer journalpostId) {
    return safConsumer.hentJournalpost(journalpostId);
  }

  public List<JournalpostDto> finnJournalposter(String saksnummer, String fagomrade) {
    var journalposterResponse = safConsumer.finnJournalposter(saksnummer, fagomrade);
    return journalposterResponse.stream()
        .map(Journalpost::tilJournalpostDto)
        .collect(Collectors.toList());

  }

  public HttpResponse<Void> endre(Integer journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand) {
    var journalpost = hentJournalpost(journalpostId);

    var oppdaterJournalpostRequest = new OppdaterJournalpostRequest(journalpostId.toString(), endreJournalpostCommand, journalpost);
    var oppdatertJournalpostResponse = dokarkivConsumer.endre(oppdaterJournalpostRequest);

    oppdatertJournalpostResponse.fetchBody().ifPresent(response -> LOGGER.info("endret: {}", response));

    return HttpResponse.from(
        oppdatertJournalpostResponse.fetchHeaders(),
        oppdatertJournalpostResponse.getResponseEntity().getStatusCode()
    );
  }
}
