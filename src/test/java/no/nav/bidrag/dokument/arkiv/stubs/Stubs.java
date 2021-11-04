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

  private ResponseDefinitionBuilder baseResponse(){
    return aResponse()
        .withHeader(HttpHeaders.CONNECTION, "close")
        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json");
  }

  public void mockBidragOrganisasjonSaksbehandler() {
    try {
      stubFor(
          get(urlPathMatching("/organisasjon/bidrag-organisasjon/saksbehandler/info/.*")).willReturn(
              baseResponse()
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
            baseResponse()
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

  public void mockDokarkivOppdaterRequest(Long journalpostId, HttpStatus status) throws JsonProcessingException {
    stubFor(
        put(urlMatching("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId)).willReturn(
            baseResponse()
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
            baseResponse()
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
            baseResponse()
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
            baseResponse()
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
            baseResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .withStatus(status.value())
                .withBodyFile("json/"+filename)
        )
    );
  }

  public void mockSafResponseTilknyttedeJournalposter(HttpStatus httpStatus) {
    stubFor(
        post(urlEqualTo("/saf/"))
            .withRequestBody(new ContainsPattern("query tilknyttedeJournalposter")).willReturn(
                baseResponse()
                    .withStatus(httpStatus.value())
                    .withBodyFile("json/tilknyttedeJournalposter.json")
            )
    );
  }

  public void mockSafResponseDokumentOversiktFagsak(HttpStatus status) {
    stubFor(
        post(urlEqualTo("/saf/"))
            .withRequestBody(new ContainsPattern("query dokumentoversiktFagsak")).willReturn(
                baseResponse()
                    .withStatus(status.value())
                    .withBodyFile("json/dokumentoversiktFagsakQueryResponse.json")
            )
    );
  }

  public void mockPersonResponse(PersonResponse personResponse, HttpStatus status) {
    try {
      stubFor(
          get(urlMatching("/person/.*")).willReturn(
              baseResponse()
                  .withStatus(status.value())
                  .withBody(new ObjectMapper().writeValueAsString(personResponse))
          )
      );
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }

  public static class VerifyStub {

    public void verifyDokarkivOppdaterRequest(Long journalpostId, String ...contains) {
      var requestPattern =    putRequestedFor(urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId));
      Arrays.stream(contains).forEach(contain -> requestPattern.withRequestBody(new ContainsPattern(contain)));
      verify(requestPattern);
    }

    public void verifyPersonRequested() {
      verify(
          getRequestedFor(urlMatching("/person/.*"))
      );
    }

    public void verifyDokarkivProxyTilknyttSakerRequested(Integer times, Long journalpostId, String ...contains){
      var verify = putRequestedFor(
          urlMatching(
              "/dokarkivproxy" + String.format(DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK, journalpostId)
          )
      );
      Arrays.stream(contains).forEach(contain -> verify.withRequestBody(new ContainsPattern(contain)));
      verify(exactly(times), verify);
    }

    public void verifyDokarkivProxyTilknyttSakerRequested(Long journalpostId, String ...contains){
      verifyDokarkivProxyTilknyttSakerRequested(1, journalpostId, contains);
    }

    public void verifyDokarkivFerdigstillRequested(Integer times, Long journalpostId){
      verify(exactly(times), patchRequestedFor(
          urlMatching(
              "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
          )
      ));
    }

    public void verifyDokarkivFerdigstillRequested(Long journalpostId){
      verifyDokarkivFerdigstillRequested(1, journalpostId);
    }

    public void verifyDokarkivFeilregistrerRequest(String path, Long journalpostId) {
      verify(
          patchRequestedFor(urlMatching("/dokarkiv" + String.format(
              DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
              journalpostId
          ) + "/" + path))
      );
    }

    public void verifySafHentJournalpostRequested(){
      verify(
          postRequestedFor(urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query journalpost"))
      );
    }

    public void verifySafDokumentOversiktFagsakRequested(){
      verify(
          postRequestedFor(urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query dokumentoversiktFagsak"))
      );
    }

    public void verifySafTilknyttedeJournalpostedRequested(){
      verifySafTilknyttedeJournalpostedRequested(1);
    }

    public void verifySafTilknyttedeJournalpostedRequested(Integer times){
      verify(exactly(times),
          postRequestedFor(urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query tilknyttedeJournalposter"))
      );
    }
  }
}
