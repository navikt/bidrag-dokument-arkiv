package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.modig.testcertificates.TestCertificates;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.security.oidc.context.TokenContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
@PropertySource("classpath:url.properties")
@DisplayName("GraphQueryConsumer")
class GraphQueryConsumerTest {

  @Autowired
  private GraphQueryConsumer graphQueryConsumer;
  @MockBean
  private OIDCRequestContextHolder oidcRequestContextHolderMock;

  @BeforeAll
  static void setUpKeyAndTrustStore() {
    TestCertificates.setupKeyAndTrustStore();
  }

  @Test
  @DisplayName("skal feile når Bearer token ikke finnes")
  void skalFeileUtenBearerToken() {
    assertThatIllegalStateException().isThrownBy(() -> graphQueryConsumer.hentJournalpost(1001))
        .withMessage("Kunne ikke videresende Bearer token");
  }

  @Test
  void skalIkkeFeileMedIllegalStateExceptionNarBearerTokenKanUtredes() {
    OIDCValidationContext oidcValidationContextMock = mock(OIDCValidationContext.class);
    TokenContext tokenContextMock = mock(TokenContext.class);

    when(oidcRequestContextHolderMock.getOIDCValidationContext()).thenReturn(oidcValidationContextMock);
    when(oidcValidationContextMock.getToken(ISSUER)).thenReturn(tokenContextMock);
    when(tokenContextMock.getIdToken()).thenReturn("i'm so secure!!!");

    assertThatThrownBy(() -> graphQueryConsumer.hentJournalpost(1001))
        .hasMessageNotContaining("Kunne ikke videresende Bearer token");
  }
}