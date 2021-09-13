package no.nav.bidrag.dokument.arkiv;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQuery;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.dto.EndreDokument;
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand;
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
  @DisplayName("skal mappe OppdaterJournalpost til json")
  void skalMappeOppdaterJournalpostTilJson() throws IOException {
    var journalpost = new Journalpost();
    var endreDokument = new EndreDokument();
    endreDokument.setTittel("Tittelen på dokument");
    endreDokument.setDokId(55555);

    var endreJournalpostCommand = new EndreJournalpostCommand();
    endreJournalpostCommand.setAvsenderNavn("AvsenderNavn");
    endreJournalpostCommand.setEndreDokumenter(Arrays.asList(endreDokument));
    endreJournalpostCommand.setFagomrade("BID");
    endreJournalpostCommand.setGjelder("1234");
    endreJournalpostCommand.setTittel("Tittelen på journalposten");
    endreJournalpostCommand.setGjelderType("FNR");
    endreJournalpostCommand.setTilknyttSaker(Arrays.asList("sakIdent"));

    var endreJournalpostIntern = new EndreJournalpostCommandIntern(endreJournalpostCommand, "4805");
    var oppdaterJp = new OppdaterJournalpostRequest(12345, endreJournalpostIntern, journalpost);

    var jsonMap = objectMapper.convertValue(oppdaterJp, Map.class);

    assertAll(
        () -> assertThat(((Map<String, String>)jsonMap.get("avsenderMottaker")).get("navn")).as("avsenderMottaker").isEqualTo("AvsenderNavn"),
        () -> assertThat(((Map<String, String>)jsonMap.get("bruker")).get("id")).as("id").isEqualTo("1234"),
        () -> assertThat(((Map<String, String>)jsonMap.get("bruker")).get("idType")).as("idType").isEqualTo("FNR"),
        () -> assertThat(((Map<String, String>)jsonMap.get("sak")).get("fagsakId")).as("fagsakId").isEqualTo("sakIdent"),
        () -> assertThat(((Map<String, String>)jsonMap.get("sak")).get("fagsaksystem")).as("fagsaksystem").isEqualTo("BISYS"),
        () -> assertThat(((Map<String, String>)jsonMap.get("sak")).get("sakstype")).as("fagsaksystem").isEqualTo("FAGSAK"),
        () -> assertThat(((List<Map<String, String>>)jsonMap.get("dokumenter")).get(0).get("dokumentInfoId")).as("dokumentInfoId").isEqualTo("55555"),
        () -> assertThat(((List<Map<String, String>>)jsonMap.get("dokumenter")).get(0).get("tittel")).as("tittel").isEqualTo("Tittelen på dokument"),
        () -> assertThat(jsonMap.get("tema")).as("tema").isEqualTo("BID"),
        () -> assertThat(jsonMap.get("tittel")).as("tittel").isEqualTo("Tittelen på journalposten")
    );
  }

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
    var safQuery = new DokumentoversiktFagsakQuery("666", "BID").getQuery();

    //noinspection unchecked
    assertAll(
        () -> assertThat(safQuery).as("querystring")
            .contains("fagsakId: \"666\"")
            .contains("tema:BID")
    );
  }
}
