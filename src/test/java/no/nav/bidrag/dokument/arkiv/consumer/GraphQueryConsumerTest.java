package no.nav.bidrag.dokument.arkiv.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.NavConsumerTokenGenerator;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@DisplayName("GraphQueryConsumer")
@SpringBootTest(
    classes = {BidragDokumentArkivLocal.class},
    properties = {"SAF_GRAPHQL_URL=http://localhost:8090/query"},
    webEnvironment = WebEnvironment.DEFINED_PORT
)
@AutoConfigureWireMock(port = 8090)
class GraphQueryConsumerTest {

  @MockBean
  private TokenValidationContextHolder tokenValidationContextHolderMock;
  @MockBean
  private NavConsumerTokenGenerator navConsumerTokenGeneratorMock;
  @Autowired
  private GraphQueryConsumer graphQueryConsumer;

  @BeforeEach
  void mockTokenValidation() {
    var tokenValidationContextMock = mock(TokenValidationContext.class);
    var jwtTokenMock = mock(JwtToken.class);

    when(tokenValidationContextHolderMock.getTokenValidationContext()).thenReturn(tokenValidationContextMock);
    when(tokenValidationContextMock.getJwtTokenAsOptional(BidragDokumentArkivConfig.ISSUER)).thenReturn(Optional.of(jwtTokenMock));
    when(jwtTokenMock.getTokenAsString()).thenReturn("A very secure token");
  }

  @Test
  @DisplayName("skal utføre SAF spørring uten å finne journalposter")
  void skalUtforeSafSporringUtenNoenJournalposter() {
    // gitt
    stubEmptyQueryResult();

    // når
    var journalposter = graphQueryConsumer.finnJournalposter("007", "BID");

    // så
    assertThat(journalposter).isEmpty();
  }

  private void stubEmptyQueryResult() {
    stubFor(post(urlEqualTo("/query/"))
        .willReturn(aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(String.join(
                "\n",
                "{",
                "  \"data\": {",
                "    \"dokumentoversiktFagsak\": {",
                "      \"journalposter\": []",
                "    }",
                "  }",
                "}"
            ))
        )
    );
  }

  @Test
  @DisplayName("skal utføre SAF spørring og finne en journalpost")
  void skalUtforeSafSporringOgFinneEnJournalpost() {
    // gitt
    stubFor(post(urlEqualTo("/query/"))
        .willReturn(aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody(String.join(
                "\n",
                "{",
                "  \"data\": {",
                "    \"dokumentoversiktFagsak\": {",
                "      \"journalposter\": [ {",
                "          \"journalpostId\":\"1001\"",
                "        } ]",
                "    }",
                "  }",
                "}"
            ))
        )
    );

    // når
    var journalposter = graphQueryConsumer.finnJournalposter("007", "BID");

    // så
    assertAll(
        () -> assertThat(journalposter).hasSize(1),
        () -> assertThat(journalposter.iterator().next()).extracting(Journalpost::getJournalpostId).isEqualTo("1001")
    );
  }

  @Test
  @DisplayName("skal generere Nav-Consumer-Token header ved query kall")
  void skalLeggeTokenBlantHeaders() {
    // gitt
    stubEmptyQueryResult();

    // når
    graphQueryConsumer.finnJournalposter("101", "BID");

    // så
    verify(navConsumerTokenGeneratorMock).generateToken();
  }
}
