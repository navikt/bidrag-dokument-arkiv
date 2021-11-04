package no.nav.bidrag.dokument.arkiv.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.dto.AvvikType;
import no.nav.bidrag.dokument.dto.Avvikshendelse;
import no.nav.bidrag.dokument.dto.BehandleAvvikshendelseResponse;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AvvikControllerTest extends AbstractControllerTest {

  @Test
  @DisplayName("skal utføre avvik OVERFOR_TIL_ANNEN_ENHET")
  void skalSendeAvvikOverforEnhet() throws IOException, JSONException {
    // given
    var xEnhet = "1234";
    var overforTilEnhet = "4833";
    var journalpostIdFraJson = 201028011L;
    var avvikHendelse = createAvvikHendelse(AvvikType.OVERFOR_TIL_ANNEN_ENHET, Map.of("nyttEnhetsnummer", overforTilEnhet));

    stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);
    stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson);
    stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson);

    // when
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);
    var overforEnhetResponse = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "/avvik",
        HttpMethod.POST,
        new HttpEntity<>(avvikHendelse, headersMedEnhet),
        BehandleAvvikshendelseResponse.class
    );

    // then
    assertAll(
        () -> assertThat(overforEnhetResponse)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.OK),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, String.format("\"journalfoerendeEnhet\":\"%s\"", overforTilEnhet)),
        () -> verify(kafkaTemplateMock).send(eq(topicJournalpost), eq("JOARK-" + journalpostIdFraJson), any())
    );
  }

  @Test
  @DisplayName("skal ikke sende journalpostHendelse når avvik OVER_TIL_ANNEN_ENHET feiler")
  void shouldNotSendeKafkaMessageWhenAvvikFails() throws IOException, JSONException {
    // given
    var xEnhet = "1234";
    var overforTilEnhet = "4833";
    var journalpostIdFraJson = 201028011L;
    var avvikHendelse = createAvvikHendelse(AvvikType.OVERFOR_TIL_ANNEN_ENHET, Map.of("nyttEnhetsnummer", overforTilEnhet));

    stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);
    stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson, HttpStatus.BAD_REQUEST);

    // when
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);
    var overforEnhetResponse = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "/avvik",
        HttpMethod.POST,
        new HttpEntity<>(avvikHendelse, headersMedEnhet),
        BehandleAvvikshendelseResponse.class
    );

    // then
    assertAll(
        () -> assertThat(overforEnhetResponse)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.BAD_REQUEST),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, String.format("\"journalfoerendeEnhet\":\"%s\"", overforTilEnhet)),
        () -> verify(kafkaTemplateMock, never()).send(any(), any(), any())
    );
  }

  @Test
  @DisplayName("skal utføre avvik ENDRE_FAGOMRADE")
  void skalSendeAvvikEndreFagomrade() throws IOException, JSONException {
    // given
    var xEnhet = "1234";
    var nyttFagomrade = "FAR";
    var journalpostIdFraJson = 201028011L;
    var avvikHendelse = createAvvikHendelse(AvvikType.ENDRE_FAGOMRADE, Map.of("fagomrade", nyttFagomrade));

    stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);
    stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson);

    // when
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);
    var overforEnhetResponse = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "/avvik",
        HttpMethod.POST,
        new HttpEntity<>(avvikHendelse, headersMedEnhet),
        BehandleAvvikshendelseResponse.class
    );

    // then
    assertAll(
        () -> assertThat(overforEnhetResponse)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.OK),
        () -> stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, String.format("\"tema\":\"%s\"", nyttFagomrade)),
        () -> verify(kafkaTemplateMock).send(eq(topicJournalpost), eq("JOARK-" + journalpostIdFraJson), any())
    );
  }

  @Test
  @DisplayName("skal utføre avvik FEILFORE_SAK")
  void skalSendeAvvikFeilfor() throws IOException {
    // given
    var xEnhet = "1234";
    var journalpostIdFraJson = 201028011L;
    var avvikHendelse = createAvvikHendelse(AvvikType.FEILFORE_SAK, Map.of());

    stubs.mockSafResponseHentJournalpost(journalpostJournalfortSafResponse, HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);
    stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson);
    stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson);
    stubs.mockDokarkivFeilregistrerRequest("feilregistrerSakstilknytning", journalpostIdFraJson);

    // when
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);
    var overforEnhetResponse = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "/avvik",
        HttpMethod.POST,
        new HttpEntity<>(avvikHendelse, headersMedEnhet),
        BehandleAvvikshendelseResponse.class
    );

    // then
    assertAll(
        () -> assertThat(overforEnhetResponse)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.OK),
        () -> stubs.verifyStub.dokarkivFeilregistrerKalt("feilregistrerSakstilknytning", journalpostIdFraJson),
        () -> verify(kafkaTemplateMock).send(eq(topicJournalpost), eq("JOARK-" + journalpostIdFraJson), any())
    );
  }

  @Test
  @DisplayName("skal utføre avvik TREKK_JOURNALPOST")
  void skalUtforeAvvikTrekkJournalpost() throws IOException {
    // given
    var xEnhet = "1234";
    var journalpostIdFraJson = 201028011L;
    var avvikHendelse = createAvvikHendelse(AvvikType.TREKK_JOURNALPOST, Map.of());

    stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK);
    stubs.mockPersonResponse(new PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK);
    stubs.mockDokarkivFeilregistrerRequest("settStatusUtg(.*)r", journalpostIdFraJson);
    // when
    var headersMedEnhet = new HttpHeaders();
    headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet);
    var overforEnhetResponse = httpHeaderTestRestTemplate.exchange(
        initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "/avvik",
        HttpMethod.POST,
        new HttpEntity<>(avvikHendelse, headersMedEnhet),
        BehandleAvvikshendelseResponse.class
    );

    // then
    assertAll(
        () -> assertThat(overforEnhetResponse)
            .extracting(ResponseEntity::getStatusCode)
            .as("statusCode")
            .isEqualTo(HttpStatus.OK),
        () -> stubs.verifyStub.dokarkivFeilregistrerKalt("settStatusUtg(.*)r", journalpostIdFraJson),
        () -> verify(kafkaTemplateMock).send(eq(topicJournalpost), eq("JOARK-" + journalpostIdFraJson), any())
    );
  }

  private Avvikshendelse createAvvikHendelse(AvvikType avvikType, Map<String, String> detaljer) {
    var avvikHendelse = new Avvikshendelse();
    avvikHendelse.setAvvikType(avvikType.name());
    avvikHendelse.setDetaljer(detaljer);
    return avvikHendelse;
  }
}
