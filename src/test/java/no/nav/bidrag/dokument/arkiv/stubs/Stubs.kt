package no.nav.bidrag.dokument.arkiv.stubs

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivProxyConsumer
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerInfoResponse
import org.junit.Assert
import org.junit.jupiter.api.fail
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.io.IOException

class Stubs {
    companion object {
        @kotlin.jvm.JvmField
        var SAKSNUMMER_JOURNALPOST = "5276661"
        @kotlin.jvm.JvmField
        var SAKSNUMMER_TILKNYTTET_1 = "2106585"
        @kotlin.jvm.JvmField
        var SAKSNUMMER_TILKNYTTET_2 = "9999999"
    }
    var objectMapper: ObjectMapper = ObjectMapper();

    fun mockBidragOrganisasjonSaksbehandler() {
        try {
            WireMock.stubFor(
                WireMock.get(WireMock.urlPathMatching("/organisasjon/bidrag-organisasjon/saksbehandler/info/.*")).willReturn(
                    WireMock.aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withStatus(HttpStatus.OK.value())
                        .withBody(objectMapper.writeValueAsString(SaksbehandlerInfoResponse("ident", "navn")))
                )
            )
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }

    fun mockSts() {
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
        )
    }

    @Throws(JsonProcessingException::class)
    fun mockDokarkivOppdaterRequest(journalpostId: Long) {
        mockDokarkivOppdaterRequest(journalpostId, HttpStatus.OK)
    }

    fun verifyDokarkivOppdaterRequest(journalpostId: Long, contains: String?) {
        WireMock.verify(
            WireMock.putRequestedFor(WireMock.urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId))
                .withRequestBody(ContainsPattern(contains))
        )
    }

    fun verifyDokarkivFerdigstillRequested(journalpostId: Long){
        WireMock.verify(WireMock.patchRequestedFor(
            WireMock.urlMatching(
                "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
            )
        ))
    }

    fun verifyDokarkivProxyTilknyttSakerRequested(journalpostId: Long, vararg contains: String){
        val verify = WireMock.putRequestedFor(
            WireMock.urlMatching(
                "/dokarkivproxy" + DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK.format(journalpostId)
            )
        )
        contains.forEach { contain -> verify.withRequestBody(ContainsPattern(contain))}
        WireMock.verify(verify)
    }

    fun verifyDokarkivProxyTilknyttSakerNotRequested(journalpostId: Long, vararg contains: String){
        val verify = WireMock.putRequestedFor(
            WireMock.urlMatching(
                "/dokarkivproxy" + DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK.format(journalpostId)
            )
        )
        contains.forEach { contain -> verify.withRequestBody(ContainsPattern(contain))}
        WireMock.verify(0, verify)
    }

    fun verifyDokarkivFerdigstillNotRequested(journalpostId: Long){
        WireMock.verify(0, WireMock.patchRequestedFor(
            WireMock.urlMatching(
                "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
            )
        ))
    }

    fun verifyDokarkivFeilregistrerRequest(path: String, journalpostId: Long) {
        WireMock.verify(
            WireMock.patchRequestedFor(WireMock.urlMatching("/dokarkiv" + String.format(
                DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
                journalpostId
            ) + "/" + path))
        )
    }

    fun verifySafHentJournalpostRequested(){
        WireMock.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/")).withRequestBody(ContainsPattern("query journalpost"))
        )
    }

    fun verifySafDokumentOversiktFagsakRequested(){
        WireMock.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/")).withRequestBody(ContainsPattern("query dokumentoversiktFagsak"))
        )
    }

    fun verifySafTilknyttedeJournalpostedRequested(){
        WireMock.verify(
            WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/")).withRequestBody(ContainsPattern("query tilknyttedeJournalposter"))
        )
    }

    fun verifySafTilknyttedeJournalpostedNotRequested(){
        WireMock.verify(0,
            WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/")).withRequestBody(ContainsPattern("query tilknyttedeJournalposter"))
        )
    }

    fun getDokarkivOppdaterRequests(journalpostId: Long): List<LoggedRequest?>? {
        return WireMock.findAll(WireMock.putRequestedFor(WireMock.urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId)))
    }

    @Throws(JsonProcessingException::class)
    fun mockDokarkivOppdaterRequest(journalpostId: Long, status: HttpStatus) {
        WireMock.stubFor(
            WireMock.put(WireMock.urlMatching("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId)).willReturn(
                WireMock.aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .withStatus(status.value())
                    .withBody(ObjectMapper().writeValueAsString(OppdaterJournalpostResponse(journalpostId)))
            )
        )
    }

    fun mockDokarkivFeilregistrerRequest(path: String, journalpostId: Long?) {
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
        )
    }

    fun mockDokarkivFerdigstillRequest(journalpostId: Long?) {
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
        )
    }

    fun mockDokarkivProxyTilknyttRequest(journalpostId: Long?) {
        WireMock.stubFor(
            WireMock.put(
                WireMock.urlMatching(
                    "/dokarkivproxy" + DokarkivProxyConsumer.URL_KNYTT_TIL_ANNEN_SAK.format(journalpostId)
                )
            ).willReturn(
                WireMock.aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .withStatus(HttpStatus.OK.value())
            )
        )
    }

    @Throws(IOException::class)
    fun mockSafResponseHentJournalpost(status: HttpStatus) {
        mockSafResponseHentJournalpost("journalpostSafResponse.json", status)
    }

    @Throws(IOException::class)
    fun mockSafResponseHentJournalpost(filename: String, status: HttpStatus) {
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/saf/")).withRequestBody(ContainsPattern("query journalpost")).willReturn(
                WireMock.aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .withStatus(status.value())
                    .withBodyFile("json/$filename")
            )
        )
    }

    @Throws(IOException::class)
    fun mockSafResponseTilknyttedeJournalposter(status: HttpStatus) {
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/saf/"))
                .withRequestBody(ContainsPattern("query tilknyttedeJournalposter")).willReturn(
                    WireMock.aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withStatus(status.value())
                        .withBodyFile("json/tilknyttedeJournalposter.json")
                )
        )
    }

    @Throws(IOException::class)
    fun mockSafResponseDokumentOversiktFagsak(status: HttpStatus) {
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/saf/"))
                .withRequestBody(ContainsPattern("query dokumentoversiktFagsak")).willReturn(
                    WireMock.aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withStatus(status.value())
                        .withBodyFile("json/dokumentoversiktFagsakQueryResponse.json")
                )
        )
    }

    fun verifyPersonRequested() {
            WireMock.verify(
                WireMock.getRequestedFor(WireMock.urlMatching("/person/.*"))
            )
    }

    fun mockPersonResponse(personResponse: PersonResponse?, status: HttpStatus) {
        try {
            WireMock.stubFor(
                WireMock.get(WireMock.urlMatching("/person/.*")).willReturn(
                    WireMock.aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withStatus(status.value())
                        .withBody(ObjectMapper().writeValueAsString(personResponse))
                )
            )
        } catch (e: JsonProcessingException) {
            fail(e.message)
        }
    }
}