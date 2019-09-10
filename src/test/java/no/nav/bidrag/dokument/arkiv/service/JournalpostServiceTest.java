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
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.Dokument;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQueryResponse;
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
    assertThat(httpStatusJournalpostResponse.fetchOptionalResult()).hasValueSatisfying(journalpost -> assertAll(
        () -> {
          assertThat(journalpost).as("journalpost").isNotNull();

          var avsenderMottaker = journalpost.getAvsenderMottaker();
          var bruker = journalpost.getBruker();
          var dokumenter = journalpost.getDokumenter();

          assertAll(
              () -> assertThat(bruker).extracting(Bruker::getId).as("aktor.ident").isEqualTo("1000024690889"),
              () -> assertThat(avsenderMottaker).extracting(AvsenderMottaker::getNavn).as("avsenders navn").isEqualTo("Draugen, Do"),
              () -> assertThat(journalpost.getJournalposttype()).as("journalposttype").isEqualTo("N"),
              () -> assertThat(journalpost.getTema()).as("tema").isEqualTo("BID"),
              () -> assertThat(journalpost.getTittel()).as("tittel").isEqualTo("Filosofens bidrag"),
              () -> assertThat(journalpost.getJournalforendeEnhet()).as("journalførende enhet").isEqualTo("4817"),
              () -> assertThat(journalpost.getJournalfortAvNavn()).as("journalført av navn").isEqualTo("Bånn, Jaims"),
              () -> assertThat(journalpost.hentDatoJournalfort()).as("journalført dato").isEqualTo(LocalDate.of(2010, 3, 8)),
              () -> assertThat(journalpost.getJournalpostId()).as("journalpostId").isEqualTo(String.valueOf(journalpostIdFraJson)),
              () -> assertThat(journalpost.getJournalstatus()).as("journalstatus").isEqualTo("JOURNALFOERT"),
              () -> assertThat(journalpost.hentDatoRegistrert()).as("registrert dato").isEqualTo(LocalDate.of(2010, 3, 12)),
              () -> {
                assertThat(dokumenter).as("dokumenter").hasSize(1);
                assertThat(dokumenter.get(0)).extracting(Dokument::getTittel).as("dokument.tittel").isEqualTo("Å være eller ikke være...");
              }
          );
        }
    ));
  }
}
