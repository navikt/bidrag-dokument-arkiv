package no.nav.bidrag.dokument.arkiv.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DokDistDistribuerJournalpostRequest")
class DokDistDistribuerJournalpostRequestTest {

  @Test
  @DisplayName("skal distribuerTilAdresse til DokdistDistribuerTilAdresse")
  void skalMappeDistribuerTilAdresseTilDokdistDistribuerTilAdresse() throws IOException {
    var jpid = 123123;
    var distribuerTilAdresse = new DistribuerTilAdresse(
        "Adresselinje1",
        "Adresselinje2",
        "Adresselinje3",
        "NO",
        "3000",
        "Ingen"
    );

    var request = new DokDistDistribuerJournalpostRequest(jpid, distribuerTilAdresse);
    var mappedAdresse = request.getAdresse();
    assertAll(
        () -> assertThat(mappedAdresse.getAdresselinje1()).isEqualTo(distribuerTilAdresse.getAdresselinje1()),
        () -> assertThat(mappedAdresse.getAdresselinje2()).isEqualTo(distribuerTilAdresse.getAdresselinje2()),
        () -> assertThat(mappedAdresse.getAdresselinje3()).isEqualTo(distribuerTilAdresse.getAdresselinje3()),
        () -> assertThat(mappedAdresse.getLand()).isEqualTo(distribuerTilAdresse.getLand()),
        () -> assertThat(mappedAdresse.getPostnummer()).isEqualTo(distribuerTilAdresse.getPostnummer()),
        () -> assertThat(mappedAdresse.getPoststed()).isEqualTo(distribuerTilAdresse.getPoststed()),
        () -> assertThat(mappedAdresse.getAdressetype()).isEqualTo(DokDistAdresseType.norskPostadresse)
    );
  }

  @Test
  @DisplayName("skal mappe adressetype til utenlandsk adresse")
  void skalMappeTilUtenlandskAdresseHvisLandIkkeErNO() throws IOException {
    var jpid = 123123;
    var distribuerTilAdresse = new DistribuerTilAdresse(
        "Adresselinje1",
        "Adresselinje2",
        "Adresselinje3",
        "SE",
        "3000",
        "Ingen"
    );

    var request = new DokDistDistribuerJournalpostRequest(jpid, distribuerTilAdresse);
    var mappedAdresse = request.getAdresse();
    assertAll(
        () -> assertThat(mappedAdresse.getAdressetype()).isEqualTo(DokDistAdresseType.utenlandskPostadresse)
    );
  }

  @Test
  @DisplayName("skal ikke mappe adresse hvis distribuerTilAdresse er null")
  void skalIkkeMappeAdresseHvisDistribuerTilAdresseErNull() throws IOException {
    var jpid = 123123;

    var request = new DokDistDistribuerJournalpostRequest(jpid, null);
    assertAll(
        () -> assertThat(request.getAdresse()).isNull()
    );
  }

}
