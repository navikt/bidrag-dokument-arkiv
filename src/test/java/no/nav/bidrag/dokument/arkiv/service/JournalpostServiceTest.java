package no.nav.bidrag.dokument.arkiv.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.consumer.GraphQueryConsumer;
import no.nav.bidrag.dokument.dto.AktorDto;
import no.nav.bidrag.dokument.dto.DokumentDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
@DisplayName("JournalpostService")
class JournalpostServiceTest {

  private static final String JSON = String.join("\n", "{",
      "  \"data\": {",
      "    \"journalpost\": {",
      "      \"avsenderMottaker\": {",
      "        \"navn\": \"Draugen, Do\"",
      "      },",
      "      \"bruker\": {",
      "        \"id\": \"1000028562627\",",
      "        \"type\": \"AKTOERID\"",
      "      },",
      "      \"dokumenter\": [",
      "        {",
      "          \"tittel\": \"Å være eller ikke være...\"",
      "        }",
      "      ],",
      "      \"journalforendeEnhet\": \"4817\",",
      "      \"journalfortAvNavn\": \"Bånn, Jaims\",",
      "      \"journalpostId\": \"500\",",
      "      \"journalposttype\": \"N\",",
      "      \"journalstatus\": \"FERDIGSTILT\",",
      "      \"relevanteDatoer\": [",
      "        {",
      "          \"dato\": \"2001-05-01T00:00\",",
      "          \"datotype\": \"DATO_DOKUMENT\"",
      "        },",
      "        {",
      "          \"dato\": \"" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")) + "\",",
      "          \"datotype\": \"DATO_JOURNALFOERT\"",
      "        },",
      "        {",
      "          \"dato\": \"" + LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")) + "\",",
      "          \"datotype\": \"DATO_REGISTRERT\"",
      "        }",
      "      ],",
      "      \"sak\": {",
      "        \"fagsakId\": \"1234567\"",
      "      },",
      "      \"tema\": \"BID\",",
      "      \"tittel\": \"Filosofens bidrag\"",
      "    }",
      "  }",
      "}");

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private JournalpostService journalpostService;
  @MockBean
  private GraphQueryConsumer graphQueryConsumerMock;

  @Test
  @DisplayName("skal oversette Map fra consumer til JournalpostDto")
  void skalOversetteMapFraConsumerTilJournalpostDto() throws IOException {

    @SuppressWarnings("unchecked") Optional<Map<String, Object>> journalpostMapOversattMedJackson = Optional.of(
        objectMapper.readValue(JSON, HashMap.class)
    );

    when(graphQueryConsumerMock.hentJournalpost(101)).thenReturn(journalpostMapOversattMedJackson);

    var muligJournalpost = journalpostService.hentJournalpost(101);

    assertThat(muligJournalpost).hasValueSatisfying(journalpostDto -> assertAll(
        () -> {
          assertThat(journalpostDto).as("journalpost").isNotNull();

          var gjelderAktor = journalpostDto.getGjelderAktor();
          var dokumenter = journalpostDto.getDokumenter();

          assertAll(
              () -> assertThat(gjelderAktor).extracting(AktorDto::getIdent).as("aktor.ident").isEqualTo("1000028562627"),
              () -> assertThat(gjelderAktor).extracting(AktorDto::getIdentType).as("aktor.identtype").isEqualTo("AKTOERID"),
              () -> assertThat(journalpostDto.getAvsenderNavn()).as("avsenders navn").isEqualTo("Draugen, Do"),
              () -> assertThat(journalpostDto.getDokumentType()).as("dokumenttype").isEqualTo("N"),
              () -> assertThat(journalpostDto.getFagomrade()).as("fagområde").isEqualTo("BID"),
              () -> assertThat(journalpostDto.getInnhold()).as("innhold").isEqualTo("Filosofens bidrag"),
              () -> assertThat(journalpostDto.getJournalforendeEnhet()).as("journalførende enhet").isEqualTo("4817"),
              () -> assertThat(journalpostDto.getJournalfortAv()).as("journalført av").isEqualTo("Bånn, Jaims"),
              () -> assertThat(journalpostDto.getJournalfortDato()).as("journalført dato").isEqualTo(LocalDate.now()),
              () -> assertThat(journalpostDto.getJournalpostId()).as("journalpostId").isEqualTo("500"),
              () -> assertThat(journalpostDto.getJournalstatus()).as("journalstatus").isEqualTo("FERDIGSTILT"),
              () -> assertThat(journalpostDto.getMottattDato()).as("mottatt dato").isEqualTo(LocalDate.now().plusDays(1)),
              () -> assertThat(journalpostDto.getSaksnummer()).as("saksnummer").isEqualTo("1234567"),
              () -> {
                assertThat(dokumenter).as("dokumenter").hasSize(1);
                assertThat(dokumenter.get(0)).extracting(DokumentDto::getDokumentType).as("dokument.type").isEqualTo("N");
                assertThat(dokumenter.get(0)).extracting(DokumentDto::getTittel).as("dokument.tittel").isEqualTo("Å være eller ikke være...");
              }
          );
        }
    ));
  }
}
