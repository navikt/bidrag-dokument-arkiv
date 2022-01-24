package no.nav.bidrag.dokument.arkiv.service;

import static no.nav.bidrag.dokument.arkiv.dto.DistribusjonKt.validerKanDistribueres;
import static no.nav.bidrag.dokument.arkiv.stubs.TestDataKt.createDistribuerTilAdresse;

import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker;
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottakerIdType;
import no.nav.bidrag.dokument.arkiv.dto.DistribuerJournalpostRequestInternal;
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.dto.DistribuerJournalpostRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class DistribuerJournalpostServiceTest {

  @Test
  @DisplayName("skal validere distribuer journalpost")
  void skalValidere() {
    Assertions.assertDoesNotThrow(()->validerKanDistribueres(createValidJournalpost(), new DistribuerJournalpostRequestInternal(
        createValidDistribuerJournalpostRequest())));
  }

  @Test
  @DisplayName("skal feile validering av distribuer journalpost hvis status ikke er FERDIGSTILT")
  void skalIkkeValidereHvisStatusIkkeErFerdigstilt() {
    var jp = createValidJournalpost();
    jp.setJournalstatus(JournalStatus.JOURNALFOERT);
    Assertions.assertThrows(IllegalArgumentException.class, ()->validerKanDistribueres(jp, new DistribuerJournalpostRequestInternal(
        createValidDistribuerJournalpostRequest())), "skal feile hvis status ikke er FERDIGSTILT");
  }

  @Test
  @DisplayName("skal feile validering av distribuer journalpost hvis tena ikke er BID")
  void skalIkkeValidereHvisTemaIkkeErBID() {
    var jp = createValidJournalpost();
    jp.setTema("FAR");
    Assertions.assertThrows(IllegalArgumentException.class, ()->validerKanDistribueres(jp, new DistribuerJournalpostRequestInternal(
        createValidDistribuerJournalpostRequest())), "Skal feile hvis tema ikke er BID");
  }

  @Test
  @DisplayName("skal feile validering av distribuer journalpost ikke har mottakerid satt")
  void skalIkkeValidereHvisJournalpostIkkeHarMottakerIdSatt() {
    var jp = createValidJournalpost();
    jp.setAvsenderMottaker(new AvsenderMottaker());
    Assertions.assertThrows(IllegalArgumentException.class, ()->validerKanDistribueres(jp, new DistribuerJournalpostRequestInternal(
        createValidDistribuerJournalpostRequest())), "Skal feile hvis mottakerid ikke er satt");
  }

  @Test
  @DisplayName("skal feile validering av distribuer journalpost hvis adresse ikke er satt")
  void skalIkkeValidereHvisJournalpostHvisAdresseIkkeErSatt() {
    var request = createValidDistribuerJournalpostRequest();
    request.setAdresse(null);
    Assertions.assertThrows(IllegalArgumentException.class, ()->validerKanDistribueres(createValidJournalpost(), new DistribuerJournalpostRequestInternal(request)), "Skal feile hvis adresse ikke er satt");
  }

  private DistribuerJournalpostRequest createValidDistribuerJournalpostRequest(){
    return new DistribuerJournalpostRequest(createDistribuerTilAdresse());
  }

  private Journalpost createValidJournalpost(){
    var journalpost = new Journalpost();
    journalpost.setJournalstatus(JournalStatus.FERDIGSTILT);
    journalpost.setTema("BID");
    journalpost.setAvsenderMottaker(new AvsenderMottaker("test", "123213213", AvsenderMottakerIdType.FNR));
    return journalpost;
  }

}
