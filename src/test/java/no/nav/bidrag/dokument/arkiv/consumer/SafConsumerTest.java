package no.nav.bidrag.dokument.arkiv.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import no.nav.bidrag.commons.security.service.SecurityTokenService;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.Sak;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.model.SafException;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({PROFILE_TEST})
@DisplayName("SafConsumer")
@SpringBootTest(
    classes = {BidragDokumentArkivLocal.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
@Disabled
class SafConsumerTest {

  @Autowired
  private ResourceByDiscriminator<SafConsumer> safConsumers;

  @MockBean
  SecurityTokenService securityTokenService;

  @BeforeEach
  void mockTokenValidation() {
    when(securityTokenService.serviceUserAuthTokenInterceptor()).thenReturn((request, body, execution) -> execution.execute(request, body));
    when(securityTokenService.authTokenInterceptor()).thenReturn((request, body, execution) -> execution.execute(request, body));
    when(securityTokenService.navConsumerTokenInterceptor()).thenReturn((request, body, execution) -> execution.execute(request, body));
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
    stubFor(post(urlEqualTo("/saf/"))
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
}
