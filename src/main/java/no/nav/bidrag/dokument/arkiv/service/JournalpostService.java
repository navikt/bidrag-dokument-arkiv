package no.nav.bidrag.dokument.arkiv.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.consumer.GraphQueryConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.springframework.stereotype.Service;

@Service
public class JournalpostService {

  private final GraphQueryConsumer graphQueryConsumer;

  public JournalpostService(GraphQueryConsumer graphQueryConsumer) {
    this.graphQueryConsumer = graphQueryConsumer;
  }

  public Optional<JournalpostDto> hentJournalpost(Integer journalpostId) {
    return graphQueryConsumer.hentJournalpost(journalpostId)
        .map(Journalpost::tilJournalpostDto);
  }

  public List<JournalpostDto> finnJournalposter(String saksnummer, String fagomrade) {
    return graphQueryConsumer.finnJournalposter(saksnummer, fagomrade).stream()
        .map(Journalpost::tilJournalpostDto)
        .collect(toList());
  }
}
