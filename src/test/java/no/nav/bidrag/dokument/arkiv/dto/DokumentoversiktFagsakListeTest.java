package no.nav.bidrag.dokument.arkiv.dto;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
@DisplayName("DokumentoversiktFagsakListe")
class DokumentoversiktFagsakListeTest {

  @Value("classpath:json/journalposter.json")
  private Resource journalposterJsonResource;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("skal mappe json til kotlin data klasse")
  void skalMappeJsonTilKotlinDataKlasse() throws IOException {
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(journalposterJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakListe = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakListe.class);

    assertThat(dokumentoversiktFagsakListe.getJournalposter()).hasSize(3);
  }
}