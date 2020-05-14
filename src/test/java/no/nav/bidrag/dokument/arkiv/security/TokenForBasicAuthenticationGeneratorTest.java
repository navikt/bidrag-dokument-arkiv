package no.nav.bidrag.dokument.arkiv.security;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@DisplayName("TokenForBasicAuthenticationGenerator")
@SpringBootTest(
    classes = {BidragDokumentArkivLocal.class},
    properties = {"ACCESS_TOKEN_URL=http://localhost:8090", "SRV_BD_ARKIV_AUTH=secured"},
    webEnvironment = WebEnvironment.DEFINED_PORT
)
@AutoConfigureWireMock(port = 8090)
class TokenForBasicAuthenticationGeneratorTest {

  @Autowired
  private TokenForBasicAuthenticationGenerator tokenForBasicAuthenticationGenerator;

  @Test
  @DisplayName("skal hente security token for srvbdarkiv")
  void skalHenteTokenForBasicAuthentication() {
    stubFor(post(urlEqualTo("/rest/v1/sts/token"))
        .willReturn(aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(201)
            .withBody(String.join(
                "\n",
                "{",
                "  \"access_token\": \"A very secret token\",",
                "  \"token_type\": \"Bearer\",",
                "  \"expires_in\": 0",
                "}"
            ))
        )
    );

    var securityToken = tokenForBasicAuthenticationGenerator.generateToken();

    assertThat(securityToken).isEqualTo("Bearer A very secret token");
  }

  @Test
  @DisplayName("skal legge til kodet header for servicebruker")
  void skalLeggeTilKodedHeaderForServiceBruker() {

    stubFor(post(urlEqualTo("/rest/v1/sts/token"))
        .willReturn(aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(201)
            .withBody(String.join(
                "\n",
                "{",
                "  \"access_token\": \"A very secret token\",",
                "  \"token_type\": \"Bearer\",",
                "  \"expires_in\": 0",
                "}"
            ))
        )
    );

    tokenForBasicAuthenticationGenerator.generateToken();

    verify(postRequestedFor(urlEqualTo("/rest/v1/sts/token"))
        .withHeader(HttpHeaders.AUTHORIZATION, new AnythingPattern())
    );
  }
}