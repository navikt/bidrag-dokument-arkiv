package no.nav.bidrag.dokument.arkiv.consumer;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
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
}