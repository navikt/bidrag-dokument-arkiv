package no.nav.bidrag.dokument.arkiv.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.junit.jupiter.api.Disabled;
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
import org.springframework.web.client.RestTemplate;

@ActiveProfiles("dev")
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
  private HttpHeaderTestRestTemplate httpHeaderTestRestTemplate;

  @Test
  @DisplayName("should map context path with random port")
  void shouldMapToContextPath() {
    assertThat(initUrl()).isEqualTo("http://localhost:" + port + "/bidrag-dokument-arkiv");
  }

  @Test
  @DisplayName("skal ha body som null når journalpost ikke finnes")
  void skalGiBodySomNullNarJournalpostIkkeFinnes() {
    when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
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

    verify(restTemplateMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(Map.class));
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

    when(restTemplateMock.exchange(eq("/"), eq(HttpMethod.POST), any(), eq(Map.class)))
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

    verify(restTemplateMock).exchange(eq("/"), eq(HttpMethod.POST), any(), eq(Map.class));
  }

  @Test
  @Disabled("wip")
  @DisplayName("skal hente journalposter for en bidragssak")
  void skalHenteJournalposterForEnBidragssak() {
    when(restTemplateMock.exchange(eq("/"), eq(HttpMethod.POST), any(), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(enMapMedJournalposter(), HttpStatus.I_AM_A_TEAPOT));

    var jouralposterResponseEntity = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/sakjournal/1234567?fagomrade=BID", HttpMethod.GET, null, listeMedJournalposterTypeReference()
    );

    assertAll(
        () -> assertThat(jouralposterResponseEntity).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.OK),
        () -> assertThat(jouralposterResponseEntity.getBody()).hasSize(3)
    );
  }

  private Map<String, Object> enMapMedJournalposter() {
    return Collections.emptyMap();
  }

  private ParameterizedTypeReference<List<JournalpostDto>> listeMedJournalposterTypeReference() {
    return new ParameterizedTypeReference<>() {
    };
  }

  private String initUrl() {
    return "http://localhost:" + port + contextPath;
  }
}
