package no.nav.bidrag.dokument.arkiv.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig;
import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import no.nav.security.oidc.context.OIDCValidationContext;
import no.nav.security.oidc.context.TokenContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@DisplayName("JournalforingConsumerTest")
class JournalforingConsumerTest {

  @InjectMocks
  private JournalforingConsumer journalforingConsumer;
  @Mock
  private OIDCRequestContextHolder oidcRequestContextHolderMock;
  @Mock
  private RestTemplate restTemplateMock;

  @BeforeEach
  void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @DisplayName("skal feile når Bearer token ikke finnes")
  void skalFeileUtenBearerToken() {
    assertThatIllegalStateException().isThrownBy(() -> journalforingConsumer.hentJournalforing(1001))
        .withMessage("Kunne ikke videresende Bearer token");
  }

  @Test
  @DisplayName("skal sende header innhold med HttpHeaders.AUTHORIZATION")
  void skalSendeHeaderMedAuthorization() {
    mockBearerToken();
    when(restTemplateMock.exchange(anyString(), eq(HttpMethod.GET), any(), eq(GetJournalpostResponse.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.OK));

    journalforingConsumer.hentJournalforing(1001);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);

    verify(restTemplateMock).exchange(anyString(), eq(HttpMethod.GET), argumentCaptor.capture(), eq(GetJournalpostResponse.class));
    assertThat(argumentCaptor.getValue()).isNotNull();
    var httpHeaders = argumentCaptor.getValue().getHeaders();

    assertAll(
        () -> assertThat(httpHeaders).isNotEmpty(),
        () -> assertThat(httpHeaders).containsKey(HttpHeaders.AUTHORIZATION),
        () -> assertThat(httpHeaders.get(HttpHeaders.AUTHORIZATION)).isNotEmpty()
    );
  }

  private void mockBearerToken() {
    OIDCValidationContext oidcValidationContextMock = mock(OIDCValidationContext.class);
    TokenContext tokenContextMock = mock(TokenContext.class);

    when(oidcRequestContextHolderMock.getOIDCValidationContext()).thenReturn(oidcValidationContextMock);
    when(oidcValidationContextMock.getToken(BidragDokumentArkivConfig.ISSUER)).thenReturn(tokenContextMock);
    when(tokenContextMock.getIdToken()).thenReturn("as safe as it is gonna get!!!");
  }
}