package no.nav.bidrag.dokument.arkiv.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.consumer.GraphQueryConsumer;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQueryResponse;
import no.nav.bidrag.dokument.dto.AktorDto;
import no.nav.bidrag.dokument.dto.DokumentDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
@DisplayName("JournalpostService")
class JournalpostServiceTest {

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private JournalpostService journalpostService;
  @MockBean
  private GraphQueryConsumer graphQueryConsumerMock;
  @Value("classpath:json/dokumentoversiktFagsakQueryResponse.json")
  private Resource responseJsonResource;

  @Test
  @DisplayName("skal oversette Map fra consumer til JournalpostDto")
  void skalOversetteMapFraConsumerTilJournalpostDto() throws IOException {
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakQueryResponse = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakQueryResponse.class);
    var journalpostIdFraJson = 201028011;
    var saksnummerFraJson = "5276661";

    when(graphQueryConsumerMock.hentJournalpost(journalpostIdFraJson)).thenReturn(Optional.of(
        dokumentoversiktFagsakQueryResponse.hentJournalpost(journalpostIdFraJson)
    ));

    var httpStatusJournalpostResponse = journalpostService.hentJournalpost(saksnummerFraJson, journalpostIdFraJson);

    assertThat(httpStatusJournalpostResponse.getHttpStatus()).as("httpStatus").isEqualTo(HttpStatus.OK);
    assertThat(httpStatusJournalpostResponse.fetchOptionalResult()).hasValueSatisfying(journalpostDto -> assertAll(
        () -> {
          assertThat(journalpostDto).as("journalpost").isNotNull();

          var gjelderAktor = journalpostDto.getGjelderAktor();
          var dokumenter = journalpostDto.getDokumenter();

          assertAll(
              () -> assertThat(gjelderAktor).extracting(AktorDto::getIdent).as("aktor.ident").isEqualTo("1000024690889"),
              () -> assertThat(journalpostDto.getAvsenderNavn()).as("avsenders navn").isEqualTo("Draugen, Do"),
              () -> assertThat(journalpostDto.getDokumentType()).as("dokumenttype").isEqualTo("N"),
              () -> assertThat(journalpostDto.getFagomrade()).as("fagområde").isEqualTo("BID"),
              () -> assertThat(journalpostDto.getInnhold()).as("innhold").isEqualTo("Filosofens bidrag"),
              () -> assertThat(journalpostDto.getJournalforendeEnhet()).as("journalførende enhet").isEqualTo("4817"),
              () -> assertThat(journalpostDto.getJournalfortAv()).as("journalført av").isEqualTo("Bånn, Jaims"),
              () -> assertThat(journalpostDto.getJournalfortDato()).as("journalført dato").isEqualTo(LocalDate.of(2010, 3, 8)),
              () -> assertThat(journalpostDto.getJournalpostId()).as("journalpostId").isEqualTo("JOARK-" + journalpostIdFraJson),
              () -> assertThat(journalpostDto.getJournalstatus()).as("journalstatus").isEqualTo("JOURNALFOERT"),
              () -> assertThat(journalpostDto.getMottattDato()).as("mottatt dato").isEqualTo(LocalDate.of(2010, 3, 12)),
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
