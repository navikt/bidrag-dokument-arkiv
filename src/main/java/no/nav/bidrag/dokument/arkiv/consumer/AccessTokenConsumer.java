package no.nav.bidrag.dokument.arkiv.consumer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.dto.security.TokenForBasicAuthentication;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class AccessTokenConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccessTokenConsumer.class);
  private static final MultiValueMap<String, String> PARAMETERS = new LinkedMultiValueMap<>(2) {
    {
      add("grant_type", "client_credentials");
      add("scope", "openid");
    }
  };

  static final String REST_TOKEN_ENDPOINT = "/rest/v1/sts/token";

  private final RestTemplate restTemplate;

  public AccessTokenConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public String finnTokenFor(String serviceUser, String secretForServiceUserNotEncoded) {
    Validate.isTrue(serviceUser != null);
    Validate.isTrue(secretForServiceUserNotEncoded != null);

    LOGGER.info("Henter security token for " + serviceUser);

    var encodedAuthentication = encode(serviceUser, secretForServiceUserNotEncoded);
    var headers = new HttpHeaders();
    headers.put(HttpHeaders.AUTHORIZATION, List.of(encodedAuthentication));

    var tokenForBasicAuthenticationResponse = restTemplate.exchange(
        REST_TOKEN_ENDPOINT, HttpMethod.POST, new HttpEntity<>(PARAMETERS, headers), TokenForBasicAuthentication.class
    );

    var tokenForBasicAuthentication = tokenForBasicAuthenticationResponse.getBody();

    return Optional.ofNullable(tokenForBasicAuthentication)
        .map(TokenForBasicAuthentication::fetchToken)
        .orElseThrow(() -> new IllegalStateException(
            String.format("Kunne ikke hente token fra '%s', response: %s", REST_TOKEN_ENDPOINT, tokenForBasicAuthenticationResponse.getStatusCode())
        ));
  }

  private String encode(String serviceUser, String secretForServiceUserNotEncoded) {
    return Base64.getEncoder().encodeToString((serviceUser + ":" + secretForServiceUserNotEncoded).getBytes(StandardCharsets.UTF_8));
  }
}
