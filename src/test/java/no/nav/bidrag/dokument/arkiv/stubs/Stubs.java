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
import static no.nav.bidrag.dokument.arkiv.stubs.TestDataKt.opprettDokumentOversiktfagsakResponse;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.Pair;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivProxyConsumer;
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.GeografiskTilknytningResponse;
import no.nav.bidrag.dokument.arkiv.dto.HentPostadresseResponse;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal;
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakResponse;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.OppgaveSokResponse;
import no.nav.bidrag.dokument.arkiv.dto.JoarkOpprettJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerInfoResponse;
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class Stubs {

  public static String SAKSNUMMER_JOURNALPOST = "5276661";
  public static String SAKSNUMMER_TILKNYTTET_1 = "2106585";
  public static String BRUKER_ENHET = "4899";
  public static String SAKSNUMMER_TILKNYTTET_2 = "9999999";
  @Autowired
  private final ObjectMapper objectMapper;
  public final VerifyStub verifyStub = new VerifyStub();

  public Stubs(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  private ResponseDefinitionBuilder aClosedJsonResponse() {
    return aResponse()
        .withHeader(HttpHeaders.CONNECTION, "close")
        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json");
  }

  public void mockOrganisasjonGeografiskTilknytning() {
    mockOrganisasjonGeografiskTilknytning(BRUKER_ENHET);
  }
  public void mockOrganisasjonGeografiskTilknytning(String enhetId) {
    try {
      stubFor(
          get(urlPathMatching("/organisasjon/bidrag-organisasjon/arbeidsfordeling/enhetsliste/geografisktilknytning/.*")).willReturn(
              aClosedJsonResponse()
                  .withStatus(HttpStatus.OK.value())
                  .withBody(objectMapper.writeValueAsString(new GeografiskTilknytningResponse(enhetId, "navn")))
          )
      );
    } catch (JsonProcessingException e) {
      Assert.fail(e.getMessage());
    }
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

  public void mockDokdistFordelingRequest(HttpStatus status, String bestillingId) {
    try {
      stubFor(
          post(urlMatching("/dokdistfordeling/rest/v1/distribuerjournalpost")).willReturn(
              aClosedJsonResponse()
                  .withStatus(status.value())
                  .withBody(objectMapper.writeValueAsString(new DokDistDistribuerJournalpostResponse(bestillingId)))
          )
      );
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }

  public void mockSafHentDokumentResponse() throws JsonProcessingException {
    stubFor(
        get(urlMatching("/saf/rest/hentdokument/.*")).willReturn(
            aClosedJsonResponse()
                .withStatus(HttpStatus.OK.value())
                .withBody("JVBERi0xLjcgQmFzZTY0IGVuY29kZXQgZnlzaXNrIGRva3VtZW50")
        )
    );
  }

  public void mockDokarkivOpprettRequest(Long nyJournalpostId, HttpStatus status) throws JsonProcessingException {
    stubFor(
        post(urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1+"?forsoekFerdigstill=true")).willReturn(
            aClosedJsonResponse()
                .withStatus(status.value())
                .withBody(objectMapper.writeValueAsString(new JoarkOpprettJournalpostResponse(nyJournalpostId, "FERDIGTILT", null, true, new ArrayList<>())))
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

  public void mockDokarkivOpphevFeilregistrerRequest(Long journalpostId) {
    stubFor(
        patch(
            urlEqualTo(
                "/dokarkiv" + String.format(
                    DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
                    journalpostId
                ) + "/opphevFeilregistrertSakstilknytning"
            )
        ).willReturn(
            aClosedJsonResponse()
                .withStatus(HttpStatus.OK.value())
        )
    );
  }

  public void mockDokarkivFeilregistrerRequest(Long journalpostId) {
    stubFor(
        patch(
            urlEqualTo(
                "/dokarkiv" + String.format(
                    DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
                    journalpostId
                ) + "/feilregistrerSakstilknytning"
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
    mockDokarkivProxyTilknyttRequest(journalpostId, 123213213L, HttpStatus.OK);
  }

  public void mockDokarkivProxyTilknyttRequest(Long journalpostId, Long nyJournalpostId) {
    mockDokarkivProxyTilknyttRequest(journalpostId, nyJournalpostId, HttpStatus.OK);
  }

  public void mockDokarkivProxyTilknyttRequest(Long journalpostId, Long nyJournalpostId, HttpStatus status) {
    try {
      stubFor(
          put(
              urlMatching(
                  "/dokarkivproxy" + String.format(DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK, journalpostId)
              )
          ).willReturn(
              aClosedJsonResponse()
                  .withStatus(status.value())
                  .withBody(objectMapper.writeValueAsString(new KnyttTilAnnenSakResponse(nyJournalpostId.toString())))
          )
      );
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }

  public void mockSafResponseHentJournalpost(HttpStatus status) {
    mockSafResponseHentJournalpost("journalpostSafResponse.json", status);
  }

  public void mockSafResponseHentJournalpost(Journalpost journalpost) {
    mockSafResponseHentJournalpost(journalpost, null, null);
  }

  public void mockSafResponseHentJournalpost(Journalpost journalpost, Long journalpostId) {
    try {
      stubFor(
          post(urlEqualTo("/saf/graphql"))
              .withRequestBody(new ContainsPattern("query journalpost"))
              .withRequestBody(new ContainsPattern(String.format("\"variables\":{\"journalpostId\":%s}", journalpostId)))
              .willReturn(
                  aClosedJsonResponse()
                      .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                      .withStatus(HttpStatus.OK.value())
                      .withBody("{\"data\":{\"journalpost\": %s }}".formatted(objectMapper.writeValueAsString(journalpost)))
              )
      );
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }

  public void mockSafResponseHentJournalpost(Journalpost journalpost, String scenarioState, String nextScenario) {
    try {
      stubFor(
          post(urlEqualTo("/saf/graphql"))
              .inScenario("Saf response")
              .whenScenarioStateIs(scenarioState == null ? Scenario.STARTED : scenarioState)
              .withRequestBody(new ContainsPattern("query journalpost")).willReturn(
              aClosedJsonResponse()
                  .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                  .withStatus(HttpStatus.OK.value())
                  .withBody("{\"data\":{\"journalpost\": %s }}".formatted(objectMapper.writeValueAsString(journalpost)))
          ).willSetStateTo(nextScenario)
      );
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }

  public void mockSafResponseHentJournalpost(String filename, HttpStatus status) {
    stubFor(
        post(urlEqualTo("/saf/graphql"))
            .withRequestBody(new ContainsPattern("query journalpost"))
            .willReturn(aClosedJsonResponse()
                  .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                  .withStatus(status.value())
                  .withBodyFile("json/" + filename)
            )
    );
  }

  public void mockSafResponseTilknyttedeJournalposter(HttpStatus httpStatus) {
    stubFor(
        post(urlEqualTo("/saf/graphql"))
            .withRequestBody(new ContainsPattern("query tilknyttedeJournalposter")).willReturn(
                aClosedJsonResponse()
                    .withStatus(httpStatus.value())
                    .withBodyFile("json/tilknyttedeJournalposter.json")
            )
    );
  }

  public void mockSafResponseTilknyttedeJournalposter(List<TilknyttetJournalpost> tilknyttetJournalposts) {
    try {
      stubFor(
          post(urlEqualTo("/saf/graphql"))
              .withRequestBody(new ContainsPattern("query tilknyttedeJournalposter")).willReturn(
                  aClosedJsonResponse()
                      .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                      .withStatus(HttpStatus.OK.value())
                      .withBody("{\"data\":{\"tilknyttedeJournalposter\": %s }}".formatted(objectMapper.writeValueAsString(tilknyttetJournalposts)))

              )
      );
    }catch (Exception e){
      fail(e.getMessage());

    }
  }

  public void mockSafResponseDokumentOversiktFagsak() {
      mockSafResponseDokumentOversiktFagsak(opprettDokumentOversiktfagsakResponse());
  }

  public void mockSafResponseDokumentOversiktFagsak(List<Journalpost> response) {
    try {
      stubFor(
          post(urlEqualTo("/saf/graphql"))
              .withRequestBody(new ContainsPattern("query dokumentoversiktFagsak")).willReturn(
                  aClosedJsonResponse()
                      .withStatus(HttpStatus.OK.value())
                      .withBody("{\"data\":{\"dokumentoversiktFagsak\":{\"journalposter\": %s }}}".formatted(objectMapper.writeValueAsString(response)))
              )
      );
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }

  public void mockSokOppgave() {
    try {
      stubFor(
          get(urlMatching("/oppgave/.*")).willReturn(
              aClosedJsonResponse()
                  .withStatus(HttpStatus.OK.value())
                  .withBody(objectMapper.writeValueAsString(new OppgaveSokResponse()))
          )
      );
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }

  public void mockSokOppgave(OppgaveSokResponse oppgaveSokResponse, HttpStatus status) {
    try {
      stubFor(
          get(urlMatching("/oppgave/.*")).willReturn(
              aClosedJsonResponse()
                  .withStatus(status.value())
                  .withBody(objectMapper.writeValueAsString(oppgaveSokResponse))
          )
      );
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
  }

  public void mockOpprettOppgave(HttpStatus status) {
      stubFor(
          post(urlMatching("/oppgave/.*")).willReturn(
              aClosedJsonResponse()
                  .withStatus(status.value())
          )
      );
  }

  public void mockOppdaterOppgave(HttpStatus status) {
    stubFor(
        patch(urlMatching("/oppgave/.*")).willReturn(
            aClosedJsonResponse()
                .withStatus(status.value())
        )
    );
  }

  public void mockPersonAdresseResponse(HentPostadresseResponse hentPostadresseResponse) {
    try {
      stubFor(
          post(urlMatching("/person/bidrag-person/adresse/post")).willReturn(
              aClosedJsonResponse()
                  .withStatus(HttpStatus.OK.value())
                  .withBody(new ObjectMapper().writeValueAsString(hentPostadresseResponse == null ? new HentPostadresseResponse("Ramsegata 1", "Bakredør", null, "3939", "OSLO", "NO") : hentPostadresseResponse))
          )
      );
    } catch (JsonProcessingException e) {
      fail(e.getMessage());
    }
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

    public void hentPersonAdresseKalt(String personId){
      var requestPattern = postRequestedFor(urlMatching("/person/bidrag-person/adresse/post"));
      requestPattern.withRequestBody(new ContainsPattern(personId));
      verify(requestPattern);
    }

    public void bidragOrganisasjonGeografiskTilknytningKalt(){
      bidragOrganisasjonGeografiskTilknytningKalt(null);
    }

    public void bidragOrganisasjonGeografiskTilknytningKalt(String ident){
      verify(getRequestedFor(urlPathMatching(
          String.format("/organisasjon/bidrag-organisasjon/arbeidsfordeling/enhetsliste/geografisktilknytning/%s", ident == null ? ".*" : ident))));

    }

    public void oppgaveOpprettKalt(String... contains){
      var requestPattern = postRequestedFor(urlMatching("/oppgave/.*"));
      Arrays.stream(contains).forEach(contain -> requestPattern.withRequestBody(new ContainsPattern(contain)));
      verify(requestPattern);
    }

    public void oppgaveOppdaterKalt(Integer count, String... contains){
      var requestPattern = patchRequestedFor(urlMatching("/oppgave/.*"));
      Arrays.stream(contains).forEach(contain -> requestPattern.withRequestBody(new ContainsPattern(contain)));
      verify(count, requestPattern);
    }
    public void oppgaveSokIkkeKalt(){
      verify(0, getRequestedFor(urlMatching("/oppgave/.*")));
    }
    public void oppgaveOpprettIkkeKalt(){
      verify(0, postRequestedFor(urlMatching("/oppgave/.*")));
    }

    public void oppgaveSokKalt(Pair<String, String>... params){
      var requestPattern = getRequestedFor(urlMatching("/oppgave/.*"));
      Arrays.stream(params).forEach(contain -> requestPattern.withQueryParam(contain.getFirst(), new ContainsPattern(contain.getSecond())));
      verify(requestPattern);
    }
    public void dokarkivOppdaterDistribusjonsInfoKalt(Long journalpostId, String... contains) {
      var requestPattern = putRequestedFor(urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId + "/oppdaterDistribusjonsinfo"));
      Arrays.stream(contains).forEach(contain -> requestPattern.withRequestBody(new ContainsPattern(contain)));
      verify(requestPattern);
    }


    public void dokarkivOppdaterIkkeKalt(Long journalpostId) {
      verify(0, putRequestedFor(urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId)));
    }


    public void dokarkivOppdaterKalt(Long journalpostId, String... contains) {
      var requestPattern = putRequestedFor(urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId));
      Arrays.stream(contains).forEach(contain -> requestPattern.withRequestBody(new ContainsPattern(contain)));
      verify(requestPattern);
    }

    public void safHentDokumentKalt(Long journalpostId, Long dokumentId) {
      var requestPattern = getRequestedFor(urlEqualTo(String.format("/saf/rest/hentdokument/%s/%s/ARKIV", journalpostId, dokumentId)));
      verify(requestPattern);
    }

    public void dokarkivOpprettKalt(String... contains) {
      var requestPattern = postRequestedFor(urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "?forsoekFerdigstill=true"));
      Arrays.stream(contains).forEach(contain -> requestPattern.withRequestBody(new ContainsPattern(contain)));
      verify(requestPattern);
    }

    public void bidragPersonKalt() {
      verify(
          getRequestedFor(urlMatching("/person/.*"))
      );
    }

    public void bidragPersonIkkeKalt() {
      verify(0,
          getRequestedFor(urlMatching("/person/.*"))
      );
    }

    public void dokdistFordelingKalt(String... contains) {
      var requestPattern = postRequestedFor(urlMatching("/dokdistfordeling/.*"));
      Arrays.stream(contains).forEach(contain -> requestPattern.withRequestBody(new ContainsPattern(contain)));
      verify(requestPattern);
    }

    public void dokdistFordelingIkkeKalt() {
      verify(0,
          postRequestedFor(urlMatching("/dokdistfordeling/.*"))
      );
    }


    public void dokarkivProxyTilknyttSakerKalt(Integer times, Long journalpostId, String... contains) {
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

    public void dokarkivOpphevFeilregistrerIkkeKalt(Long journalpostId) {
      dokarkivOpphevFeilregistrerKalt(journalpostId);
    }

    public void dokarkivOpphevFeilregistrerKalt(Long journalpostId) {
      dokarkivOpphevFeilregistrerKalt(1, journalpostId);
    }

    public void dokarkivOpphevFeilregistrerKalt(Integer count, Long journalpostId) {
      verify(count,
          patchRequestedFor(urlMatching("/dokarkiv" + String.format(
              DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
              journalpostId
          ) + "/opphevFeilregistrertSakstilknytning"))
      );
    }

    public void dokarkivFeilregistrerKalt(Long journalpostId) {
      verify(
          patchRequestedFor(urlMatching("/dokarkiv" + String.format(
              DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
              journalpostId
          ) + "/feilregistrerSakstilknytning"))
      );
    }
    public void dokarkivFeilregistrerIkkeKalt(Long journalpostId) {
      verify(0,
          patchRequestedFor(urlMatching("/dokarkiv" + String.format(
              DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
              journalpostId
          ) + "/feilregistrerSakstilknytning"))
      );
    }

    public void dokarkivOppdaterDistribusjonsInfoKalt(Long journalpostId, JournalpostKanal kanal) {
      verify(
          patchRequestedFor(urlMatching("/dokarkiv" +
              DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/"+
              journalpostId + "/oppdaterDistribusjonsinfo")).withRequestBody(new ContainsPattern(kanal.name()))
      );
    }

    public void harEnSafKallEtterHentJournalpost() {
      verify(
          postRequestedFor(urlEqualTo("/saf/graphql")).withRequestBody(new ContainsPattern("query journalpost"))
      );
    }

    public void harSafKallEtterHentJournalpost(Integer antall) {
      verify(
          antall,
          postRequestedFor(urlEqualTo("/saf/graphql")).withRequestBody(new ContainsPattern("query journalpost"))
      );
    }

    public void harSafEnKallEtterDokumentOversiktFagsak() {
      verify(
          postRequestedFor(urlEqualTo("/saf/graphql")).withRequestBody(new ContainsPattern("query dokumentoversiktFagsak"))
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
          postRequestedFor(urlEqualTo("/saf/graphql")).withRequestBody(new ContainsPattern("query tilknyttedeJournalposter"))
      );
    }
  }
}
