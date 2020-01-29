package no.nav.bidrag.dokument.arkiv.consumer;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.modig.testcertificates.TestCertificates;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
@PropertySource("classpath:url.properties")
@DisplayName("GraphQueryConsumer")
class GraphQueryConsumerTest {

  @Autowired
  private GraphQueryConsumer graphQueryConsumer;
  @MockBean
  private TokenValidationContextHolder tokenValidationContextHolderMock;

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
    TokenValidationContext tokenValidationContextMock = mock(TokenValidationContext.class);
    JwtToken jwtTokenMock = mock(JwtToken.class);

    when(tokenValidationContextHolderMock.getTokenValidationContext()).thenReturn(tokenValidationContextMock);
    when(tokenValidationContextMock.getJwtToken(ISSUER)).thenReturn(jwtTokenMock);
    when(jwtTokenMock.getTokenAsString()).thenReturn("i'm so secure!!!");

    assertThatThrownBy(() -> graphQueryConsumer.hentJournalpost(1001))
        .hasMessageNotContaining("Kunne ikke videresende Bearer token");
  }
}