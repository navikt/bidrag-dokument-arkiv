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
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQueryResponse;
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
  @DisplayName("skal ha 404 NOT FOUND når prefix mangler")
  void skalHaNotFoundlNarPrefixPaJournalpostIdMangler() {
    var journalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/sak/007/journal/1",
        HttpMethod.GET,
        null,
        JournalpostDto.class
    );

    assertThat(journalpostResponseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("skal ha 404 NOT FOUND når prefix er feil")
  void skalHaNotFoundlNarPrefixPaJournalpostIdErFeil() {
    var journalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/sak/007/journal/BID-1",
        HttpMethod.GET,
        null,
        JournalpostDto.class
    );

    assertThat(journalpostResponseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("skal ha body som null når journalpost ikke finnes")
  void skalGiBodySomNullNarJournalpostIkkeFinnes() {

    var journalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/sak/007/journal/JOARK-1",
        HttpMethod.GET,
        null,
        JournalpostDto.class
    );

    assertThat(Optional.of(journalpostResponseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getBody()).isNull(),
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT),
        () -> verify(restTemplateSafMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(DokumentoversiktFagsakQueryResponse.class))
    ));
  }

  @Test
  @DisplayName("skal få 404 NOT FOUND når eksisterende journalpost er knyttet til annen sak")
  void skalFaNotFoundNarEksisterendeJournalpostErKnyttetTilAnnenSak() throws IOException {
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.getFile().toURI()))));
    var dokumentoversiktFagsakQueryResponse = objectMapper.readValue(jsonResponse, DokumentoversiktFagsakQueryResponse.class);
    var journalpostIdFraJson = 201028011;

    when(restTemplateSafMock.exchange(eq("/"), eq(HttpMethod.POST), any(), eq(DokumentoversiktFagsakQueryResponse.class)))
        .thenReturn(new ResponseEntity<>(dokumentoversiktFagsakQueryResponse, HttpStatus.I_AM_A_TEAPOT));

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/sak/007/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.GET,
        null,
        JournalpostDto.class
    );

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
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
        initUrl() + "/sak/5276661/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.GET,
        null,
        JournalpostResponse.class
    );

    var journalpost = responseEntity.getBody() != null ? responseEntity.getBody().getJournalpost() : null;

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
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
    var saksnummerFraJson = "5276661";

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
        initUrl() + "/sak/" + saksnummerFraJson + "/journal/JOARK-" + journalpostIdFraJson,
        HttpMethod.PUT,
        new HttpEntity<>(endreJournalpostCommand),
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
              journalpostIdFraJson, endreJournalpostCommand, dokumentoversiktFagsakQueryResponse.hentJournalpost(journalpostIdFraJson)
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

  private String initUrl() {
    return "http://localhost:" + port + contextPath;
  }
}
