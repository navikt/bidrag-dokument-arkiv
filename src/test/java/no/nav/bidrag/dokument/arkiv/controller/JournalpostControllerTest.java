package no.nav.bidrag.dokument.arkiv.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
import no.nav.security.oidc.test.support.jersey.TestTokenGeneratorResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
  @MockBean
  private RestTemplate restTemplateMock;
  @Value("${server.servlet.context-path}")
  private String contextPath;
  @Autowired
  private TestRestTemplate testRestTemplate;

  private HttpHeaders headersWithAuthorization;

  @Test
  @DisplayName("should map context path with random port")
  void shouldMapToContextPath() {
    assertThat(initUrl()).isEqualTo("http://localhost:" + port + "/bidrag-dokument-arkiv");
  }

  @Test
  @DisplayName("skal ha body som null når journalpost ikke finnes")
  void skalGiBodySomNullNarJournalpostIkkeFinnes() {
    when(restTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(GetJournalpostResponse.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.I_AM_A_TEAPOT));

    ResponseEntity<JournalpostDto> journalpostResponseEntity = testRestTemplate.exchange(
        initUrl() + "/journalpost/1",
        HttpMethod.GET,
        createEmptyHttpEntityWithAuthorization(),
        JournalpostDto.class
    );

    assertThat(Optional.of(journalpostResponseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getBody()).isNull(),
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT)
    ));

    verify(restTemplateMock).exchange(eq("/journalposter/1"), eq(HttpMethod.GET), any(), eq(GetJournalpostResponse.class));
  }

  @Test
  @DisplayName("skal hente Journalpost når den eksisterer")
  void skalHenteJournalpostNarDenEksisterer() {
    when(restTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(GetJournalpostResponse.class)))
        .thenReturn(new ResponseEntity<>(enGetJournalpostResponseMedTittel("bidrag"), HttpStatus.I_AM_A_TEAPOT));

    ResponseEntity<JournalpostDto> responseEntity = testRestTemplate.exchange(
        initUrl() + "/journalpost/1",
        HttpMethod.GET,
        createEmptyHttpEntityWithAuthorization(),
        JournalpostDto.class
    );

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> {
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody()).extracting(JournalpostDto::getInnhold).isEqualTo("bidrag");
        }
    ));

    verify(restTemplateMock).exchange(eq("/journalposter/1"), eq(HttpMethod.GET), any(), eq(GetJournalpostResponse.class));
  }

  private <T> HttpEntity<T> createEmptyHttpEntityWithAuthorization() {
    if (headersWithAuthorization == null) {
      headersWithAuthorization = generateHeadersWithAuthorization();
    }

    return new HttpEntity<>(null, headersWithAuthorization);
  }

  private HttpHeaders generateHeadersWithAuthorization() {
    TestTokenGeneratorResource testTokenGeneratorResource = new TestTokenGeneratorResource();
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + testTokenGeneratorResource.issueToken("localhost-idtoken"));

    return httpHeaders;
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
