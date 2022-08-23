package no.nav.bidrag.dokument.arkiv;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.ISSUER_ISSO;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivTest.PROFILE_INTEGRATION;

import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;

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
    var token = mockOAuth2Server.issueToken(ISSUER_ISSO, "aud-localhost", "aud-localhost");
    return "Bearer " + token.serialize();
  }
}
