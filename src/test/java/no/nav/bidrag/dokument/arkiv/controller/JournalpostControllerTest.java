package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.dokument.arkiv.stubs.Stubs.SAKSNUMMER_JOURNALPOST;
import static no.nav.bidrag.dokument.arkiv.stubs.TestDataKt.createDistribuerTilAdresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsTidspunkt;
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsType;
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.JournalstatusDto;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.dto.AktorDto;
import no.nav.bidrag.dokument.dto.DistribuerJournalpostRequest;
import no.nav.bidrag.dokument.dto.EndreDokument;
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand;
import no.nav.bidrag.dokument.dto.EndreReturDetaljer;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.bidrag.dokument.dto.JournalpostResponse;
import no.nav.bidrag.dokument.dto.KodeDto;
import no.nav.bidrag.dokument.dto.ReturDetaljerLog;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class JournalpostControllerTest extends AbstractControllerTest {

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
        () -> stubs.verifyStub.harEnSafKallEtterHentJournalpost()
    ));
  }

  @Test
  @DisplayName("skal få 404 NOT FOUND når eksisterende journalpost er knyttet til annen sak")
  void skalFaNotFoundNarEksisterendeJournalpostErKnyttetTilAnnenSak() throws IOException {
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
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
        () -> assertThat(response.getBody()).isNull(),
        () -> stubs.verifyStub.harEnSafKallEtterHentJournalpost()
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
        () -> stubs.verifyStub.harEnSafKallEtterHentJournalpost(),
        () -> stubs.verifyStub.bidragPersonKalt()
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
        () -> stubs.verifyStub.harEnSafKallEtterHentJournalpost(),
        () -> stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter(),
        () -> stubs.verifyStub.bidragPersonKalt()
    ));
  }

  @Test
  @DisplayName("skal hente distribuert Journalpost med adresse")
  void skalHenteDistribuertJournalpostMedAdresse() throws IOException {
    var journalpostIdFraJson = 201028011;

    stubs.mockSafResponseHentJournalpost(responseJournalpostJsonWithAdresse, HttpStatus.OK);
    stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    var journalpost = responseEntity.getBody() != null ? responseEntity.getBody().getJournalpost() : null;
    var distribuertTilAdresse = journalpost.getDistribuertTilAdresse();
    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getJournalpostId).isEqualTo("JOARK-" + journalpostIdFraJson),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getJournalstatus).isEqualTo(JournalstatusDto.EKSPEDERT),
        () -> assertThat(distribuertTilAdresse).isNotNull(),
        () -> assertThat(distribuertTilAdresse.getAdresselinje1()).isEqualTo("Testveien 20A"),
        () -> assertThat(distribuertTilAdresse.getAdresselinje2()).isEqualTo("TestLinje2"),
        () -> assertThat(distribuertTilAdresse.getAdresselinje3()).isEqualTo("TestLinje4"),
        () -> assertThat(distribuertTilAdresse.getPostnummer()).isEqualTo("7950"),
        () -> assertThat(distribuertTilAdresse.getPoststed()).isEqualTo("ABELVÆR"),
        () -> assertThat(distribuertTilAdresse.getLand()).isEqualTo("NO"),
        () -> stubs.verifyStub.harEnSafKallEtterHentJournalpost(),
        () -> stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter(),
        () -> stubs.verifyStub.bidragPersonKalt()
    ));
  }

  @Test
  @DisplayName("skal hente Journalpost med retur detaljer")
  void skalHenteJournalpostMedReturDetaljer() throws IOException {
    var journalpostIdFraJson = 201028011;

    stubs.mockSafResponseHentJournalpost(responseJournalpostJsonWithReturDetaljer, HttpStatus.OK);
    stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    var journalpost = responseEntity.getBody() != null ? responseEntity.getBody().getJournalpost() : null;
    var returDetaljer = journalpost.getReturDetaljer();
    var returDetaljerLog = journalpost.getReturDetaljer().getLogg();
    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getJournalpostId).isEqualTo("JOARK-" + journalpostIdFraJson),
        () -> assertThat(returDetaljer).isNotNull(),
        () -> assertThat(returDetaljerLog.size()).isEqualTo(3),
        () -> assertThat(returDetaljerLog.contains(new ReturDetaljerLog(LocalDate.parse("2020-11-15"), "Beskrivelse av retur"))).isTrue(),
        () -> assertThat(returDetaljerLog.contains(new ReturDetaljerLog(LocalDate.parse("2020-12-14"), "Beskrivelse av retur mer tekst for å teste lengre verdier"))).isTrue(),
        () -> assertThat(returDetaljerLog.contains(new ReturDetaljerLog(LocalDate.parse("2020-12-15"), "Beskrivelse av retur 2 mer tekst for å teste lengre verdier"))).isTrue(),
        () -> assertThat(returDetaljer.getAntall()).isEqualTo(6),
        () -> assertThat(returDetaljer.getDato()).isEqualTo(LocalDate.parse("2020-12-15")),
        () -> stubs.verifyStub.harEnSafKallEtterHentJournalpost(),
        () -> stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter(),
        () -> stubs.verifyStub.bidragPersonKalt()
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
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getBrevkode).extracting(KodeDto::getKode).isEqualTo("BI01S02"),
        () -> assertThat(saker).isNotNull().hasSize(3).contains("2106585").contains("5276661"),
        () -> stubs.verifyStub.harEnSafKallEtterHentJournalpost(),
        () -> stubs.verifyStub.harEnSafKallEtterTilknyttedeJournalposter(),
        () -> stubs.verifyStub.bidragPersonKalt()
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
        () -> stubs.verifyStub.bidragPersonKalt(),
        () -> stubs.verifyStub.harSafEnKallEtterDokumentOversiktFagsak()
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
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, String.format("\"fagsakId\":\"%s\"", "5276661")),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "\"fagsaksystem\":\"BISYS\""),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "\"sakstype\":\"FAGSAK\""),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "\"bruker\":{\"id\":\"06127412345\",\"idType\":\"FNR\"}"),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson,
            "\"dokumenter\":[{\"dokumentInfoId\":\"1\",\"tittel\":\"In a galazy far far away\",\"brevkode\":\"BLABLA\"}]"),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "\"avsenderMottaker\":{\"navn\":\"Dauden, Svarte\"}"),
        () -> stubs.verifyStub.dokarkivFerdigstillKalt(journalpostIdFraJson)
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
        () -> stubs.verifyStub.dokarkivFerdigstillKalt(journalpostIdFraJson),
        () -> stubs.verifyStub.dokarkivProxyTilknyttSakerIkkeKalt(journalpostIdFraJson, saksnummer1),
        () -> stubs.verifyStub.dokarkivProxyTilknyttSakerKalt(journalpostIdFraJson, saksnummer2, "\"journalfoerendeEnhet\":\"4806\"")
    );
  }

  @Test
  @DisplayName("skal endre utgaaende journalpost retur detaljer")
  void skalEndreUtgaaendeJournalpostReturDetaljer() throws IOException, JSONException {
    var xEnhet = "1234";
    var journalpostIdFraJson = 201028011L;
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);

    var endreJournalpostCommand = createEndreJournalpostCommand();
    endreJournalpostCommand.setSkalJournalfores(false);
    endreJournalpostCommand.setEndreReturDetaljer(
        List.of(
            new EndreReturDetaljer(LocalDate.parse("2020-11-15"), null, "Ny beskrivelse 1"),
            new EndreReturDetaljer(LocalDate.parse("2020-12-15"), LocalDate.parse("2020-10-10"), "Ny beskrivelse 2")));
    stubs.mockSafResponseHentJournalpost(responseJournalpostJsonWithReturDetaljer, HttpStatus.OK);
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
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "\"tilleggsopplysninger\":["
            + "{\"nokkel\":\"retur0_2020-11-15\",\"verdi\":\"Ny beskrivelse 1\"},"
            + "{\"nokkel\":\"retur0_2020-12-14\",\"verdi\":\"Beskrivelse av retur mer tekst for å teste lengre verdier\"},"
            + "{\"nokkel\":\"retur0_2020-10-10\",\"verdi\":\"Ny beskrivelse 2\"}]")
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
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "\"tittel\":\"So Tired\""),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "\"avsenderMottaker\":{\"navn\":\"Dauden, Svarte\"}"),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "\"datoMottatt\":\"2020-02-03\""),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson,
            "\"dokumenter\":[{\"dokumentInfoId\":\"1\",\"tittel\":\"In a galazy far far away\",\"brevkode\":\"BLABLA\"}]"),
        () -> stubs.verifyStub.dokarkivProxyTilknyttSakerKalt(journalpostIdFraJson, newSaksnummer, "\"journalfoerendeEnhet\":\"4806\""),
        () -> stubs.verifyStub.dokarkivProxyTilknyttSakerIkkeKalt(journalpostIdFraJson, existingSaksnummer)
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
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, ""),
        () -> stubs.verifyStub.dokarkivFerdigstillIkkeKalt(journalpostIdFraJson)
    );
  }

  @Test
  @DisplayName("skal distribuere journalpost")
  void skalDistribuereJournalpost() throws IOException, JSONException {
    // given
    var xEnhet = "1234";
    var bestillingId = "TEST_BEST_ID";
    var journalpostIdFraJson = 201028011L;
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);

    stubs.mockSafResponseHentJournalpost("journalpostSafUtgaaendeResponse.json", HttpStatus.OK);
    stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId);
    stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson);

    var distribuerTilAdresse = createDistribuerTilAdresse();
    distribuerTilAdresse.setAdresselinje2("Adresselinje2");
    distribuerTilAdresse.setAdresselinje3("Adresselinje3");
    var request = new DistribuerJournalpostRequest(distribuerTilAdresse);

    // when
    var oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/distribuer/JOARK-"+journalpostIdFraJson,
        HttpMethod.POST,
        new HttpEntity<>(request, headersMedEnhet),
        JournalpostDto.class
    );

    // then
    assertAll(
        () -> assertThat(oppdaterJournalpostResponseEntity)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.OK),
        () -> stubs.verifyStub.dokdistFordelingKalt(objectMapper.writeValueAsString(new DokDistDistribuerJournalpostRequest(journalpostIdFraJson, "BI01A01", request.getAdresse()))),
        () -> stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name()),
        () -> stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name()),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson,  request.getAdresse().getAdresselinje1(),  request.getAdresse().getLand()),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson,  "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}")
    );
  }

  @Test
  @DisplayName("skal ikke distribuere journalpost hvis oppdater journalpost")
  void skalIkkeDistribuereJournalpostHvisOppdaterJournalpostFeiler() throws IOException, JSONException {
    // given
    var xEnhet = "1234";
    var bestillingId = "TEST_BEST_ID";
    var journalpostIdFraJson = 201028011L;
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);

    stubs.mockSafResponseHentJournalpost("journalpostSafUtgaaendeResponse.json", HttpStatus.OK);
    stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId);
    stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson, HttpStatus.INTERNAL_SERVER_ERROR);

    var distribuerTilAdresse = createDistribuerTilAdresse();
    distribuerTilAdresse.setAdresselinje2("Adresselinje2");
    distribuerTilAdresse.setAdresselinje3("Adresselinje3");
    var request = new DistribuerJournalpostRequest(distribuerTilAdresse);

    // when
    var oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/distribuer/JOARK-"+journalpostIdFraJson,
        HttpMethod.POST,
        new HttpEntity<>(request, headersMedEnhet),
        JournalpostDto.class
    );

    // then
    assertAll(
        () -> assertThat(oppdaterJournalpostResponseEntity)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
        () -> stubs.verifyStub.dokdistFordelingIkkeKalt()
    );
  }

  @Test
  @DisplayName("skal feile med BAD REQUEST når ugyldig journalpost distribuert")
  void skalFeileMedBadRequestNaarUgyldigJournalpostDistribuert() throws IOException, JSONException {
    // given
    var xEnhet = "1234";
    var bestillingId = "TEST_BEST_ID";
    var journalpostIdFraJson = 201028011L;
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);

    stubs.mockSafResponseHentJournalpost("journalpostSafUtgaaendeResponseNoMottaker.json", HttpStatus.OK);
    stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId);

    var request = new DistribuerJournalpostRequest(createDistribuerTilAdresse());

    // when
    var oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/distribuer/JOARK-"+journalpostIdFraJson,
        HttpMethod.POST,
        new HttpEntity<>(request, headersMedEnhet),
        JournalpostDto.class
    );

    // then
    assertAll(
        () -> assertThat(oppdaterJournalpostResponseEntity)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.BAD_REQUEST),
        () -> stubs.verifyStub.dokdistFordelingIkkeKalt()
    );
  }

  private EndreJournalpostCommand createEndreJournalpostCommand() {
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
