package no.nav.bidrag.dokument.arkiv.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv;
import no.nav.bidrag.dokument.arkiv.consumer.RestTemplateFactory;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

@ActiveProfiles("dev")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = BidragDokumentArkiv.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("JournalpostController")
class JournalpostControllerTest {

  @LocalServerPort
  private int port;
  @Mock
  private RestTemplate restTemplateMock;
  @Value("${server.servlet.context-path}")
  private String contextPath;
  @Autowired
  private TestRestTemplate testRestTemplate;

  @BeforeEach
  void mockRestTemplateFactory() {
    MockitoAnnotations.initMocks(this);
    RestTemplateFactory.use(() -> restTemplateMock);
  }

  @Test
  @DisplayName("should map context path with random port")
  void shouldMapToContextPath() {
    assertThat(initUrl()).isEqualTo("http://localhost:" + port + "/bidrag-dokument-arkiv");
  }

  @Test
  @DisplayName("skal ha body som null når journalpost ikke finnes")
  void skalGiBodySomNullNarJournalpostIkkeFinnes() {
    when(restTemplateMock.getForEntity(anyString(), eq(GetJournalpostResponse.class))).thenReturn(new ResponseEntity<>(HttpStatus.I_AM_A_TEAPOT));

    ResponseEntity<JournalpostDto> journalpostResponseEntity = testRestTemplate.getForEntity(initUrl() + "/journalpost/1", JournalpostDto.class);

    verify(restTemplateMock).getForEntity(eq("/journalposter/1"), eq(GetJournalpostResponse.class));

    assertThat(Optional.of(journalpostResponseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getBody()).isNull(),
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT)
    ));
  }

  @Test
  @DisplayName("skal hente Journalpost når den eksisterer")
  void skalHenteJournalpostNarDenEksisterer() {
    when(restTemplateMock.getForEntity(anyString(), eq(GetJournalpostResponse.class))).thenReturn(new ResponseEntity<>(
        enGetJournalpostResponseMedTittel("bidrag"), HttpStatus.I_AM_A_TEAPOT
    ));

    ResponseEntity<JournalpostDto> responseEntity = testRestTemplate.getForEntity(initUrl() + "/journalpost/1", JournalpostDto.class);

    verify(restTemplateMock).getForEntity(eq("/journalposter/1"), eq(GetJournalpostResponse.class));

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(response.getBody()).extracting(JournalpostDto::getInnhold).isEqualTo("bidrag")
    ));
  }

  private GetJournalpostResponse enGetJournalpostResponseMedTittel(@SuppressWarnings("SameParameterValue") String tittel) {
    GetJournalpostResponse getJournalpostResponse = new GetJournalpostResponse();
    getJournalpostResponse.setTittel(tittel);

    return getJournalpostResponse;
  }

  private String initUrl() {
    return "http://localhost:" + port + contextPath;
  }
}
