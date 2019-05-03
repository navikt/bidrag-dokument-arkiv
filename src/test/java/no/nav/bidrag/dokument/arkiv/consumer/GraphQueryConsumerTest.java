package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.security.oidc.context.TokenContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.DefaultRequestExpectation;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ActiveProfiles("dev")
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
@PropertySource("classpath:url.properties")
@DisplayName("GraphQueryConsumer")
class GraphQueryConsumerTest {

  @Autowired
  private GraphQueryConsumer graphQueryConsumer;
  @Autowired
  private RestTemplate restTemplate;
  @MockBean
  private OIDCRequestContextHolder oidcRequestContextHolderMock;

  private MockRestServiceServer mockRestServiceServer;

  @BeforeEach
  void createRestServiceServer() {
    mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  @DisplayName("skal feile når Bearer token ikke finnes")
  void skalFeileUtenBearerToken() {
    assertThatIllegalStateException().isThrownBy(() -> graphQueryConsumer.hentJournalpost(1001))
        .withMessage("Kunne ikke videresende Bearer token");
  }

  @Test
  @Disabled("PKIX path building failed Caused by: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target???")
  @DisplayName("skal ikke feile når Baerer token kan utredes")
  void skalIkkeFeileNarBearerTokenKanUtredes() {
    OIDCValidationContext oidcValidationContextMock = mock(OIDCValidationContext.class);
    TokenContext tokenContextMock = mock(TokenContext.class);

    when(oidcRequestContextHolderMock.getOIDCValidationContext()).thenReturn(oidcValidationContextMock);
    when(oidcValidationContextMock.getToken(ISSUER)).thenReturn(tokenContextMock);
    when(tokenContextMock.getIdToken()).thenReturn("i'm so secure!!!");

    graphQueryConsumer.hentJournalpost(1001);

    mockRestServiceServer.expect(new DefaultRequestExpectation(ExpectedCount.once(), System.out::println));
  }
}