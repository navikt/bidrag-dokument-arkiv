package no.nav.bidrag.dokument.arkiv.stubs;

import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
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
      WireMock.stubFor(
          WireMock.get(WireMock.urlPathMatching("/organisasjon/bidrag-organisasjon/saksbehandler/info/.*")).willReturn(
              WireMock.aResponse()
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
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching("/sts/.*")).willReturn(
            WireMock.aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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
    WireMock.verify(
        WireMock.putRequestedFor(WireMock.urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId))
            .withRequestBody(new ContainsPattern(contains))
    );
  }

  public void verifyDokarkivFerdigstillRequested(Long journalpostId){
    WireMock.verify(WireMock.patchRequestedFor(
        WireMock.urlMatching(
            "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
        )
    ));
  }

  public void verifyDokarkivProxyTilknyttSakerRequested(Long journalpostId, String ...contains){
    var verify = WireMock.putRequestedFor(
        WireMock.urlMatching(
            "/dokarkivproxy" + String.format(DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK, journalpostId)
        )
    );
    Arrays.stream(contains).forEach(contain -> verify.withRequestBody(new ContainsPattern(contain)));
    WireMock.verify(verify);
  }

  public void verifyDokarkivProxyTilknyttSakerNotRequested(Long journalpostId, String ...contains){
    var verify = WireMock.putRequestedFor(
        WireMock.urlMatching(
            "/dokarkivproxy" + String.format(DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK, journalpostId)
        )
    );
    Arrays.stream(contains).forEach(contain -> verify.withRequestBody(new ContainsPattern(contain)));
    WireMock.verify(0, verify);
  }

  public void verifyDokarkivFerdigstillNotRequested(Long journalpostId){
    WireMock.verify(0, WireMock.patchRequestedFor(
        WireMock.urlMatching(
            "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
        )
    ));
  }

  public void verifyDokarkivFeilregistrerRequest(String path, Long journalpostId) {
    WireMock.verify(
        WireMock.patchRequestedFor(WireMock.urlMatching("/dokarkiv" + String.format(
            DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
            journalpostId
        ) + "/" + path))
    );
  }

  public void verifySafHentJournalpostRequested(){
    WireMock.verify(
        WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query journalpost"))
    );
  }

  public void verifySafDokumentOversiktFagsakRequested(){
    WireMock.verify(
        WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query dokumentoversiktFagsak"))
    );
  }

  public void verifySafTilknyttedeJournalpostedRequested(){
    WireMock.verify(
        WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query tilknyttedeJournalposter"))
    );
  }

  public void verifySafTilknyttedeJournalpostedNotRequested(){
    WireMock.verify(0,
        WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query tilknyttedeJournalposter"))
    );
  }



  public void mockDokarkivOppdaterRequest(Long journalpostId, HttpStatus status) throws JsonProcessingException {
    WireMock.stubFor(
        WireMock.put(WireMock.urlMatching("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId)).willReturn(
            WireMock.aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .withStatus(status.value())
                .withBody(objectMapper.writeValueAsString(new OppdaterJournalpostResponse(journalpostId)))
        )
    );
  }

  public void mockDokarkivFeilregistrerRequest(String path, Long journalpostId) {
    WireMock.stubFor(
        WireMock.patch(
            WireMock.urlMatching(
                "/dokarkiv" + String.format(
                    DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
                    journalpostId
                ) + "/" + path
            )
        ).willReturn(
            WireMock.aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .withStatus(HttpStatus.OK.value())
        )
    );
  }

  public void mockDokarkivFerdigstillRequest(Long journalpostId) {
    WireMock.stubFor(
        WireMock.patch(
            WireMock.urlMatching(
                "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
            )
        ).willReturn(
            WireMock.aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .withStatus(HttpStatus.OK.value())
        )
    );
  }

  public void mockDokarkivProxyTilknyttRequest(Long journalpostId) {
    WireMock.stubFor(
        WireMock.put(
            WireMock.urlMatching(
                "/dokarkivproxy" + String.format(DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK, journalpostId)
            )
        ).willReturn(
            WireMock.aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .withStatus(HttpStatus.OK.value())
        )
    );
  }

  public void mockSafResponseHentJournalpost(HttpStatus status) {
    mockSafResponseHentJournalpost("journalpostSafResponse.json", status);
  }

  public void mockSafResponseHentJournalpost(String filename, HttpStatus status) {
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo("/saf/")).withRequestBody(new ContainsPattern("query journalpost")).willReturn(
            WireMock.aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .withStatus(status.value())
                .withBodyFile("json/"+filename)
        )
    );
  }

  public void mockSafResponseTilknyttedeJournalposter(HttpStatus httpStatus) {
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo("/saf/"))
            .withRequestBody(new ContainsPattern("query tilknyttedeJournalposter")).willReturn(
                WireMock.aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .withStatus(httpStatus.value())
                    .withBodyFile("json/tilknyttedeJournalposter.json")
            )
    );
  }

  public void mockSafResponseDokumentOversiktFagsak(HttpStatus status) {
    WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo("/saf/"))
            .withRequestBody(new ContainsPattern("query dokumentoversiktFagsak")).willReturn(
                WireMock.aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .withStatus(status.value())
                    .withBodyFile("json/dokumentoversiktFagsakQueryResponse.json")
            )
    );
  }

  public void verifyPersonRequested() {
    WireMock.verify(
        WireMock.getRequestedFor(WireMock.urlMatching("/person/.*"))
    );
  }

  public void mockPersonResponse(PersonResponse personResponse, HttpStatus status) {
    try {
      WireMock.stubFor(
          WireMock.get(WireMock.urlMatching("/person/.*")).willReturn(
              WireMock.aResponse()
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
