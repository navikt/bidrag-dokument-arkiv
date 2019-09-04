package no.nav.bidrag.dokument.arkiv.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import javax.websocket.HandshakeResponse;
import no.nav.bidrag.commons.web.HttpStatusResponse;
import no.nav.bidrag.dokument.arkiv.consumer.GraphQueryConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JournalpostService {

  private final GraphQueryConsumer graphQueryConsumer;

  public JournalpostService(GraphQueryConsumer graphQueryConsumer) {
    this.graphQueryConsumer = graphQueryConsumer;
  }

  public HttpStatusResponse<JournalpostDto> hentJournalpost(String saksnummer, Integer journalpostId) {
    var muligJournalpost = graphQueryConsumer.hentJournalpost(journalpostId);

    if (muligJournalpost.isEmpty()) {
      return new HttpStatusResponse<>(HttpStatus.NO_CONTENT);
    }

    return muligJournalpost
        .filter(journalpost -> journalpost.erTilknyttetSak(saksnummer))
        .map(Journalpost::tilJournalpostDto)
        .map(journalpostDto -> new HttpStatusResponse<>(HttpStatus.OK, journalpostDto))
        .orElseGet(() -> new HttpStatusResponse<>(HttpStatus.NOT_FOUND));
  }

  public List<JournalpostDto> finnJournalposter(String saksnummer, String fagomrade) {
    return graphQueryConsumer.finnJournalposter(saksnummer, fagomrade).stream()
        .map(Journalpost::tilJournalpostDto)
        .collect(toList());
  }
}
