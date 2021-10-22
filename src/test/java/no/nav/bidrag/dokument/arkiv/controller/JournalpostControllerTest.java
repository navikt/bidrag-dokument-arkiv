package no.nav.bidrag.dokument.arkiv.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.LagreJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.dto.AktorDto;
import no.nav.bidrag.dokument.dto.EndreDokument;
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.bidrag.dokument.dto.JournalpostResponse;
import org.json.JSONException;
import org.json.JSONObject;
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
    mockSafResponse(responseJournalpostNotFoundJsonResource, HttpStatus.OK);
    mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);

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
        () -> verify(baseRestemplateMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(String.class))
    ));
  }

  @Test
  @DisplayName("skal få 400 BAD REQUEST når eksisterende journalpost er knyttet til annen sak")
  void skalFaBadRequestNarEksisterendeJournalpostErKnyttetTilAnnenSak() throws IOException {
    var journalpostIdFraJson = 201028011;

    mockSafResponse(responseJournalpostJsonResource, HttpStatus.OK);
    mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "?saksnummer=007",
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(response.getBody()).isNull(),
        () -> verify(baseRestemplateMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(String.class))
    ));
  }

  @Test
  @DisplayName("skal få 500 INTERNAL SERVER når person api feiler")
  void skalFaServerFeilNarPersonApietFeiler() throws IOException {
    var journalpostIdFraJson = 201028011;

    mockSafResponse(responseJournalpostJsonResource, HttpStatus.OK);
    mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.BAD_REQUEST);

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "?saksnummer=5276661",
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
        () -> assertThat(response.getBody()).isNull(),
        () -> verify(baseRestemplateMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(String.class))
    ));
  }

  @Test
  @DisplayName("skal hente Journalpost når den eksisterer")
  void skalHenteJournalpostNarDenEksisterer() throws IOException {
    var journalpostIdFraJson = 201028011;

    mockSafResponse(responseJournalpostJsonResource, HttpStatus.OK);
    mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "?saksnummer=5276661",
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    var journalpost = responseEntity.getBody() != null ? responseEntity.getBody().getJournalpost() : null;

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getInnhold).isEqualTo("Filosofens bidrag"),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getJournalpostId).isEqualTo("JOARK-" + journalpostIdFraJson),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getGjelderAktor).extracting(AktorDto::getIdent).isEqualTo(PERSON_IDENT),
        () -> verify(baseRestemplateMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(String.class)),
        () -> verify(baseRestemplateMock).exchange(matches("/informasjon/*"), eq(HttpMethod.GET), any(), eq(PersonResponse.class))
    ));
  }

  @Test
  @DisplayName("skal hente journalposter for en bidragssak")
  void skalHenteJournalposterForEnBidragssak() throws IOException {
    mockSafResponse(responseOversiktFagsakJsonResource, HttpStatus.OK);
    mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);

    var jouralposterResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/sak/5276661/journal?fagomrade=BID", HttpMethod.GET, null, listeMedJournalposterTypeReference()
    );

    assertAll(
        () -> assertThat(jouralposterResponseEntity).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.OK),
        () -> assertThat(jouralposterResponseEntity.getBody()).hasSize(3),
        () -> verify(baseRestemplateMock, times(3)).exchange(matches("/informasjon/*"), eq(HttpMethod.GET), eq(null), eq(PersonResponse.class)),
        () -> verify(baseRestemplateMock, times(1)).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(String.class))
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
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJournalpostJsonResource.getFile().toURI()))));
    var jsonObject = new JSONObject(jsonResponse);
    var journalpostSafResponse = objectMapper.readValue(jsonObject.getJSONObject("data").getString("journalpost"), Journalpost.class);
    var journalpostIdFraJson = 201028011L;
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);

    var endreJournalpostCommand = createEndreJournalpostCommand();
    endreJournalpostCommand.setSkalJournalfores(true);

    mockSafResponse(jsonResponse, HttpStatus.OK);
    mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);
    mockDokarkivOppdaterRequest(journalpostIdFraJson);
    mockDokarkivFerdigstillRequest(journalpostIdFraJson);

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
        () -> {
          var forventetUrlForOppdateringAvJournalpost = "/rest/journalpostapi/v1/journalpost/" + journalpostIdFraJson;
          var oppdaterJournalpostRequest = new LagreJournalpostRequest(
              journalpostIdFraJson,
              new EndreJournalpostCommandIntern(endreJournalpostCommand, xEnhet),
              journalpostSafResponse
          );

          verify(baseRestemplateMock).exchange(
              eq(forventetUrlForOppdateringAvJournalpost),
              eq(HttpMethod.PUT),
              eq(new HttpEntity<>(oppdaterJournalpostRequest)),
              eq(OppdaterJournalpostResponse.class)
          );
        },
        ()->{
          var ferdigstillJournalpostUrl = "/rest/journalpostapi/v1/journalpost/" + journalpostIdFraJson + "/ferdigstill";
          var ferdigstillJournalpostRequest = new FerdigstillJournalpostRequest(journalpostIdFraJson, xEnhet);
          verify(baseRestemplateMock).exchange(
              eq(ferdigstillJournalpostUrl),
              eq(HttpMethod.PATCH),
              eq(new HttpEntity<>(ferdigstillJournalpostRequest)),
              eq(Void.class)
          );
        }
    );
  }

  @Test
  @DisplayName("skal endre journalpost")
  void skalEndreJournalpost() throws IOException, JSONException {
    // given
    var xEnhet = "1234";
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJournalpostJsonResource.getFile().toURI()))));
    var jsonObject = new JSONObject(jsonResponse);
    var journalpostSafResponse = objectMapper.readValue(jsonObject.getJSONObject("data").getString("journalpost"), Journalpost.class);
    var journalpostIdFraJson = 201028011L;
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);

    var endreJournalpostCommand = createEndreJournalpostCommand();

    mockSafResponse(jsonResponse, HttpStatus.OK);
    mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);
    mockDokarkivOppdaterRequest(journalpostIdFraJson);
    mockDokarkivFerdigstillRequest(journalpostIdFraJson);
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
        () -> {
          var forventetUrlForOppdateringAvJournalpost = "/rest/journalpostapi/v1/journalpost/" + journalpostIdFraJson;
          var oppdaterJournalpostRequest = new LagreJournalpostRequest(
              journalpostIdFraJson,
              new EndreJournalpostCommandIntern(endreJournalpostCommand, xEnhet),
              journalpostSafResponse
          );

          verify(baseRestemplateMock).exchange(
              eq(forventetUrlForOppdateringAvJournalpost),
              eq(HttpMethod.PUT),
              eq(new HttpEntity<>(oppdaterJournalpostRequest)),
              eq(OppdaterJournalpostResponse.class)
          );
        },
        ()->{
          var ferdigstillJournalpostUrl = "/rest/journalpostapi/v1/journalpost/" + journalpostIdFraJson + "/ferdigstill";
          var ferdigstillJournalpostRequest = new FerdigstillJournalpostRequest(journalpostIdFraJson, xEnhet);
          verify(baseRestemplateMock, never()).exchange(
              eq(ferdigstillJournalpostUrl),
              eq(HttpMethod.PATCH),
              eq(new HttpEntity<>(ferdigstillJournalpostRequest)),
              eq(Void.class)
          );
        }
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
