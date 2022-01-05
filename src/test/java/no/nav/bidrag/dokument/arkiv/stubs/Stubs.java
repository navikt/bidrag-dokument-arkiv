package no.nav.bidrag.dokument.arkiv.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import java.util.Arrays;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivProxyConsumer;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerInfoResponse;
import org.junit.Assert;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public class Stubs {

  public static String SAKSNUMMER_JOURNALPOST = "5276661";
  public static String SAKSNUMMER_TILKNYTTET_1 = "2106585";
  public static String SAKSNUMMER_TILKNYTTET_2 = "9999999";
  private final ObjectMapper objectMapper = new ObjectMapper();
  public final VerifyStub verifyStub = new VerifyStub();

  private ResponseDefinitionBuilder aClosedJsonResponse() {
    return aResponse()
        .withHeader(HttpHeaders.CONNECTION, "close")
        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json");
  }

  public void mockBidragOrganisasjonSaksbehandler() {
    try {
      stubFor(
          get(urlPathMatching("/organisasjon/bidrag-organisasjon/saksbehandler/info/.*")).willReturn(
              aClosedJsonResponse()
                  .withStatus(HttpStatus.OK.value())
                  .withBody(objectMapper.writeValueAsString(new SaksbehandlerInfoResponse("ident", "navn")))
          )
      );
    } catch (JsonProcessingException e) {
      Assert.fail(e.getMessage());
    }
  }

  public void mockSts() {
    stubFor(
        post(urlPathMatching("/sts/.*")).willReturn(
            aClosedJsonResponse()
                .withStatus(HttpStatus.OK.value())
                .withBody(
                    "{\n"
                        + "    \"access_token\": \"DUMMY\",\n"
                        + "    \"expiresIn\": 3600,\n"
                        + "    \"idToken\": \"DUMMY\",\n"
                        + "    \"scope\": \"openid\",\n"
                        + "    \"token_type\": \"Bearer\"\n"
                        + "}"
                )
        )
    );
  }

  public void mockDokarkivOppdaterRequest(Long journalpostId) throws JsonProcessingException {
    this.mockDokarkivOppdaterRequest(journalpostId, HttpStatus.OK);
  }

  public void mockDokarkivOppdaterDistribusjonsInfoRequest(Long journalpostId, HttpStatus status) throws JsonProcessingException {
    stubFor(
        patch(urlMatching("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId + "/oppdaterDistribusjonsinfo")).willReturn(
            aClosedJsonResponse()
                .withStatus(status.value())
        )
    );
  }

  public void mockDokarkivOppdaterRequest(Long journalpostId, HttpStatus status) throws JsonProcessingException {
    stubFor(
        put(urlMatching("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId)).willReturn(
            aClosedJsonResponse()
                .withStatus(status.value())
                .withBody(objectMapper.writeValueAsString(new OppdaterJournalpostResponse(journalpostId)))
        )
    );
  }

  public void mockDokarkivFeilregistrerRequest(String path, Long journalpostId) {
    stubFor(
        patch(
            urlMatching(
                "/dokarkiv" + String.format(
                    DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
                    journalpostId
                ) + "/" + path
            )
        ).willReturn(
            aClosedJsonResponse()
                .withStatus(HttpStatus.OK.value())
        )
    );
  }

  public void mockDokarkivOppdaterDistribusjonsInfoRequest(Long journalpostId) {
    stubFor(
        patch(
            urlMatching(
                "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/oppdaterDistribusjonsinfo"
            )
        ).willReturn(
            aClosedJsonResponse()
                .withStatus(HttpStatus.OK.value())
        )
    );
  }

  public void mockDokarkivFerdigstillRequest(Long journalpostId) {
    stubFor(
        patch(
            urlMatching(
                "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
            )
        ).willReturn(
            aClosedJsonResponse()
                .withStatus(HttpStatus.OK.value())
        )
    );
  }

  public void mockDokarkivProxyTilknyttRequest(Long journalpostId) {
    stubFor(
        put(
            urlMatching(
                "/dokarkivproxy" + String.format(DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK, journalpostId)
            )
        ).willReturn(
            aClosedJsonResponse()
                .withStatus(HttpStatus.OK.value())
        )
    );
  }

  public void mockSafResponseHentJournalpost(HttpStatus status) {
    mockSafResponseHentJournalpost("journalpostSafResponse.json", status);
  }

  public void mockSafResponseHentJournalpost(String filename, HttpStatus status) {
    stubFor(
        post(urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query journalpost")).willReturn(
            aClosedJsonResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .withStatus(status.value())
                .withBodyFile("json/" + filename)
        )
    );
  }

  public void mockSafResponseTilknyttedeJournalposter(HttpStatus httpStatus) {
    stubFor(
        post(urlEqualTo("/saf/"))
            .withRequestBody(new ContainsPattern("query tilknyttedeJournalposter")).willReturn(
                aClosedJsonResponse()
                    .withStatus(httpStatus.value())
                    .withBodyFile("json/tilknyttedeJournalposter.json")
            )
    );
  }

  public void mockSafResponseDokumentOversiktFagsak(HttpStatus status) {
    stubFor(
        post(urlEqualTo("/saf/"))
            .withRequestBody(new ContainsPattern("query dokumentoversiktFagsak")).willReturn(
                aClosedJsonResponse()
                    .withStatus(status.value())
                    .withBodyFile("json/dokumentoversiktFagsakQueryResponse.json")
            )
    );
  }

  public void mockPersonResponse(PersonResponse personResponse, HttpStatus status) {
    try {
      stubFor(
          get(urlMatching("/person/.*")).willReturn(
              aClosedJsonResponse()
                  .withStatus(status.value())
                  .withBody(new ObjectMapper().writeValueAsString(personResponse))
          )
      );
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }

  public static class VerifyStub {

    public void dokarkivOppdaterDistribusjonsInfoKalt(Long journalpostId, String... contains) {
      var requestPattern = putRequestedFor(urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId + "/oppdaterDistribusjonsinfo"));
      Arrays.stream(contains).forEach(contain -> requestPattern.withRequestBody(new ContainsPattern(contain)));
      verify(requestPattern);
    }

    public void dokarkivOppdaterKalt(Long journalpostId, String... contains) {
      var requestPattern = putRequestedFor(urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId));
      Arrays.stream(contains).forEach(contain -> requestPattern.withRequestBody(new ContainsPattern(contain)));
      verify(requestPattern);
    }

    public void bidragPersonKalt() {
      verify(
          getRequestedFor(urlMatching("/person/.*"))
      );
    }


    private void dokarkivProxyTilknyttSakerKalt(Integer times, Long journalpostId, String... contains) {
      var verify = putRequestedFor(
          urlMatching(
              "/dokarkivproxy" + String.format(DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK, journalpostId)
          )
      );
      Arrays.stream(contains).forEach(contain -> verify.withRequestBody(new ContainsPattern(contain)));
      verify(exactly(times), verify);
    }

    public void dokarkivProxyTilknyttSakerIkkeKalt(Long journalpostId, String... contains) {
      dokarkivProxyTilknyttSakerKalt(0, journalpostId, contains);
    }

    public void dokarkivProxyTilknyttSakerKalt(Long journalpostId, String... contains) {
      dokarkivProxyTilknyttSakerKalt(1, journalpostId, contains);
    }

    private void dokarkivFerdigstillKalt(Integer times, Long journalpostId) {
      verify(exactly(times), patchRequestedFor(
          urlMatching(
              "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
          )
      ));
    }

    public void dokarkivFerdigstillIkkeKalt(Long journalpostId) {
      dokarkivFerdigstillKalt(0, journalpostId);
    }

    public void dokarkivFerdigstillKalt(Long journalpostId) {
      dokarkivFerdigstillKalt(1, journalpostId);
    }

    public void dokarkivFeilregistrerKalt(String path, Long journalpostId) {
      verify(
          patchRequestedFor(urlMatching("/dokarkiv" + String.format(
              DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
              journalpostId
          ) + "/" + path))
      );
    }

    public void dokarkivOppdaterDistribusjonsInfoKalt(Long journalpostId) {
      verify(
          patchRequestedFor(urlMatching("/dokarkiv" +
              DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/"+
              journalpostId + "/oppdaterDistribusjonsinfo"))
      );
    }

    public void harEnSafKallEtterHentJournalpost() {
      verify(
          postRequestedFor(urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query journalpost"))
      );
    }

    public void harSafEnKallEtterDokumentOversiktFagsak() {
      verify(
          postRequestedFor(urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query dokumentoversiktFagsak"))
      );
    }

    public void harEnSafKallEtterTilknyttedeJournalposter() {
      harEnSafKallEtterTilknyttedeJournalposter(1);
    }

    public void harIkkeEnSafKallEtterTilknyttedeJournalposter() {
      harEnSafKallEtterTilknyttedeJournalposter(0);
    }

    private void harEnSafKallEtterTilknyttedeJournalposter(Integer times) {
      verify(exactly(times),
          postRequestedFor(urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query tilknyttedeJournalposter"))
      );
    }
  }
}
