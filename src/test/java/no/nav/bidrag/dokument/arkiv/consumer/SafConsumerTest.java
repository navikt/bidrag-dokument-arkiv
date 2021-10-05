package no.nav.bidrag.dokument.arkiv.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.model.SafException;
import no.nav.bidrag.dokument.arkiv.dto.Sak;
import no.nav.bidrag.dokument.arkiv.security.OidcTokenGenerator;
import no.nav.bidrag.dokument.arkiv.security.TokenForBasicAuthenticationGenerator;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@DisplayName("SafConsumer")
@SpringBootTest(
    classes = {BidragDokumentArkivLocal.class},
    properties = {"SAF_GRAPHQL_URL=http://localhost:8090/query"},
    webEnvironment = WebEnvironment.DEFINED_PORT
)
@AutoConfigureWireMock(port = 8090)
class SafConsumerTest {

  @MockBean
  private OidcTokenGenerator oidcTokenGenerator;
  @MockBean
  private TokenForBasicAuthenticationGenerator tokenForBasicAuthenticationGenerator;
  @Autowired
  private ResourceByDiscriminator<SafConsumer> safConsumers;

  @BeforeEach
  void mockTokenValidation() {
    var tokenValidationContextMock = mock(TokenValidationContext.class);
    var jwtTokenMock = mock(JwtToken.class);

    when(oidcTokenGenerator.fetchBearerToken()).thenReturn("token");
    when(tokenForBasicAuthenticationGenerator.generateToken()).thenReturn("token");
    when(tokenValidationContextMock.getJwtTokenAsOptional(BidragDokumentArkivConfig.ISSUER)).thenReturn(Optional.of(jwtTokenMock));
    when(jwtTokenMock.getTokenAsString()).thenReturn("A very secure token");
  }

  @Test
  @DisplayName("skal utføre SAF spørring uten å finne journalposter")
  void skalUtforeSafSporringUtenNoenJournalposter() {
    // gitt
    stubEmptyQueryResult();

    // når
    var journalposter = safConsumers.get(Discriminator.REGULAR_USER).finnJournalposter("007", "BID");

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
    var journalposter = safConsumers.get(Discriminator.REGULAR_USER).finnJournalposter("007", "BID");

    // så
    assertAll(
        () -> assertThat(journalposter).hasSize(1),
        () -> assertThat(journalposter.iterator().next()).extracting(Journalpost::getJournalpostId).isEqualTo("1001")
    );
  }

  @Test
  @DisplayName("skal hente enkel journalpost")
  void skalHenteEnkelJournalpost() {
    // gitt
    stubFor(post(urlEqualTo("/query/"))
        .willReturn(aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody("""
                {
                    "data": {
                        "journalpost": {
                            "avsenderMottaker": {
                                "navn": "Avsender"
                            },
                            "bruker": {
                                "id": "1385076492416",
                                "type": "AKTOERID"
                            },
                            "dokumenter": [
                                {
                                    "tittel": null
                                }
                            ],
                            "journalforendeEnhet": "4806",
                            "journalfortAvNavn": null,
                            "journalpostId": "510536260",
                            "journalposttype": "U",
                            "journalstatus": "UNDER_ARBEID",
                            "relevanteDatoer": [
                                {
                                    "dato": "2021-08-18T13:20:33",
                                    "datotype": "DATO_DOKUMENT"
                                }
                            ],
                            "tema": "BID",
                            "tittel": "KOPIFORSIDE",
                            "sak": {
                                "fagsakId": "2106534"
                            }
                        }
                    }
                }
                """
            )
        )
    );

    // når
    var journalpost = safConsumers.get(Discriminator.REGULAR_USER).hentJournalpost(23424234L);
    assertThat(journalpost).isNotNull();

    assertAll(
        () -> assertThat(journalpost.getTema()).as("tema").isEqualTo("BID"),
        () -> assertThat(journalpost.getAvsenderMottaker()).as("navn").extracting(AvsenderMottaker::getNavn).isEqualTo("Avsender"),
        () -> assertThat(journalpost.getBruker()).as("bruker.id").extracting(Bruker::getId).isEqualTo("1385076492416"),
        () -> assertThat(journalpost.getJournalforendeEnhet()).as("journalforendeEnhet").isEqualTo("4806"),
        () -> assertThat(journalpost.getTittel()).as("tittel").isEqualTo("KOPIFORSIDE"),
        () -> assertThat(journalpost.getSak()).as("sak.fagsakId").extracting(Sak::getFagsakId).isEqualTo("2106534")
    );
  }

  @Test
  @DisplayName("skal kaste feil når SAF responderer med feil")
  void skalFeileNarSafGirIngenTilgang() {
    // gitt
    stubFor(post(urlEqualTo("/query/"))
        .willReturn(aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody("""
                {
                    "errors": [
                        {
                            "message": "Tilgang til ressurs ble avvist. Saksbehandler eller system har ikke tilgang til ressurs tilhørende bruker som har kode 6/7, egen ansatt eller utenfor tillatt geografisk område.",
                            "locations": [
                                {
                                    "line": 2,
                                    "column": 3
                                }
                            ],
                            "path": [
                                "journalpost"
                            ],
                            "extensions": {
                                "code": "forbidden",
                                "classification": "ExecutionAborted"
                            }
                        }
                    ],
                    "data": {
                        "journalpost": null
                    }
                }
                """
            )
        )
    );

    // når
    try {
      safConsumers.get(Discriminator.REGULAR_USER).hentJournalpost(23424234L);
      fail("Saf kallet skal feile");
    } catch (SafException e) {
      assertThat(e.getMessage()).isEqualTo(
          "Tilgang til ressurs ble avvist. Saksbehandler eller system har ikke tilgang til ressurs tilhørende bruker som har kode 6/7, egen ansatt eller utenfor tillatt geografisk område.");
      assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }
  }

  @Test
  @DisplayName("skal kaste feil når SAF responderer med journalpost ikke funnet")
  void skalFeileNarSafIkkeFinnerJournalpost() {
    // gitt
    stubFor(post(urlEqualTo("/query/"))
        .willReturn(aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withStatus(200)
            .withBody("""
                {
                    "errors": [
                        {
                            "message": "Fant ikke journalpost i fagarkivet. journalpostId=910536260",
                            "locations": [
                                {
                                    "line": 2,
                                    "column": 3
                                }
                            ],
                            "path": [
                                "journalpost"
                            ],
                            "extensions": {
                                "code": "not_found",
                                "classification": "ExecutionAborted"
                            }
                        }
                    ],
                    "data": {
                        "journalpost": null
                    }
                }
                """
            )
        )
    );

    // når
    try {
      safConsumers.get(Discriminator.REGULAR_USER).hentJournalpost(23424234L);
      fail("Saf kallet skal feile");
    } catch (JournalpostIkkeFunnetException e) {
      assertThat(e.getMessage()).isEqualTo("Fant ikke journalpost i fagarkivet. journalpostId=910536260");
      assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }

  @Test
  @DisplayName("skal generere Nav-Consumer-Token header ved query kall")
  void skalLeggeTokenBlantHeaders() {
    // gitt
    stubEmptyQueryResult();

    // når
    safConsumers.get(Discriminator.REGULAR_USER).finnJournalposter("101", "BID");

    // så
    verify(oidcTokenGenerator).fetchBearerToken();
  }

  @Test
  @DisplayName("skal generere Nav-Consumer-Token header ved query kall")
  void skalLeggeServiceBrukerTokenBlantHeaders() {
    // gitt
    stubEmptyQueryResult();

    // når
    safConsumers.get(Discriminator.SERVICE_USER).finnJournalposter("101", "BID");

    // så
    verify(tokenForBasicAuthenticationGenerator).generateToken();
  }
}
