package no.nav.bidrag.dokument.arkiv.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@DisplayName("DokumentoversiktFagsakListe")
class DokumentoversiktFagsakListeTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("skal mappe json til kotlin data klasse")
  void skalMappeJsonTilKotlinDataKlasse() throws IOException {
    Resource journalposterJsonResource = new ClassPathResource("__files/json/journalposter.json");
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(journalposterJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakListe = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakListe.class);

    assertThat(dokumentoversiktFagsakListe.getJournalposter()).hasSize(3);
  }
}