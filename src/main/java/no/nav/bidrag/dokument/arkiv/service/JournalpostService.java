package no.nav.bidrag.dokument.arkiv.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import no.nav.bidrag.commons.web.HttpStatusResponse;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.GraphQueryConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

  public HttpStatusResponse<Journalpost> hentJournalpost(String saksnummer, Integer journalpostId) {
    var muligJournalpost = graphQueryConsumer.hentJournalpost(journalpostId);

    if (muligJournalpost.isEmpty()) {
      return new HttpStatusResponse<>(HttpStatus.NO_CONTENT);
    }

    return muligJournalpost
        .filter(journalpost -> journalpost.erTilknyttetSak(saksnummer))
        .map(journalpostDto -> new HttpStatusResponse<>(HttpStatus.OK, journalpostDto))
        .orElseGet(() -> new HttpStatusResponse<>(HttpStatus.NOT_FOUND));
  }

  public List<JournalpostDto> finnJournalposter(String saksnummer, String fagomrade) {
    return graphQueryConsumer.finnJournalposter(saksnummer, fagomrade).stream()
        .map(Journalpost::tilJournalpostDto)
        .collect(toList());
  }

  public HttpStatusResponse<Void> endre(
      String saksnummer,
      Integer journalpostId,
      EndreJournalpostCommand endreJournalpostCommand
  ) {
    var journalpost = hentJournalpost(saksnummer, journalpostId).fetchOptionalResult()
        .orElseThrow(() -> new IllegalArgumentException(String.format("Kunne ikke hente journalpost med id %s til å endre!", journalpostId)));

    var oppdaterJournalpostRequest = new OppdaterJournalpostRequest(journalpostId, endreJournalpostCommand, journalpost);
    var oppdatertJournalpostResponse = dokarkivConsumer.endre(oppdaterJournalpostRequest);

    oppdatertJournalpostResponse.fetchOptionalResult().ifPresent(response -> {
      response.setSaksnummer(saksnummer);
      LOGGER.info("endret: {}", response);
    });

    return new HttpStatusResponse<>(oppdatertJournalpostResponse.getHttpStatus());
  }
}
