package no.nav.bidrag.dokument.arkiv.controller;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal.PROFILE_INTEGRATION;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.stubs.Stubs;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({PROFILE_TEST, PROFILE_INTEGRATION})
@DisplayName("JournalpostController")
@SpringBootTest(
    classes = {BidragDokumentArkivLocal.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
public abstract class AbstractControllerTest {

  protected String PERSON_IDENT = "12345678910";
  protected String AKTOR_IDENT = "92345678910";
  protected String responseJournalpostJson = "journalpostSafResponse.json";
  protected String responseJournalpostJsonUtgaaende = "journalpostSafUtgaaendeResponse.json";
  protected String responseJournalpostJsonWithReturDetaljer = "journalpostSafReturDetaljerResponse.json";
  protected String responseJournalpostIngenSakerJson = "journalpostSafIngenSakerResponse.json";
  protected String journalpostSafNotFoundResponse = "journalpostSafNotFoundResponse.json";
  protected String journalpostJournalfortSafResponse = "journalpostJournalfortSafResponse.json";
  protected Stubs stubs = new Stubs();

  @LocalServerPort
  protected int port;
  @Value("${server.servlet.context-path}")
  protected String contextPath;
  @Autowired
  protected HttpHeaderTestRestTemplate httpHeaderTestRestTemplate;
  @MockBean
  protected KafkaTemplate<String, String> kafkaTemplateMock;
  @Value("${TOPIC_JOURNALPOST}")
  protected String topicJournalpost;


  @BeforeEach
  public void initMocks() {
    stubs.mockSts();
    stubs.mockBidragOrganisasjonSaksbehandler();
  }

  @AfterEach
  public void resetMocks() {
    WireMock.reset();
    WireMock.resetToDefault();
  }

  protected String initUrl() {
    return "http://localhost:" + port + contextPath;
  }

}
