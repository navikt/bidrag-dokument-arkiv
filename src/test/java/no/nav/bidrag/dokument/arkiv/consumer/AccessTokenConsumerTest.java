package no.nav.bidrag.dokument.arkiv.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;

import no.nav.bidrag.dokument.arkiv.model.TokenException;
import no.nav.bidrag.dokument.arkiv.dto.security.TokenForBasicAuthentication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessTokenConsumer")
class AccessTokenConsumerTest {

  @Mock
  private RestTemplate restTemplateMock;

  @InjectMocks
  private AccessTokenConsumer accessTokenConsumer;

  @Test
  @DisplayName("skal feile når argumenter er null")
  void skalFeileNarArgumenterErNull() {
    assertAll(
        () -> assertThatIllegalArgumentException().as("username is null")
            .isThrownBy(() -> accessTokenConsumer.finnTokenFor(null, "na")),
        () -> assertThatIllegalArgumentException().as("secret is null")
            .isThrownBy(() -> accessTokenConsumer.finnTokenFor("na", null))
    );
  }

  @Test
  @DisplayName("skal hente token for basic authentication")
  void skalHenteTokenForBasicAuthentication() {
    TokenForBasicAuthentication tokenForBasicAuthentication = new TokenForBasicAuthentication();
    tokenForBasicAuthentication.setAccess_token("secret");
    tokenForBasicAuthentication.setTokenType("top");

    when(restTemplateMock.exchange(eq(AccessTokenConsumer.REST_TOKEN_ENDPOINT), eq(POST), any(), eq(TokenForBasicAuthentication.class)))
        .thenReturn(new ResponseEntity<>(tokenForBasicAuthentication, HttpStatus.OK));

    var securityToken = accessTokenConsumer.finnTokenFor("some", "thing");

    assertThat(securityToken).isEqualTo("top secret");
  }

  @Test
  @DisplayName("skal feile når ingen TokenForBasicAuthentication blir returnert")
  void skalFeileNarBodyErNull() {
    when(restTemplateMock.exchange(anyString(), any(), any(), eq(TokenForBasicAuthentication.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

    assertThatExceptionOfType(TokenException.class).isThrownBy(() -> accessTokenConsumer.finnTokenFor("some", "thing"))
        .withMessageContaining(String.valueOf(HttpStatus.NO_CONTENT));
  }
}
