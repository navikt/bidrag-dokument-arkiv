package no.nav.bidrag.dokument.arkiv.service;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.consumer.GraphQueryConsumer;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.springframework.stereotype.Service;

@Service
public class JournalpostService {

  private final GraphQueryConsumer graphQueryConsumer;
  private final JournalpostMapper journalpostMapper;

  public JournalpostService(GraphQueryConsumer graphQueryConsumer, JournalpostMapper journalpostMapper) {
    this.graphQueryConsumer = graphQueryConsumer;
    this.journalpostMapper = journalpostMapper;
  }

  public Optional<JournalpostDto> hentJournalpost(Integer journalpostId) {
    return graphQueryConsumer.hentJournalpost(journalpostId)
        .map(this::tilJournalpostMap)
        .map(journalpostMapper::tilJournalpostDto);
  }

  private Map tilJournalpostMap(Map mapFraConsumer) {
    return (Map) Optional.ofNullable(mapFraConsumer.get("data"))
        .map(obj -> obj instanceof Map ? ((Map) obj).get("journalpost") : null)
        .orElse(null);
  }

  public List<JournalpostDto> finnJournalposter(String saksnummer, String fagomrade) {
    return graphQueryConsumer.finnJournalposter(saksnummer, fagomrade).stream()
        .map(this::tilJournalpostMapForListeAvJournalposter)
        .flatMap(List::stream)
        .map(journalpostMapper::tilJournalpostDto)
        .collect(toList());
  }

  @SuppressWarnings("unchecked")
  private List<Map> tilJournalpostMapForListeAvJournalposter(Map mapFraConsumer) {
    return (List<Map>) Optional.ofNullable(mapFraConsumer.get("data"))
        .map(obj -> obj instanceof Map ? ((Map) obj).get("journalposter") : null)
        .orElse(emptyList());
  }
}
