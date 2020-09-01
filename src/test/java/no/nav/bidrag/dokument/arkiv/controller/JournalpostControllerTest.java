package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQueryResponse;
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.dto.EndreDokument;
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.bidrag.dokument.dto.JournalpostResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@DisplayName("JournalpostController")
@SpringBootTest(
    classes = {BidragDokumentArkivLocal.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class JournalpostControllerTest {

  @LocalServerPort
  private int port;
  @MockBean
  @Qualifier("saf")
  private HttpHeaderRestTemplate restTemplateSafMock;
  @MockBean
  @Qualifier("dokarkiv")
  private HttpHeaderRestTemplate restTemplateDokarkivMock;
  @Value("classpath:json/dokumentoversiktFagsakQueryResponse.json")
  private Resource responseJsonResource;
  @Value("${server.servlet.context-path}")
  private String contextPath;
  @Autowired
  private HttpHeaderTestRestTemplate httpHeaderTestRestTemplate;
  @Autowired
  private ObjectMapper objectMapper;

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
  @DisplayName("skal ha body som null når journalpost ikke finnes")
  void skalGiBodySomNullNarJournalpostIkkeFinnes() {
    when(restTemplateSafMock.exchange(eq("/"), eq(HttpMethod.POST), any(), eq(DokumentoversiktFagsakQueryResponse.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

    var journalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-1?saksnummer=007",
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    assertThat(Optional.of(journalpostResponseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getBody()).isNull(),
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
        () -> verify(restTemplateSafMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(DokumentoversiktFagsakQueryResponse.class))
    ));
  }

  @Test
  @DisplayName("skal få 400 BAD REQUEST når eksisterende journalpost er knyttet til annen sak")
  void skalFaBadRequestNarEksisterendeJournalpostErKnyttetTilAnnenSak() throws IOException {
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakQueryResponse = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakQueryResponse.class);
    var journalpostIdFraJson = 201028011;

    when(restTemplateSafMock.exchange(eq("/"), eq(HttpMethod.POST), any(), eq(DokumentoversiktFagsakQueryResponse.class)))
        .thenReturn(new ResponseEntity<>(dokumentoversiktFagsakQueryResponse, HttpStatus.I_AM_A_TEAPOT));

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "?saksnummer=007",
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(response.getBody()).isNull(),
        () -> verify(restTemplateSafMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(DokumentoversiktFagsakQueryResponse.class))
    ));
  }

  @Test
  @DisplayName("skal hente Journalpost når den eksisterer")
  void skalHenteJournalpostNarDenEksisterer() throws IOException {
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakQueryResponse = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakQueryResponse.class);
    var journalpostIdFraJson = 201028011;

    when(restTemplateSafMock.exchange(eq("/"), eq(HttpMethod.POST), any(), eq(DokumentoversiktFagsakQueryResponse.class))).thenReturn(
        new ResponseEntity<>(dokumentoversiktFagsakQueryResponse, HttpStatus.I_AM_A_TEAPOT));

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "?saksnummer=5276661",
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    var journalpost = responseEntity.getBody() != null ? responseEntity.getBody().getJournalpost() : null;

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getInnhold).isEqualTo("Filosofens bidrag"),
        () -> assertThat(journalpost).isNotNull().extracting(JournalpostDto::getJournalpostId).isEqualTo("JOARK-" + journalpostIdFraJson),
        () -> verify(restTemplateSafMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(DokumentoversiktFagsakQueryResponse.class))
    ));
  }

  @Test
  @DisplayName("skal hente journalposter for en bidragssak")
  void skalHenteJournalposterForEnBidragssak() throws IOException {
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakQueryResponse = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakQueryResponse.class);

    when(restTemplateSafMock.exchange(eq("/"), eq(HttpMethod.POST), any(), eq(DokumentoversiktFagsakQueryResponse.class)))
        .thenReturn(new ResponseEntity<>(dokumentoversiktFagsakQueryResponse, HttpStatus.I_AM_A_TEAPOT));

    var jouralposterResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/sak/5276661/journal?fagomrade=BID", HttpMethod.GET, null, listeMedJournalposterTypeReference()
    );

    assertAll(
        () -> assertThat(jouralposterResponseEntity).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.OK),
        () -> assertThat(jouralposterResponseEntity.getBody()).hasSize(3)
    );
  }

  private ParameterizedTypeReference<List<JournalpostDto>> listeMedJournalposterTypeReference() {
    return new ParameterizedTypeReference<>() {
    };
  }

  @Test
  @DisplayName("skal endre journalpost")
  void skalEndreJournalpost() throws IOException {
    // given
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakQueryResponse = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakQueryResponse.class);
    var journalpostIdFraJson = 201028011;
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, "1234");

    var endreJournalpostCommand = new EndreJournalpostCommand();
    endreJournalpostCommand.setAvsenderNavn("Dauden, Svarte");
    endreJournalpostCommand.setGjelder("06127412345");
    endreJournalpostCommand.setTittel("So Tired");
    endreJournalpostCommand.setEndreDokumenter(List.of(
        new EndreDokument("BLABLA", 1, "In a galazy far far away")
    ));

    when(restTemplateSafMock.exchange(eq("/"), eq(HttpMethod.POST), any(HttpEntity.class), eq(DokumentoversiktFagsakQueryResponse.class)))
        .thenReturn(new ResponseEntity<>(dokumentoversiktFagsakQueryResponse, HttpStatus.I_AM_A_TEAPOT));

    when(restTemplateDokarkivMock.exchange(
        eq("/rest/journalpostapi/v1/journalpost/" + journalpostIdFraJson),
        eq(HttpMethod.PUT),
        any(HttpEntity.class),
        eq(OppdaterJournalpostResponse.class)
    )).thenReturn(new ResponseEntity<>(new OppdaterJournalpostResponse(journalpostIdFraJson, null), HttpStatus.ACCEPTED));

    // when
    var oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.PUT,
        new HttpEntity<>(endreJournalpostCommand, headersMedEnhet),
        JournalpostDto.class
    );

    // then
    assertAll(
        () -> assertThat(oppdaterJournalpostResponseEntity)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.ACCEPTED),
        () -> {
          var forventetUrlForOppdateringAvJournalpost = "/rest/journalpostapi/v1/journalpost/" + journalpostIdFraJson;
          var oppdaterJournalpostRequest = new OppdaterJournalpostRequest(
              journalpostIdFraJson,
              new EndreJournalpostCommandIntern(endreJournalpostCommand, "1234"),
              dokumentoversiktFagsakQueryResponse.hentJournalpost(journalpostIdFraJson)
          );

          verify(restTemplateDokarkivMock).exchange(
              eq(forventetUrlForOppdateringAvJournalpost),
              eq(HttpMethod.PUT),
              eq(new HttpEntity<>(oppdaterJournalpostRequest.tilJournalpostApi())),
              eq(OppdaterJournalpostResponse.class)
          );
        }
    );
  }

  @Test
  @DisplayName("skal videresende eventuelle headere fra kall mot GraphQuery")
  void skalReturnereHeadereFraSaf() throws IOException {
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakQueryResponse = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakQueryResponse.class);
    var httpHeader = new HttpHeaders();
    httpHeader.add(HttpHeaders.CONTENT_LANGUAGE, "røverspråk");

    when(restTemplateSafMock.exchange(eq("/"), eq(HttpMethod.POST), any(), eq(DokumentoversiktFagsakQueryResponse.class)))
        .thenReturn(new ResponseEntity<>(dokumentoversiktFagsakQueryResponse, httpHeader, HttpStatus.I_AM_A_TEAPOT));

    var jouralposterResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/sak/5276661/journal?fagomrade=BID", HttpMethod.GET, null, listeMedJournalposterTypeReference()
    );

    var contentLanguage = jouralposterResponseEntity.getHeaders().getFirst(HttpHeaders.CONTENT_LANGUAGE);

    assertAll(
        () -> assertThat(jouralposterResponseEntity).extracting(ResponseEntity::getStatusCode).as("status").isEqualTo(HttpStatus.OK),
        () -> assertThat(contentLanguage).as("language").isEqualTo("røverspråk")
    );
  }

  private String initUrl() {
    return "http://localhost:" + port + contextPath;
  }
}
