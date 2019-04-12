package no.nav.bidrag.dokument.arkiv.service;

import java.util.Collections;
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

  public List<JournalpostDto> finnJournalposter(Integer saksnummer, String fagomrade) {
    return Collections.emptyList();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> tilJournalpostMap(Map<String, Object> mapFromConsumer) {
    return (Map<String, Object>) Optional.ofNullable(mapFromConsumer.get("data"))
        .map(obj -> obj instanceof Map ? ((Map<String, Object>) obj).get("journalpost") : null)
        .orElse(null);
  }

}
