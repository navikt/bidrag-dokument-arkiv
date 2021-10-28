package no.nav.bidrag.dokument.arkiv.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@DisplayName("DokumentoversiktFagsakQueryResponse")
class DokumentoversiktFagsakQueryResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("skal mappe query resultat til kotlin data klasse")
  void skalMappeQueryResultatTilKotlinDataKlasse() throws IOException {
    Resource responseJsonResource = new ClassPathResource("__files/json/dokumentoversiktFagsakQueryResponse.json");
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakQueryResponse = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakQueryResponse.class);

    assertThat(dokumentoversiktFagsakQueryResponse.getData()).as("data").isNotNull();
    assertThat(dokumentoversiktFagsakQueryResponse.getData().getDokumentoversiktFagsak()).as("dokumentoversiktFagsak").isNotNull();
    List<Journalpost> journalposter = dokumentoversiktFagsakQueryResponse.getData().getDokumentoversiktFagsak().getJournalposter();

    assertThat(journalposter).as("journalposter").hasSize(3);
  }
}