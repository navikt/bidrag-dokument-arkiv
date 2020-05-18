package no.nav.bidrag.dokument.arkiv;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@DisplayName("Mapping av json verdier")
@PropertySource("classpath:resources.properties")
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
class JsonMapperTest {

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("skal mappe json streng til java.util.Map")
  void skalMappeJsonRequest() throws JsonProcessingException {
    var opprettJournalpostRequestAsJson = String.join("\n", "{",
        "\"avsenderMottaker\": { \"navn\": \"Birger\" },",
        "\"behandlingstema\": \"BI01\",",
        "\"bruker\": { \"id\": \"06127412345\", \"idType\": \"FNR\" },",
        "\"dokumenter\": [{ \"brevkode\": \"BREVKODEN\", \"dokumentKategori\": \"dokumentKategori\", \"tittel\": \"Tittelen på dokumentet\" }],",
        "\"eksternReferanseId\": \"dokreferanse\",",
        "\"journalfoerendeEnhet\": \"666\",",
        "\"journalpostType\": \"N\",",
        "\"kanal\": \"nav.no\",",
        "\"sak\": { \"arkivsaksnummer\": \"1900001\", \"arkivsaksystem\": \"GSAK\" },",
        "\"tema\": \"BID\",",
        "\"tittel\": \"Tittelen på journalposten\"",
        "}"
    );

    var jsonMap = objectMapper.readValue(opprettJournalpostRequestAsJson, Map.class);

    assertAll(
        () -> assertThat(jsonMap.get("avsenderMottaker")).as("avsenderMottaker").isNotNull(),
        () -> assertThat(jsonMap.get("behandlingstema")).as("behandlingstema").isEqualTo("BI01"),
        () -> assertThat(jsonMap.get("bruker")).as("bruker").isNotNull(),
        () -> assertThat(jsonMap.get("dokumenter")).as("dokumenter").isNotNull(),
        () -> assertThat(jsonMap.get("eksternReferanseId")).as("eksternReferanseId").isEqualTo("dokreferanse"),
        () -> assertThat(jsonMap.get("journalfoerendeEnhet")).as("journalfoerendeEnhet").isEqualTo("666"),
        () -> assertThat(jsonMap.get("journalpostType")).as("journalpostType").isEqualTo("N"),
        () -> assertThat(jsonMap.get("kanal")).as("kanal").isEqualTo("nav.no"),
        () -> assertThat(jsonMap.get("sak")).as("sak").isNotNull(),
        () -> assertThat(jsonMap.get("tema")).as("tema").isEqualTo("BID"),
        () -> assertThat(jsonMap.get("tittel")).as("tittel").isEqualTo("Tittelen på journalposten")
    );
  }

  @Test
  @DisplayName("skal mappe saf query til java.util.Map")
  void skalMappeSafQueryTilMap() throws JsonProcessingException {
    var query = new DokumentoversiktFagsakQuery("666", "BID").writeQuery();
    var jsonMap = objectMapper.readValue(query, Map.class);

    //noinspection unchecked
    assertAll(
        () -> assertThat(jsonMap).hasSize(1),
        () -> {
          String safQuery = (String) jsonMap.get("query");
          assertThat(safQuery).as("query property").isNotNull();
          assertThat(safQuery).as("querystring")
              .contains("fagsakId: \"666\"")
              .contains("tema:BID");
        }
    );
  }
}
