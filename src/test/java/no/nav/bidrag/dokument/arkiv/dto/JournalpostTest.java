package no.nav.bidrag.dokument.arkiv.dto;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.dto.AvvikType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
@DisplayName("Journalpost")
class JournalpostTest {

  @Value("classpath:json/journalpost.json")
  private Resource journalpostJsonResource;

  @Autowired
  private ObjectMapper objectMapper;

  private String journalpostJsonText;

  @BeforeEach
  void lesJsonResourceTilStreng() throws IOException {
    journalpostJsonText = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(journalpostJsonResource.getFile().toURI()))));
  }

  @Test
  @DisplayName("skal mappe en journalpost fra json")
  void skalMappeJournalpostFraJson() throws IOException {
    var journalpost = objectMapper.readValue(journalpostJsonText, Journalpost.class);

    assertAll(
        () -> assertThat(journalpost.getAvsenderMottaker()).as("avsenderMottaker").isEqualTo(new AvsenderMottaker("Tuborg")),
        () -> assertThat(journalpost.getBruker()).as("bruker").isEqualTo(new Bruker("1000024690889", "AKTOERID")),
        () -> assertThat(journalpost.getDokumenter()).as("dokumenter").isEqualTo(List.of(
            new Dokument("ROD SNO", "12345"),
            new Dokument("SNOMANNEN", "56789")
        )),
        () -> assertThat(journalpost.getJournalforendeEnhet()).as("journalforendeEnhet").isEqualTo("0104"),
        () -> assertThat(journalpost.getJournalfortAvNavn()).as("journalfortAvNavn").isEqualTo("Terkelsen, Karin"),
        () -> assertThat(journalpost.getJournalpostId()).as("journalpostId").isEqualTo("203915975"),
        () -> assertThat(journalpost.getJournalposttype()).as("journalposttype").isEqualTo("I"),
        () -> assertThat(journalpost.getJournalstatus()).as("journalstatus").isEqualTo(JournalStatus.JOURNALFOERT),
        () -> assertThat(journalpost.getRelevanteDatoer()).as("relevanteDater").isEqualTo(List.of(
            new DatoType("2010-12-16T00:00", "DATO_JOURNALFOERT"),
            new DatoType("2010-12-15T00:00", "DATO_REGISTRERT")
        )),
        () -> assertThat(journalpost.getTema()).as("tema").isEqualTo("AAP"),
        () -> assertThat(journalpost.getTittel()).as("tittel").isEqualTo("...and so on...")
    );
  }

  @Test
  @DisplayName("skal hente journalført dato")
  void skalHenteJournalfortDato() throws IOException {
    var journalpost = objectMapper.readValue(journalpostJsonText, Journalpost.class);

    assertThat(journalpost.hentDatoJournalfort()).isEqualTo(LocalDate.of(2010, 12, 16));
  }

  @Test
  @DisplayName("skal hente registrert dato")
  void skalHenteRegistrertDato() throws IOException {
    var journalpost = objectMapper.readValue(journalpostJsonText, Journalpost.class);

    assertThat(journalpost.hentDatoRegistrert()).isEqualTo(LocalDate.of(2010, 12, 15));
  }

  @Test
  @DisplayName("skal hente avvik hvis Journalpost med status mottatt og inngående")
  void skalHenteAvvikForMottattOgInngaaende() {
    var journalpost = new Journalpost();
    journalpost.setJournalstatus(JournalStatus.MOTTATT);
    journalpost.setJournalposttype("I");
    var avvikListe = journalpost.tilAvvik();
    assertThat(avvikListe).hasSize(3);
    assertThat(avvikListe).contains(AvvikType.OVERFOR_TIL_ANNEN_ENHET);
    assertThat(avvikListe).contains(AvvikType.TREKK_JOURNALPOST);
    assertThat(avvikListe).contains(AvvikType.ENDRE_FAGOMRADE);
  }

  @Test
  @DisplayName("skal returnere avvik FEILFORE_SAK hvis status ikke er feilfort")
  void skalHenteAvvikFEILFORE_SAKHvisIkkeFeilfort() {
    var journalpost = new Journalpost();
    journalpost.setJournalstatus(JournalStatus.JOURNALFOERT);
    journalpost.setJournalposttype("I");
    journalpost.setSak(new Sak(""));
    var avvikListe = journalpost.tilAvvik();
    assertThat(avvikListe).hasSize(2);
    assertThat(avvikListe).contains(AvvikType.FEILFORE_SAK);
  }

  @Test
  @DisplayName("skal ikke tillate avvik FEILFORE_SAK hvis status feilregistrert")
  void skalHenteAvvikHvisStatusFeilregistrert() {
    var journalpost = new Journalpost();
    journalpost.setJournalstatus(JournalStatus.FEILREGISTRERT);
    journalpost.setJournalposttype("I");
    journalpost.setSak(new Sak(""));
    var avvikListe = journalpost.tilAvvik();
    assertThat(avvikListe).hasSize(1);
    assertThat(avvikListe).doesNotContain(AvvikType.FEILFORE_SAK);
  }
}
