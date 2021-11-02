package no.nav.bidrag.dokument.arkiv.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
  private ObjectMapper objectMapper = new ObjectMapper();

  public void mockBidragOrganisasjonSaksbehandler() {
    try {
      stubFor(
          get(urlPathMatching("/organisasjon/bidrag-organisasjon/saksbehandler/info/.*")).willReturn(
              aResponse()
                  .withHeader(HttpHeaders.CONNECTION, "close")
                  .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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
            aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .withHeader(HttpHeaders.CONNECTION, "close")
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

  public void verifyDokarkivOppdaterRequest(Long journalpostId, String contains) {
    verify(
        putRequestedFor(urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId))
            .withRequestBody(new ContainsPattern(contains))
    );
  }

  public void verifyDokarkivFerdigstillRequested(Long journalpostId){
    verify(patchRequestedFor(
        urlMatching(
            "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
        )
    ));
  }

  public void verifyDokarkivProxyTilknyttSakerRequested(Long journalpostId, String ...contains){
    var verify = putRequestedFor(
        urlMatching(
            "/dokarkivproxy" + String.format(DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK, journalpostId)
        )
    );
    Arrays.stream(contains).forEach(contain -> verify.withRequestBody(new ContainsPattern(contain)));
    verify(verify);
  }

  public void verifyDokarkivProxyTilknyttSakerNotRequested(Long journalpostId, String ...contains){
    var verify = putRequestedFor(
        urlMatching(
            "/dokarkivproxy" + String.format(DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK, journalpostId)
        )
    );
    Arrays.stream(contains).forEach(contain -> verify.withRequestBody(new ContainsPattern(contain)));
    verify(0, verify);
  }

  public void verifyDokarkivFerdigstillNotRequested(Long journalpostId){
    verify(0, patchRequestedFor(
        urlMatching(
            "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
        )
    ));
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
    verify(
        postRequestedFor(urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query tilknyttedeJournalposter"))
    );
  }

  public void verifySafTilknyttedeJournalpostedNotRequested(){
    verify(0,
        postRequestedFor(urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query tilknyttedeJournalposter"))
    );
  }



  public void mockDokarkivOppdaterRequest(Long journalpostId, HttpStatus status) throws JsonProcessingException {
    stubFor(
        put(urlMatching("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId)).willReturn(
            aResponse()
                .withHeader(HttpHeaders.CONNECTION, "close")
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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
            aResponse()
                .withHeader(HttpHeaders.CONNECTION, "close")
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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
            aResponse()
                .withHeader(HttpHeaders.CONNECTION, "close")
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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
            aResponse()
                .withHeader(HttpHeaders.CONNECTION, "close")
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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
            aResponse()
                .withHeader(HttpHeaders.CONNECTION, "close")
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
                aResponse()
                    .withHeader(HttpHeaders.CONNECTION, "close")
                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .withStatus(httpStatus.value())
                    .withBodyFile("json/tilknyttedeJournalposter.json")
            )
    );
  }

  public void mockSafResponseDokumentOversiktFagsak(HttpStatus status) {
    stubFor(
        post(urlEqualTo("/saf/"))
            .withRequestBody(new ContainsPattern("query dokumentoversiktFagsak")).willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONNECTION, "close")
                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .withStatus(status.value())
                    .withBodyFile("json/dokumentoversiktFagsakQueryResponse.json")
            )
    );
  }

  public void verifyPersonRequested() {
    verify(
        getRequestedFor(urlMatching("/person/.*"))
    );
  }

  public void mockPersonResponse(PersonResponse personResponse, HttpStatus status) {
    try {
      stubFor(
          get(urlMatching("/person/.*")).willReturn(
              aResponse()
                  .withHeader(HttpHeaders.CONNECTION, "close")
                  .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                  .withStatus(status.value())
                  .withBody(new ObjectMapper().writeValueAsString(personResponse))
          )
      );
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }
}
