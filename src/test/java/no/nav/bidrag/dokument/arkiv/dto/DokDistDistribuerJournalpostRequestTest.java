package no.nav.bidrag.dokument.arkiv.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import no.nav.bidrag.transport.dokument.DistribuerTilAdresse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DokDistDistribuerJournalpostRequest")
class DokDistDistribuerJournalpostRequestTest {

  @Test
  @DisplayName("skal mappe DokDistDistribuerJournalpostRequest")
  void skalMappeDokDistDistribuerJournalpostRequest() throws IOException {
    var jpid = 123123;
    var distribuerTilAdresse =
        new DistribuerTilAdresse(
            "Adresselinje1", "Adresselinje2", "Adresselinje3", "NO", "3000", "Ingen");

    var request =
        new DokDistDistribuerJournalpostRequest(jpid, "asdasd", "", distribuerTilAdresse, null);
    assertAll(
        () -> assertThat(request.getBestillendeFagsystem()).isEqualTo("BIDRAG"),
        () -> assertThat(request.getDokumentProdApp()).isEqualTo("bidragDokArkiv"),
        () -> assertThat(request.getDistribusjonstype()).isEqualTo(DistribusjonsType.VIKTIG),
        () ->
            assertThat(request.getDistribusjonstidspunkt())
                .isEqualTo(DistribusjonsTidspunkt.KJERNETID));
  }

  @Test
  @DisplayName("skal distribuerTilAdresse til DokdistDistribuerTilAdresse")
  void skalMappeDistribuerTilAdresseTilDokdistDistribuerTilAdresse() throws IOException {
    var jpid = 123123;
    var distribuerTilAdresse =
        new DistribuerTilAdresse(
            "Adresselinje1", "Adresselinje2", "Adresselinje3", "NO", "3000", "Ingen");

    var request =
        new DokDistDistribuerJournalpostRequest(jpid, "asdasd", "", distribuerTilAdresse, null);
    var mappedAdresse = request.getAdresse();
    assertAll(
        () ->
            assertThat(mappedAdresse.getAdresselinje1())
                .isEqualTo(distribuerTilAdresse.getAdresselinje1()),
        () ->
            assertThat(mappedAdresse.getAdresselinje2())
                .isEqualTo(distribuerTilAdresse.getAdresselinje2()),
        () ->
            assertThat(mappedAdresse.getAdresselinje3())
                .isEqualTo(distribuerTilAdresse.getAdresselinje3()),
        () -> assertThat(mappedAdresse.getLand()).isEqualTo(distribuerTilAdresse.getLand()),
        () ->
            assertThat(mappedAdresse.getPostnummer())
                .isEqualTo(distribuerTilAdresse.getPostnummer()),
        () -> assertThat(mappedAdresse.getPoststed()).isEqualTo(distribuerTilAdresse.getPoststed()),
        () ->
            assertThat(mappedAdresse.getAdressetype())
                .isEqualTo(DokDistAdresseType.NorskPostadresse.getVerdi()),
        () -> assertThat(request.getDistribusjonstype()).isEqualTo(DistribusjonsType.VIKTIG),
        () ->
            assertThat(request.getDistribusjonstidspunkt())
                .isEqualTo(DistribusjonsTidspunkt.KJERNETID));
  }

  @Test
  @DisplayName("skal mappe adressetype til utenlandsk adresse")
  void skalMappeTilUtenlandskAdresseHvisLandIkkeErNO() throws IOException {
    var jpid = 123123;
    var distribuerTilAdresse =
        new DistribuerTilAdresse(
            "Adresselinje1", "Adresselinje2", "Adresselinje3", "SE", "3000", "Poststed");

    var request =
        new DokDistDistribuerJournalpostRequest(jpid, null, null, distribuerTilAdresse, null);
    var mappedAdresse = request.getAdresse();

    assertAll(
        () ->
            assertThat(mappedAdresse.getAdressetype())
                .isEqualTo(DokDistAdresseType.UtenlandskPostadresse.getVerdi()),
        () ->
            assertThat(mappedAdresse.getAdresselinje1())
                .isEqualTo("Adresselinje1"),
        () ->
            assertThat(mappedAdresse.getAdresselinje2())
                .isEqualTo("Adresselinje2, 3000 Poststed"),
        () ->
            assertThat(mappedAdresse.getPoststed()).isNull(),
        () ->
            assertThat(mappedAdresse.getPostnummer()).isNull()
    );
  }

  @Test
  void skalMappeTilUtenlandskAdresseHvisLandIkkeErNOUtenPostssted() throws IOException {
    var jpid = 123123;
    var distribuerTilAdresse =
        new DistribuerTilAdresse(
            "Adresselinje1", "Adresselinje2", "Adresselinje3", "SE", "3000", null);

    var request =
        new DokDistDistribuerJournalpostRequest(jpid, null, null, distribuerTilAdresse, null);
    var mappedAdresse = request.getAdresse();

    assertAll(
        () ->
            assertThat(mappedAdresse.getAdressetype())
                .isEqualTo(DokDistAdresseType.UtenlandskPostadresse.getVerdi()),
        () ->
            assertThat(mappedAdresse.getAdresselinje2())
                .isEqualTo("Adresselinje2, 3000"),
        () ->
            assertThat(mappedAdresse.getPoststed()).isNull(),
        () ->
            assertThat(mappedAdresse.getPostnummer()).isNull()
    );
  }

  @Test
  void skalMappeTilUtenlandskAdresseHvisLandIkkeErNOUtenPostnummer() throws IOException {
    var jpid = 123123;
    var distribuerTilAdresse =
        new DistribuerTilAdresse(
            "Adresselinje1", "Adresselinje2", "Adresselinje3", "SE", null, "Poststed");

    var request =
        new DokDistDistribuerJournalpostRequest(jpid, null, null, distribuerTilAdresse, null);
    var mappedAdresse = request.getAdresse();

    assertAll(
        () ->
            assertThat(mappedAdresse.getAdressetype())
                .isEqualTo(DokDistAdresseType.UtenlandskPostadresse.getVerdi()),
        () ->
            assertThat(mappedAdresse.getAdresselinje2())
                .isEqualTo("Adresselinje2, Poststed"),
        () ->
            assertThat(mappedAdresse.getPoststed()).isNull(),
        () ->
            assertThat(mappedAdresse.getPostnummer()).isNull()
    );
  }

  @Test
  void skalMappeTilUtenlandskAdresseHvisLandIkkeErNOUtenPostnummerOgPossted() throws IOException {
    var jpid = 123123;
    var distribuerTilAdresse =
        new DistribuerTilAdresse(
            "Adresselinje1", "Adresselinje2", "Adresselinje3", "SE", null, null);

    var request =
        new DokDistDistribuerJournalpostRequest(jpid, null, null, distribuerTilAdresse, null);
    var mappedAdresse = request.getAdresse();

    assertAll(
        () ->
            assertThat(mappedAdresse.getAdressetype())
                .isEqualTo(DokDistAdresseType.UtenlandskPostadresse.getVerdi()),
        () ->
            assertThat(mappedAdresse.getAdresselinje2())
                .isEqualTo("Adresselinje2"),
        () ->
            assertThat(mappedAdresse.getPoststed()).isNull(),
        () ->
            assertThat(mappedAdresse.getPostnummer()).isNull()
    );
  }

  @Test
  void skalMappeTilUtenlandskAdresseHvisLandIkkeErNOUtenAdresselinje2() throws IOException {
    var jpid = 123123;
    var distribuerTilAdresse =
        new DistribuerTilAdresse(
            "Adresselinje1", null, "Adresselinje3", "SE", "postnummer", "poststed");

    var request =
        new DokDistDistribuerJournalpostRequest(jpid, null, null, distribuerTilAdresse, null);
    var mappedAdresse = request.getAdresse();

    assertAll(
        () ->
            assertThat(mappedAdresse.getAdressetype())
                .isEqualTo(DokDistAdresseType.UtenlandskPostadresse.getVerdi()),
        () ->
            assertThat(mappedAdresse.getAdresselinje2())
                .isEqualTo("postnummer poststed"),
        () ->
            assertThat(mappedAdresse.getPoststed()).isNull(),
        () ->
            assertThat(mappedAdresse.getPostnummer()).isNull()
    );
  }

  @Test
  @DisplayName("skal ikke mappe adresse hvis distribuerTilAdresse er null")
  void skalIkkeMappeAdresseHvisDistribuerTilAdresseErNull() throws IOException {
    var jpid = 123123;

    var request = new DokDistDistribuerJournalpostRequest(jpid, null, null, null, null);
    assertAll(
        () -> assertThat(request.getAdresse()).isNull(),
        () -> assertThat(request.getDistribusjonstype()).isEqualTo(DistribusjonsType.VIKTIG));
  }

  @Test
  @DisplayName("skal mappe distribusjonstype vedtak hvis tittel inneholder vedtak")
  void skalMappeDistribusjonsTypeVedtakHvisTittelInneholderVedtak() throws IOException {
    var jpid = 123123;

    var request =
        new DokDistDistribuerJournalpostRequest(
            jpid, "???", "Brev som inneholder VedTak", null, null);
    assertAll(() -> assertThat(request.getDistribusjonstype()).isEqualTo(DistribusjonsType.VEDTAK));
  }

  @Test
  @DisplayName("skal mappe distribusjonstype vedtak hvis tittel inneholder decision")
  void skalMappeDistribusjonsTypeVedtakHvisTittelInneholderDecision() throws IOException {
    var jpid = 123123;

    var request = new DokDistDistribuerJournalpostRequest(jpid, "???", "Some deciSion", null, null);
    assertAll(() -> assertThat(request.getDistribusjonstype()).isEqualTo(DistribusjonsType.VEDTAK));
  }

  @Test
  @DisplayName(
      "skal mappe distribusjonstype viktig hvis tittel ikke inneholder vedtak eller decision")
  void skalMappeDistribusjonsTypeViktigHvisTittelIkkeInneholderVedtakDecision() throws IOException {
    var jpid = 123123;

    var request =
        new DokDistDistribuerJournalpostRequest(
            jpid, "???", "Brev med en tittel vedta decisio", null, null);
    assertAll(() -> assertThat(request.getDistribusjonstype()).isEqualTo(DistribusjonsType.VIKTIG));
  }

  @Test
  @DisplayName("skal mappe distribusjonstype vedtak")
  void skalMappeDistribusjonsTypeVedtak() throws IOException {
    var jpid = 123123;

    var request = new DokDistDistribuerJournalpostRequest(jpid, "BI01A01", null, null, null);
    assertAll(() -> assertThat(request.getDistribusjonstype()).isEqualTo(DistribusjonsType.VEDTAK));
  }

  @Test
  @DisplayName("skal mappe distribusjonstype viktig")
  void skalMappeDistribusjonsTypeViktig() throws IOException {
    var jpid = 123123;

    var request = new DokDistDistribuerJournalpostRequest(jpid, "BI01A03", null, null, null);
    assertAll(() -> assertThat(request.getDistribusjonstype()).isEqualTo(DistribusjonsType.VIKTIG));
  }
}
