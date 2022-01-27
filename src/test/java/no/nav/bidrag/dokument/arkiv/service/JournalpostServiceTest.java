package no.nav.bidrag.dokument.arkiv.service;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Objects;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.Dokument;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQueryResponse;
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
@DisplayName("JournalpostService")
class JournalpostServiceTest {

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private ResourceByDiscriminator<JournalpostService> journalpostService;
  @MockBean
  private SafConsumer safConsumerMock;
  @MockBean
  private PersonConsumer personConsumerMock;
  @Value("classpath:__files/json/dokumentoversiktFagsakQueryResponse.json")
  private Resource responseJsonResource;

  @Test
  @DisplayName("skal oversette Map fra consumer til JournalpostDto")
  void skalOversetteMapFraConsumerTilJournalpostDto() throws IOException {
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakQueryResponse = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakQueryResponse.class);
    Long journalpostIdFraJson = 201028011L;

    var journalpostDokOversikt = dokumentoversiktFagsakQueryResponse.hentJournalpost(journalpostIdFraJson);

    when(safConsumerMock.hentJournalpost(journalpostIdFraJson)).thenReturn(journalpostDokOversikt);
    when(personConsumerMock.hentPerson(journalpostDokOversikt.getBruker().getId())).thenReturn(HttpResponse.from(
        HttpStatus.OK, new PersonResponse("123123", "555555")));

    var muligJournalpost = journalpostService.get(Discriminator.REGULAR_USER).hentJournalpostMedFnrOgTilknyttedeSaker(journalpostIdFraJson, null);

    assertAll(
        () -> {
          assertThat(muligJournalpost).as("journalpost").isPresent();

          var journalpost = muligJournalpost.get();
          var avsenderMottaker = journalpost.getAvsenderMottaker();
          var bruker = journalpost.getBruker();
          var dokumenter = journalpost.getDokumenter();

          assertAll(
              () -> assertThat(bruker).extracting(Bruker::getId).as("aktor.ident").isEqualTo("123123"),
              () -> assertThat(bruker).extracting(Bruker::getType).as("aktor.tyoe").isEqualTo("FNR"),
              () -> assertThat(avsenderMottaker).extracting(AvsenderMottaker::getNavn).as("avsenders navn").isEqualTo("Draugen, Do"),
              () -> assertThat(journalpost.getJournalposttype()).as("journalposttype").isEqualTo("N"),
              () -> assertThat(journalpost.getTema()).as("tema").isEqualTo("BID"),
              () -> assertThat(journalpost.getTittel()).as("tittel").isEqualTo("Filosofens bidrag"),
              () -> assertThat(journalpost.getJournalforendeEnhet()).as("journalførende enhet").isEqualTo("4817"),
              () -> assertThat(journalpost.getJournalfortAvNavn()).as("journalført av navn").isEqualTo("Bånn, Jaims"),
              () -> assertThat(journalpost.hentDatoJournalfort()).as("journalført dato").isEqualTo(LocalDate.of(2010, 3, 8)),
              () -> assertThat(journalpost.getJournalpostId()).as("journalpostId").isEqualTo(String.valueOf(journalpostIdFraJson)),
              () -> assertThat(journalpost.getJournalstatus()).as("journalstatus").isEqualTo(JournalStatus.JOURNALFOERT),
              () -> assertThat(journalpost.hentDatoRegistrert()).as("registrert dato").isEqualTo(LocalDate.of(2010, 3, 12)),
              () -> {
                assertThat(dokumenter).as("dokumenter").hasSize(1);
                assertThat(dokumenter.get(0)).extracting(Dokument::getTittel).as("dokument.tittel").isEqualTo("Å være eller ikke være...");
              }
          );
        });
  }
}
