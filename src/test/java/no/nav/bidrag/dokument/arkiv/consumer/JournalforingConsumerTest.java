package no.nav.bidrag.dokument.arkiv.consumer;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.security.oidc.context.TokenContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
@DisplayName("JournalforingConsumerTest")
class JournalforingConsumerTest {

  @Autowired
  private JournalforingConsumer journalforingConsumer;
  @MockBean
  private OIDCRequestContextHolder oidcRequestContextHolderMock;

  @Test
  @DisplayName("skal feile når Bearer token ikke finnes")
  void skalFeileUtenBearerToken() {
    assertThatIllegalStateException().isThrownBy(() -> journalforingConsumer.hentJournalforing(1001))
        .withMessage("Kunne ikke videresende Bearer token");
  }

  @Test
  @DisplayName("skal feile med ugyldig Bearer token")
  void skalFeileMedUgyldigBearerToken() {
    mockBearerToken();

    Assertions.assertThatCode(() -> journalforingConsumer.hentJournalforing(1001))
        .hasMessage("403 Forbidden");
  }

  private void mockBearerToken() {
    OIDCValidationContext oidcValidationContextMock = mock(OIDCValidationContext.class);
    TokenContext tokenContextMock = mock(TokenContext.class);

    when(oidcRequestContextHolderMock.getOIDCValidationContext()).thenReturn(oidcValidationContextMock);
    when(oidcValidationContextMock.getToken(BidragDokumentArkivConfig.ISSUER)).thenReturn(tokenContextMock);
    when(tokenContextMock.getIdToken()).thenReturn("as safe as it is gonna get!!!");
  }
}