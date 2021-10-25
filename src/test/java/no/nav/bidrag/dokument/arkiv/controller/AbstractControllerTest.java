package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer.ARBEIDSFORDELING_URL;
import static no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer.SAKSBEHANDLER_INFO;
import static no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer.URL_JOURNALPOSTAPI_V1;
import static no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.dto.GeografiskTilknytningResponse;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@DisplayName("JournalpostController")
@SpringBootTest(
    classes = {BidragDokumentArkivLocal.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public abstract class AbstractControllerTest {
  @LocalServerPort
  protected int port;
  @MockBean
  @Qualifier("base")
  protected HttpHeaderRestTemplate baseRestemplateMock;
  @MockBean
  @Qualifier("serviceuser")
  protected HttpHeaderRestTemplate serviceUserResttemapletMock;
  @Value("classpath:json/dokumentoversiktFagsakQueryResponse.json")
  protected Resource responseOversiktFagsakJsonResource;
  @Value("classpath:json/journalpostSafResponse.json")
  protected Resource responseJournalpostJsonResource;
  @Value("classpath:json/journalpostSafNotFoundResponse.json")
  protected Resource responseJournalpostNotFoundJsonResource;
  @Value("${server.servlet.context-path}")
  protected String contextPath;
  @Autowired
  protected HttpHeaderTestRestTemplate httpHeaderTestRestTemplate;
  @Autowired
  protected ObjectMapper objectMapper;
  @MockBean
  protected KafkaTemplate<String, String> kafkaTemplateMock;
  @Value("${TOPIC_JOURNALPOST}")
  protected String topicJournalpost;

  protected String PERSON_IDENT = "12345678910";
  protected String AKTOR_IDENT = "92345678910";

  protected void mockBidragOrganisasjonSaksbehandler(){
    when(serviceUserResttemapletMock.exchange(
        matches("saksbehandler/info/*"),
        eq(HttpMethod.GET),
        eq(null),
        eq(SaksbehandlerInfoResponse.class)
    )).thenReturn(new ResponseEntity<>(new SaksbehandlerInfoResponse("ident", "navn"), HttpStatus.OK));
  }

  protected void mockBidragOrganisasjonGeografisk(String enhet){
    when(serviceUserResttemapletMock.exchange(
        matches("/arbeidsfordeling/enhetsliste/geografisktilknytning/*"),
        eq(HttpMethod.GET),
        eq(null),
        eq(GeografiskTilknytningResponse.class)
    )).thenReturn(new ResponseEntity<>(new GeografiskTilknytningResponse(enhet, "navn"), HttpStatus.OK));
  }

  protected void mockDokarkivOppdaterRequest(Long journalpostId){
     mockDokarkivOppdaterRequest(journalpostId, HttpStatus.OK);
  }

  protected void mockDokarkivOppdaterRequest(Long journalpostId, HttpStatus status){
    when(baseRestemplateMock.exchange(
        eq(URL_JOURNALPOSTAPI_V1 + '/' + journalpostId),
        eq(HttpMethod.PUT),
        any(HttpEntity.class),
        eq(OppdaterJournalpostResponse.class)
    )).thenReturn(new ResponseEntity<>(new OppdaterJournalpostResponse(journalpostId), status));
  }

  protected void mockDokarkivFeilregistrerRequest(String path, Long journalpostId){
    when(baseRestemplateMock.exchange(
        eq(String.format(URL_JOURNALPOSTAPI_V1_FEILREGISTRER, journalpostId)+"/"+path),
        eq(HttpMethod.PATCH),
        eq(null),
        eq(Void.class)
    )).thenReturn(new ResponseEntity<>(HttpStatus.OK));
  }

  protected void mockDokarkivFerdigstillRequest(Long journalpostId){
    when(baseRestemplateMock.exchange(
        eq(URL_JOURNALPOSTAPI_V1 + '/' + journalpostId + "/ferdigstill"),
        eq(HttpMethod.PATCH),
        any(HttpEntity.class),
        eq(Void.class)
    )).thenReturn(new ResponseEntity<>(HttpStatus.OK));
  }

  protected void mockSafResponse(Resource resource, HttpStatus status) throws IOException {
    var jsonResponse = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(resource.getFile().toURI()))));
    mockSafResponse(jsonResponse, status);
  }

  protected void mockSafResponse(String response, HttpStatus status){
    when(baseRestemplateMock.exchange(eq("/"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, status));
  }
  protected void mockPersonResponse(PersonResponse personResponse, HttpStatus status){
    when(baseRestemplateMock.exchange(matches("/informasjon/*"), eq(HttpMethod.GET), eq(null), eq(PersonResponse.class)))
        .thenReturn(new ResponseEntity<>(personResponse, status));
  }

  protected String initUrl() {
    return "http://localhost:" + port + contextPath;
  }

}
