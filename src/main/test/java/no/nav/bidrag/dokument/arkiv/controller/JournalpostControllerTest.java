package no.nav.bidrag.dokument.arkiv.controller;

import no.nav.bidrag.dokument.arkiv.consumer.RestTemplateFactory;
import no.nav.bidrag.dokument.arkiv.dto.JournalforingDto;
import no.nav.bidrag.dokument.dto.JournalpostDto;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class) @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("JournalpostController") class JournalpostControllerTest {

    @LocalServerPort private int port;
    @Mock private RestTemplate restTemplateMock;
    @Value("${server.servlet.context-path}") private String contextPath;
    @Autowired private TestRestTemplate testRestTemplate;

    @BeforeEach void mockRestTemplateFactory() {
        MockitoAnnotations.initMocks(this);
        RestTemplateFactory.use(() -> restTemplateMock);
    }

    @DisplayName("should map context path with random port")
    @Test void shouldMapToContextPath() {
        assertThat(initUrl()).isEqualTo("http://localhost:" + port + "/bidrag-dokument-arkiv");
    }

    @DisplayName("skal ha body som null når journalpost ikke finnes")
    @Test void skalGiBodySomNullNarJournalpostIkkeFinnes() {
        when(restTemplateMock.getForEntity(eq("/rest/journalfoerinngaaende/v1/journalposter/1"), eq(JournalforingDto.class))).thenReturn(new ResponseEntity<>(HttpStatus.I_AM_A_TEAPOT));

        ResponseEntity<JournalpostDto> journalpostResponseEntity = testRestTemplate.getForEntity(initUrl() + "/journalpost/1", JournalpostDto.class);

        verify(restTemplateMock).getForEntity(eq("/rest/journalfoerinngaaende/v1/journalposter/1"), eq(JournalforingDto.class));

        assertThat(Optional.of(journalpostResponseEntity)).hasValueSatisfying(response -> assertAll(
                () -> assertThat(response.getBody()).isNull(),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT)
        ));
    }

    @DisplayName("skal hente Journalpost når den eksisterer")
    @Test void skalHenteJournalpostNarDenEksisterer() {
        when(restTemplateMock.getForEntity(eq("/rest/journalfoerinngaaende/v1/journalposter/1"), eq(JournalforingDto.class))).thenReturn(new ResponseEntity<>(
                enJournalforingMedTilstand("MIDLERTIDIG"), HttpStatus.I_AM_A_TEAPOT
        ));

        ResponseEntity<JournalpostDto> responseEntity = testRestTemplate.getForEntity(initUrl() + "/journalpost/1", JournalpostDto.class);

        verify(restTemplateMock).getForEntity(eq("/rest/journalfoerinngaaende/v1/journalposter/1"), eq(JournalforingDto.class));

        assertThat(Optional.of(responseEntity)).hasValueSatisfying(response -> assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).extracting(JournalpostDto::getHello).isEqualTo("hello from bidrag-dokument"),
                () -> assertThat(response.getBody()).extracting(JournalpostDto::getJournaltilstand).isEqualTo("MIDLERTIDIG")
        ));
    }

    private JournalforingDto enJournalforingMedTilstand(@SuppressWarnings("SameParameterValue") String journaltilstand) {
        JournalforingDto journalforingDto = new JournalforingDto();
        journalforingDto.setJournalTilstand(journaltilstand);

        return journalforingDto;
    }

    private String initUrl() {
        return "http://localhost:" + port + contextPath;
    }
}
