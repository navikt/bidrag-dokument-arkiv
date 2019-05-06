package no.nav.bidrag.dokument.arkiv.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv;
import no.nav.bidrag.dokument.arkiv.TestRestTemplateConfiguration;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@DisplayName("JournalpostController")
@SpringBootTest(
    classes = {BidragDokumentArkiv.class, TestRestTemplateConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class JournalpostControllerTest {

  private static final String JOURNALPOSTER_JSON = String.join("\n", "{",
      "  \"data\": {",
      "    \"journalposter\": [",
      "      { \"journalpostId\": \"1001001\" },",
      "      { \"journalpostId\": \"1001002\" },",
      "      { \"journalpostId\": \"1001003\" }",
      "    ]",
      "  }",
      "}"
  );

  @LocalServerPort
  private int port;
  @MockBean
  private HttpHeaderRestTemplate httpHeaderRestTemplateMock;
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
  @DisplayName("skal ha body som null når journalpost ikke finnes")
  void skalGiBodySomNullNarJournalpostIkkeFinnes() {
    when(httpHeaderRestTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.I_AM_A_TEAPOT));

    var journalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journalpost/1",
        HttpMethod.GET,
        null,
        JournalpostDto.class
    );

    assertThat(Optional.of(journalpostResponseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getBody()).isNull(),
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT)
    ));

    verify(httpHeaderRestTemplateMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(Map.class));
  }

  @Test
  @DisplayName("skal hente Journalpost når den eksisterer")
  void skalHenteJournalpostNarDenEksisterer() {
    var innhold = new HashMap<>();
    innhold.put("tittel", "bidrag");
    var journalpostMap = new HashMap<>();
    journalpostMap.put("journalpost", innhold);
    var dataMap = new HashMap<>();
    dataMap.put("data", journalpostMap);

    when(httpHeaderRestTemplateMock.exchange(eq("/"), eq(HttpMethod.POST), any(), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(dataMap, HttpStatus.I_AM_A_TEAPOT));

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journalpost/1",
        HttpMethod.GET,
        null,
        JournalpostDto.class
    );

    assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> {
          assertThat(response.getBody()).isNotNull();
          assertThat(response.getBody()).extracting(JournalpostDto::getInnhold).isEqualTo("bidrag");
        }
    ));

    verify(httpHeaderRestTemplateMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(Map.class));
  }

  @Test
  @DisplayName("skal hente journalposter for en bidragssak")
  void skalHenteJournalposterForEnBidragssak() throws IOException {
    Map journalposterMapOversattMedJackson = objectMapper.readValue(JOURNALPOSTER_JSON, HashMap.class);

    when(httpHeaderRestTemplateMock.exchange(eq("/"), eq(HttpMethod.POST), any(), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(journalposterMapOversattMedJackson, HttpStatus.I_AM_A_TEAPOT));

    var jouralposterResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/sakjournal/1234567?fagomrade=BID", HttpMethod.GET, null, listeMedJournalposterTypeReference()
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

  private String initUrl() {
    return "http://localhost:" + port + contextPath;
  }
}
