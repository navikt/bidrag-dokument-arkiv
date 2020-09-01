package no.nav.bidrag.dokument.arkiv.service;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.stream.Collectors;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.GraphQueryConsumer;
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

  private final GraphQueryConsumer graphQueryConsumer;
  private final DokarkivConsumer dokarkivConsumer;

  public JournalpostService(GraphQueryConsumer graphQueryConsumer, DokarkivConsumer dokarkivConsumer) {
    this.graphQueryConsumer = graphQueryConsumer;
    this.dokarkivConsumer = dokarkivConsumer;
  }

  public HttpResponse<Journalpost> hentJournalpost(Integer journalpostId) {
    return graphQueryConsumer.hentJournalpost(journalpostId);
  }

  public HttpResponse<List<JournalpostDto>> finnJournalposter(String saksnummer, String fagomrade) {
    var journalposterResponse = graphQueryConsumer.finnJournalposter(saksnummer, fagomrade);
    var muligeJournalposter = journalposterResponse.fetchBody();

    List<JournalpostDto> journalposter = emptyList();

    if (muligeJournalposter.isPresent()) {
      journalposter = muligeJournalposter.get().stream()
          .map(Journalpost::tilJournalpostDto)
          .collect(Collectors.toList());
    }

    return HttpResponse.from(
        journalposter,
        journalposterResponse.fetchHeaders(),
        journalposterResponse.getResponseEntity().getStatusCode()
    );
  }

  public HttpResponse<Void> endre(Integer journalpostId, EndreJournalpostCommandIntern endreJournalpostCommand) {
    var journalpost = hentJournalpost(journalpostId).fetchBody().orElseThrow(
        () -> new IllegalArgumentException(String.format("Kunne ikke hente journalpost med id %s til å endre!", journalpostId))
    );

    var oppdaterJournalpostRequest = new OppdaterJournalpostRequest(journalpostId, endreJournalpostCommand, journalpost);
    var oppdatertJournalpostResponse = dokarkivConsumer.endre(oppdaterJournalpostRequest);

    oppdatertJournalpostResponse.fetchBody().ifPresent(response -> LOGGER.info("endret: {}", response));

    return HttpResponse.from(
        oppdatertJournalpostResponse.fetchHeaders(),
        oppdatertJournalpostResponse.getResponseEntity().getStatusCode()
    );
  }
}
