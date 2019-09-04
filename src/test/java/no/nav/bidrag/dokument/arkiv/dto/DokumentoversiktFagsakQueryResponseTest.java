package no.nav.bidrag.dokument.arkiv.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
@DisplayName("DokumentoversiktFagsakQueryResponse")
class DokumentoversiktFagsakQueryResponseTest {

  @Value("classpath:json/dokumentoversiktFagsakQueryResponse.json")
  private Resource responseJsonResource;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("skal mappe query resultat til kotlin data klasse")
  void skalMappeQueryResultatTilKotlinDataKlasse() throws IOException {
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakQueryResponse = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakQueryResponse.class);

    assertThat(dokumentoversiktFagsakQueryResponse.getData()).as("data").isNotNull();
    assertThat(dokumentoversiktFagsakQueryResponse.getData().getDokumentoversiktFagsak()).as("dokumentoversiktFagsak").isNotNull();
    List<Journalpost> journalposter = dokumentoversiktFagsakQueryResponse.getData().getDokumentoversiktFagsak().getJournalposter();

    assertThat(journalposter).as("journalposter").hasSize(3);
  }
}