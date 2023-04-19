package no.nav.bidrag.dokument.arkiv;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivTest.PROFILE_INTEGRATION;

import com.nimbusds.jose.JOSEObjectType;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;

@Configuration
@Profile(PROFILE_INTEGRATION)
public class TestRestTemplateConfiguration {
  @Autowired
  private MockOAuth2Server mockOAuth2Server;
  @Bean
  HttpHeaderTestRestTemplate httpHeaderTestRestTemplate() {
    TestRestTemplate testRestTemplate = new TestRestTemplate(new RestTemplateBuilder());
    HttpHeaderTestRestTemplate httpHeaderTestRestTemplate = new HttpHeaderTestRestTemplate(testRestTemplate);
    httpHeaderTestRestTemplate.add(HttpHeaders.AUTHORIZATION, this::generateBearerToken);

    return httpHeaderTestRestTemplate;
  }

  private String generateBearerToken() {
    var iss = mockOAuth2Server.issuerUrl("aad");
    var newIssuer = iss.newBuilder().host("localhost").build();
    var token = mockOAuth2Server.issueToken(
            "aad",
            "aud-localhost",
            new DefaultOAuth2TokenCallback(
                    "aad",
                    "aud-localhost",
                    JOSEObjectType.JWT.getType(),
                    List.of("aud-localhost"),
                    Map.of("iss", newIssuer.toString()),
                    3600
            )
    );
    return "Bearer " + token.serialize();
  }
}
