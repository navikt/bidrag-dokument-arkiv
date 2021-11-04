package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.dokument.arkiv.stubs.Stubs.SAKSNUMMER_JOURNALPOST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.dto.AktorDto;
import no.nav.bidrag.dokument.dto.EndreDokument;
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.bidrag.dokument.dto.JournalpostResponse;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class JournalpostControllerTest extends AbstractControllerTest  {

  @Test
  @DisplayName("should map context path with random port")
  void shouldMapToContextPath() {
    assertThat(initUrl()).isEqualTo("http://localhost:" + port + "/bidrag-dokument-arkiv");
  }

  @Test
  @DisplayName("skal ha 400 BAD REQUEST når prefix mangler")
  void skalHaBadRequestNarPrefixPaJournalpostIdMangler() {
    var journalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/1?saknummer=007",
        HttpMethod.GET,
        null,
        JournalpostDto.class
    );

    assertThat(journalpostResponseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("skal ha 400 BAD REQUEST når prefix er feil")
  void skalHaBadRequestlNarPrefixPaJournalpostIdErFeil() {
    var journalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/BID-1?saksnummer=007",
        HttpMethod.GET,
        null,
        JournalpostDto.class
    );

    assertThat(journalpostResponseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("skal ha body som er null samt header warning når journalpost ikke finnes")
  void skalHaBodySomErNullSamtHeaderWarningNarJournalpostIkkeFinnes() throws IOException {
    stubs.mockSafResponseHentJournalpost(journalpostSafNotFoundResponse, HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);

    var journalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-1?saksnummer=007",
        HttpMethod.GET,
        null,
        String.class
    );

    assertThat(Optional.of(journalpostResponseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getBody()).as("body").isNull(),
        () -> assertThat(response.getHeaders().get(HttpHeaders.WARNING)).as("header warning").first()
            .isEqualTo("Fant ikke journalpost i fagarkivet. journalpostId=910536260"),
        () -> assertThat(response.getStatusCode()).as("status").isEqualTo(HttpStatus.NOT_FOUND),
        () -> stubs.verifyStub.verifySafHentJournalpostRequested()
    ));
  }

  @Test
  @DisplayName("skal få 400 BAD REQUEST når eksisterende journalpost er knyttet til annen sak")
  void skalFaBadRequestNarEksisterendeJournalpostErKnyttetTilAnnenSak() throws IOException {
    var journalpostIdFraJson = 201028011;
    stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "?saksnummer=007",
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(response.getBody()).isNull(),
        () -> stubs.verifyStub.verifySafHentJournalpostRequested()
    ));
  }

  @Test
  @DisplayName("skal få 500 INTERNAL SERVER når person api feiler")
  void skalFaServerFeilNarPersonApietFeiler() throws IOException {
    var journalpostIdFraJson = 201028011;
    stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK);
    stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.BAD_REQUEST);

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(response.getBody()).isNull(),
        () -> stubs.verifyStub.verifySafHentJournalpostRequested(),
        () -> stubs.verifyStub.verifyPersonRequested()
    ));
  }


  @Test
  @DisplayName("skal hente Journalpost uten saker når den eksisterer")
  void skalHenteJournalpostUtenSakerNarDenEksisterer() throws IOException {
    var journalpostIdFraJson = 201028011;

    stubs.mockSafResponseHentJournalpost(responseJournalpostIngenSakerJson, HttpStatus.OK);
    stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    var journalpost = responseEntity.getBody() != null ? responseEntity.getBody().getJournalpost() : null;
    var saker = responseEntity.getBody() != null ? responseEntity.getBody().getSakstilknytninger() : null;

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getInnhold).isEqualTo("Filosofens bidrag"),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getJournalpostId).isEqualTo("JOARK-" + journalpostIdFraJson),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getGjelderAktor).extracting(AktorDto::getIdent).isEqualTo(PERSON_IDENT),
        () -> assertThat(saker).isNotNull().hasSize(0),
        () -> stubs.verifyStub.verifySafHentJournalpostRequested(),
        () -> stubs.verifyStub.verifySafTilknyttedeJournalpostedRequested(0),
        () -> stubs.verifyStub.verifyPersonRequested()
    ));
  }

  @Test
  @DisplayName("skal hente Journalpost når den eksisterer")
  void skalHenteJournalpostNarDenEksisterer() throws IOException {
    var journalpostIdFraJson = 201028011;

    stubs.mockSafResponseHentJournalpost(journalpostJournalfortSafResponse, HttpStatus.OK);
    stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "?saksnummer=5276661",
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    var journalpost = responseEntity.getBody() != null ? responseEntity.getBody().getJournalpost() : null;
    var saker = responseEntity.getBody() != null ? responseEntity.getBody().getSakstilknytninger() : null;

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getInnhold).isEqualTo("Filosofens bidrag"),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getJournalpostId).isEqualTo("JOARK-" + journalpostIdFraJson),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getGjelderAktor).extracting(AktorDto::getIdent).isEqualTo(PERSON_IDENT),
        () -> assertThat(saker).isNotNull().hasSize(3).contains("2106585").contains("5276661"),
        () -> stubs.verifyStub.verifySafHentJournalpostRequested(),
        () -> stubs.verifyStub.verifySafTilknyttedeJournalpostedRequested(),
        () -> stubs.verifyStub.verifyPersonRequested()
    ));
  }

  @Test
  @DisplayName("skal hente journalposter for en bidragssak")
  void skalHenteJournalposterForEnBidragssak() throws IOException {
    stubs.mockSafResponseDokumentOversiktFagsak(HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);

    var jouralposterResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/sak/5276661/journal?fagomrade=BID", HttpMethod.GET, null, listeMedJournalposterTypeReference()
    );

    assertAll(
        () -> assertThat(jouralposterResponseEntity).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.OK),
        () -> assertThat(jouralposterResponseEntity.getBody()).hasSize(3),
        () -> stubs.verifyStub.verifyPersonRequested(),
        () -> stubs.verifyStub.verifySafDokumentOversiktFagsakRequested()
    );
  }

  private ParameterizedTypeReference<List<JournalpostDto>> listeMedJournalposterTypeReference() {
    return new ParameterizedTypeReference<>() {
    };
  }

  @Test
  @DisplayName("skal endre og journalføre journalpost")
  void skalEndreOgJournalforeJournalpost() throws IOException, JSONException {
    // given

    var xEnhet = "1234";
    var journalpostIdFraJson = 201028011L;
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);

    var endreJournalpostCommand = createEndreJournalpostCommand();
    endreJournalpostCommand.setSkalJournalfores(true);
    endreJournalpostCommand.setTilknyttSaker(Arrays.asList("5276661"));

    stubs.mockSafResponseHentJournalpost(HttpStatus.OK);
    stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);
    stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson);
    stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson);

    // when
    var oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.PATCH,
        new HttpEntity<>(endreJournalpostCommand, headersMedEnhet),
        JournalpostDto.class
    );

    // then
    assertAll(
        () -> assertThat(oppdaterJournalpostResponseEntity)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.OK),
        ()->stubs.verifyStub.verifyDokarkivOppdaterRequest(journalpostIdFraJson, String.format("\"fagsakId\":\"%s\"", "5276661")),
        ()->stubs.verifyStub.verifyDokarkivOppdaterRequest(journalpostIdFraJson, "\"fagsaksystem\":\"BISYS\""),
        ()->stubs.verifyStub.verifyDokarkivOppdaterRequest(journalpostIdFraJson, "\"sakstype\":\"FAGSAK\""),
        ()->stubs.verifyStub.verifyDokarkivOppdaterRequest(journalpostIdFraJson, "\"bruker\":{\"id\":\"06127412345\",\"idType\":\"FNR\"}"),
        ()->stubs.verifyStub.verifyDokarkivOppdaterRequest(journalpostIdFraJson, "\"dokumenter\":[{\"dokumentInfoId\":\"1\",\"tittel\":\"In a galazy far far away\",\"brevkode\":\"BLABLA\"}]"),
        ()->stubs.verifyStub.verifyDokarkivOppdaterRequest(journalpostIdFraJson, "\"avsenderMottaker\":{\"navn\":\"Dauden, Svarte\"}"),
        ()->stubs.verifyStub.verifyDokarkivFerdigstillRequested(journalpostIdFraJson)
    );
  }

  @Test
  @DisplayName("skal endre og journalføre journalpost med flere saker")
  void skalEndreOgJournalforeJournalpostMedFlereSaker() throws IOException, JSONException {
    var xEnhet = "1234";
    var saksnummer1 = "200000";
    var saksnummer2 = "200001";
    var journalpostIdFraJson = 201028011L;
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);

    var endreJournalpostCommand = createEndreJournalpostCommand();
    endreJournalpostCommand.setSkalJournalfores(true);
    endreJournalpostCommand.setTilknyttSaker(Arrays.asList(saksnummer1, saksnummer2));

    stubs.mockSafResponseHentJournalpost(HttpStatus.OK);
    stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);
    stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson);
    stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson);
    stubs.mockDokarkivProxyTilknyttRequest(journalpostIdFraJson);

    // when
    var oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.PATCH,
        new HttpEntity<>(endreJournalpostCommand, headersMedEnhet),
        JournalpostDto.class
    );

    // then
    assertAll(
        () -> assertThat(oppdaterJournalpostResponseEntity)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.OK),
        ()->stubs.verifyStub.verifyDokarkivFerdigstillRequested(journalpostIdFraJson),
        ()->stubs.verifyStub.verifyDokarkivProxyTilknyttSakerRequested(0, journalpostIdFraJson, saksnummer1),
        ()->stubs.verifyStub.verifyDokarkivProxyTilknyttSakerRequested(journalpostIdFraJson, saksnummer2, "\"journalfoerendeEnhet\":\"4806\"")
    );
  }

  @Test
  @DisplayName("skal endre journalført journalpost med flere saker")
  void skalEndreJournalfortJournalpostMedFlereSaker() throws IOException, JSONException {
    var xEnhet = "1234";
    var existingSaksnummer = SAKSNUMMER_JOURNALPOST;
    var newSaksnummer = "200000";
    var journalpostIdFraJson = 201028011L;
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);

    var endreJournalpostCommand = createEndreJournalpostCommand();
    endreJournalpostCommand.setSkalJournalfores(false);
    endreJournalpostCommand.setTilknyttSaker(Arrays.asList(existingSaksnummer, newSaksnummer));
    endreJournalpostCommand.setDokumentDato(LocalDate.of(2020, 2, 3));

    stubs.mockSafResponseHentJournalpost("journalpostJournalfortSafResponse.json", HttpStatus.OK);
    stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);
    stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson);
    stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson);
    stubs.mockDokarkivProxyTilknyttRequest(journalpostIdFraJson);

    // when
    var oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.PATCH,
        new HttpEntity<>(endreJournalpostCommand, headersMedEnhet),
        JournalpostDto.class
    );

    // then
    assertAll(
        () -> assertThat(oppdaterJournalpostResponseEntity)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.OK),
        ()->stubs.verifyStub.verifyDokarkivOppdaterRequest(journalpostIdFraJson, "\"tittel\":\"So Tired\"","\"avsenderMottaker\":{\"navn\":\"Dauden, Svarte\"}","\"datoMottatt\":\"2020-02-03\"", "\"dokumenter\":[{\"dokumentInfoId\":\"1\",\"tittel\":\"In a galazy far far away\",\"brevkode\":\"BLABLA\"}]"),
        ()->stubs.verifyStub.verifyDokarkivProxyTilknyttSakerRequested(journalpostIdFraJson, newSaksnummer, "\"journalfoerendeEnhet\":\"4806\""),
        ()->stubs.verifyStub.verifyDokarkivProxyTilknyttSakerRequested(0, journalpostIdFraJson, existingSaksnummer, "\"journalfoerendeEnhet\":\"4806\"")
    );
  }

  @Test
  @DisplayName("skal endre journalpost uten journalføring")
  void skalEndreJournalpostUtenJournalforing() throws IOException, JSONException {
    // given
    var xEnhet = "1234";
    var journalpostIdFraJson = 201028011L;
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);

    var endreJournalpostCommand = createEndreJournalpostCommand();

    stubs.mockSafResponseHentJournalpost(HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);
    stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson);

    // when
    var oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.PATCH,
        new HttpEntity<>(endreJournalpostCommand, headersMedEnhet),
        JournalpostDto.class
    );

    // then
    assertAll(
        () -> assertThat(oppdaterJournalpostResponseEntity)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.OK),
        ()->stubs.verifyStub.verifyDokarkivOppdaterRequest(journalpostIdFraJson, ""),
        ()->stubs.verifyStub.verifyDokarkivFerdigstillRequested(0, journalpostIdFraJson)
    );
  }

  private EndreJournalpostCommand createEndreJournalpostCommand(){
    var endreJournalpostCommand = new EndreJournalpostCommand();
    endreJournalpostCommand.setAvsenderNavn("Dauden, Svarte");
    endreJournalpostCommand.setGjelder("06127412345");
    endreJournalpostCommand.setTittel("So Tired");
    endreJournalpostCommand.setEndreDokumenter(List.of(
        new EndreDokument("BLABLA", 1, "In a galazy far far away")
    ));
    return endreJournalpostCommand;
  }

}
