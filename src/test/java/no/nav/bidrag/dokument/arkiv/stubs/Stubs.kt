package no.nav.bidrag.dokument.arkiv.stubs

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import com.github.tomakehurst.wiremock.stubbing.Scenario
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivKnyttTilSakConsumer
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostResponse
import no.nav.bidrag.dokument.arkiv.dto.GeografiskTilknytningResponse
import no.nav.bidrag.dokument.arkiv.dto.HentPostadresseResponse
import no.nav.bidrag.dokument.arkiv.dto.JoarkOpprettJournalpostResponse
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal
import no.nav.bidrag.dokument.arkiv.dto.KnyttTilAnnenSakResponse
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse
import no.nav.bidrag.dokument.arkiv.dto.OppgaveSokResponse
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerInfoResponse
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import org.junit.Assert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.util.Arrays

@Component
class Stubs(@field:Autowired private val objectMapper: ObjectMapper) {
    val verifyStub = VerifyStub()
    private fun aClosedJsonResponse(): ResponseDefinitionBuilder {
        return WireMock.aResponse()
            .withHeader(HttpHeaders.CONNECTION, "close")
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
    }

    fun mockDokarkivTilknyttRequest(journalpostId: Long) {
        mockDokarkivTilknyttRequest(journalpostId, 123213213L)
    }

    fun mockDokarkivTilknyttRequest(journalpostId: Long, nyJournalpostId: Long = 123213213L, status: HttpStatus = HttpStatus.OK) {
        try {
            WireMock.stubFor(
                WireMock.put(
                    WireMock.urlMatching(
                        "/dokarkivknytt" + String.format(DokarkivKnyttTilSakConsumer.URL_KNYTT_TIL_ANNEN_SAK, journalpostId)
                    )
                ).willReturn(
                    aClosedJsonResponse()
                        .withStatus(status.value())
                        .withBody(objectMapper.writeValueAsString(KnyttTilAnnenSakResponse(nyJournalpostId.toString())))
                )
            )
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }
    @JvmOverloads
    fun mockOrganisasjonGeografiskTilknytning(enhetId: String? = BRUKER_ENHET) {
        try {
            WireMock.stubFor(
                WireMock.get(WireMock.urlPathMatching("/organisasjon/bidrag-organisasjon/arbeidsfordeling/enhetsliste/geografisktilknytning/.*"))
                    .willReturn(
                        aClosedJsonResponse()
                            .withStatus(HttpStatus.OK.value())
                            .withBody(objectMapper.writeValueAsString(GeografiskTilknytningResponse(enhetId!!, "navn")))
                    )
            )
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }

    fun mockBidragOrganisasjonSaksbehandler() {
        try {
            WireMock.stubFor(
                WireMock.get(WireMock.urlPathMatching("/organisasjon/bidrag-organisasjon/saksbehandler/info/.*")).willReturn(
                    aClosedJsonResponse()
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
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(
                        """{
                            "access_token": "DUMMY",
                            "expiresIn": 3600,
                            "idToken": "DUMMY",
                            "scope": "openid",
                            "token_type": "Bearer"
                        }"""
                    )
            )
        )
    }

    fun mockDokdistFordelingRequest(status: HttpStatus, bestillingId: String?) {
        try {
            WireMock.stubFor(
                WireMock.post(WireMock.urlMatching("/dokdistfordeling/rest/v1/distribuerjournalpost")).willReturn(
                    aClosedJsonResponse()
                        .withStatus(status.value())
                        .withBody(objectMapper.writeValueAsString(DokDistDistribuerJournalpostResponse(bestillingId!!)))
                )
            )
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }

    @Throws(JsonProcessingException::class)
    fun mockSafHentDokumentResponse() {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/saf/rest/hentdokument/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(DOKUMENT_FIL)
            )
        )
    }

    @Throws(JsonProcessingException::class)
    fun mockDokarkivOpprettRequest(nyJournalpostId: Long?, status: HttpStatus) {
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "?forsoekFerdigstill=true")).willReturn(
                aClosedJsonResponse()
                    .withStatus(status.value())
                    .withBody(
                        objectMapper.writeValueAsString(
                            JoarkOpprettJournalpostResponse(
                                nyJournalpostId,
                                "FERDIGTILT",
                                null,
                                true,
                                ArrayList()
                            )
                        )
                    )
            )
        )
    }

    @JvmOverloads
    @Throws(JsonProcessingException::class)
    fun mockDokarkivOppdaterRequest(journalpostId: Long, status: HttpStatus = HttpStatus.OK) {
        WireMock.stubFor(
            WireMock.put(WireMock.urlMatching("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId)).willReturn(
                aClosedJsonResponse()
                    .withStatus(status.value())
                    .withBody(objectMapper.writeValueAsString(OppdaterJournalpostResponse(journalpostId)))
            )
        )
    }

    fun mockDokarkivOpphevFeilregistrerRequest(journalpostId: Long?) {
        WireMock.stubFor(
            WireMock.patch(
                WireMock.urlEqualTo(
                    "/dokarkiv" + String.format(
                        DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
                        journalpostId
                    ) + "/opphevFeilregistrertSakstilknytning"
                )
            ).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
            )
        )
    }

    fun mockDokarkivFeilregistrerRequest(journalpostId: Long?) {
        WireMock.stubFor(
            WireMock.patch(
                WireMock.urlEqualTo(
                    "/dokarkiv" + String.format(
                        DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
                        journalpostId
                    ) + "/feilregistrerSakstilknytning"
                )
            ).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
            )
        )
    }

    fun mockDokarkivOppdaterDistribusjonsInfoRequest(journalpostId: Long) {
        WireMock.stubFor(
            WireMock.patch(
                WireMock.urlMatching(
                    "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/oppdaterDistribusjonsinfo"
                )
            ).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
            )
        )
    }

    fun mockDokarkivFerdigstillRequest(journalpostId: Long) {
        WireMock.stubFor(
            WireMock.patch(
                WireMock.urlMatching(
                    "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
                )
            ).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
            )
        )
    }

    fun mockSafResponseHentJournalpost(status: HttpStatus) {
        mockSafResponseHentJournalpost("journalpostSafResponse.json", status)
    }

    fun mockSafResponseHentJournalpost(journalpost: Journalpost?, journalpostId: Long?) {
        try {
            WireMock.stubFor(
                WireMock.post(WireMock.urlEqualTo("/saf/graphql"))
                    .withRequestBody(ContainsPattern("query journalpost"))
                    .withRequestBody(ContainsPattern(String.format("\"variables\":{\"journalpostId\":$journalpostId}")))
                    .willReturn(
                        aClosedJsonResponse()
                            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                            .withStatus(HttpStatus.OK.value())
                            .withBody("{\"data\":{\"journalpost\": ${objectMapper.writeValueAsString(journalpost)} }}")
            ))
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }

    @JvmOverloads
    fun mockSafResponseHentJournalpost(journalpost: Journalpost?, scenarioState: String? = null, nextScenario: String? = null) {
        try {
            WireMock.stubFor(
                WireMock.post(WireMock.urlEqualTo("/saf/graphql"))
                    .inScenario("Saf response")
                    .whenScenarioStateIs(scenarioState ?: Scenario.STARTED)
                    .withRequestBody(ContainsPattern("query journalpost")).willReturn(
                        aClosedJsonResponse()
                            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                            .withStatus(HttpStatus.OK.value())
                            .withBody("{\"data\":{\"journalpost\": %s }}".formatted(objectMapper.writeValueAsString(journalpost)))
                    ).willSetStateTo(nextScenario)
            )
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }

    fun mockSafResponseHentJournalpost(filename: String, status: HttpStatus) {
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/saf/graphql"))
                .withRequestBody(ContainsPattern("query journalpost"))
                .willReturn(
                    aClosedJsonResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withStatus(status.value())
                        .withBodyFile("json/$filename")
                )
        )
    }

    fun mockSafResponseTilknyttedeJournalposter(httpStatus: HttpStatus) {
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/saf/graphql"))
                .withRequestBody(ContainsPattern("query tilknyttedeJournalposter")).willReturn(
                    aClosedJsonResponse()
                        .withStatus(httpStatus.value())
                        .withBodyFile("json/tilknyttedeJournalposter.json")
                )
        )
    }

    fun mockSafResponseTilknyttedeJournalposter(tilknyttetJournalposts: List<TilknyttetJournalpost?>?) {
        try {
            WireMock.stubFor(
                WireMock.post(WireMock.urlEqualTo("/saf/graphql"))
                    .withRequestBody(ContainsPattern("query tilknyttedeJournalposter")).willReturn(
                        aClosedJsonResponse()
                            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                            .withStatus(HttpStatus.OK.value())
                            .withBody(
                                "{\"data\":{\"tilknyttedeJournalposter\": %s }}".formatted(
                                    objectMapper.writeValueAsString(
                                        tilknyttetJournalposts
                                    )
                                )
                            )
                    )
            )
        } catch (e: Exception) {
            Assert.fail(e.message)
        }
    }

    @JvmOverloads
    fun mockSafResponseDokumentOversiktFagsak(response: List<Journalpost?>? = opprettDokumentOversiktfagsakResponse()) {
        try {
            WireMock.stubFor(
                WireMock.post(WireMock.urlEqualTo("/saf/graphql"))
                    .withRequestBody(ContainsPattern("query dokumentoversiktFagsak")).willReturn(
                        aClosedJsonResponse()
                            .withStatus(HttpStatus.OK.value())
                            .withBody(
                                "{\"data\":{\"dokumentoversiktFagsak\":{\"journalposter\": %s }}}".formatted(
                                    objectMapper.writeValueAsString(
                                        response
                                    )
                                )
                            )
                    )
            )
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }

    fun mockSokOppgave() {
        try {
            WireMock.stubFor(
                WireMock.get(WireMock.urlMatching("/oppgave/.*")).willReturn(
                    aClosedJsonResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(objectMapper.writeValueAsString(OppgaveSokResponse()))
                )
            )
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }

    fun mockSokOppgave(oppgaveSokResponse: OppgaveSokResponse?, status: HttpStatus) {
        try {
            WireMock.stubFor(
                WireMock.get(WireMock.urlMatching("/oppgave/.*")).willReturn(
                    aClosedJsonResponse()
                        .withStatus(status.value())
                        .withBody(objectMapper.writeValueAsString(oppgaveSokResponse))
                )
            )
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }

    fun mockOpprettOppgave(status: HttpStatus) {
        WireMock.stubFor(
            WireMock.post(WireMock.urlMatching("/oppgave/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(status.value())
            )
        )
    }

    fun mockOppdaterOppgave(status: HttpStatus) {
        WireMock.stubFor(
            WireMock.patch(WireMock.urlMatching("/oppgave/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(status.value())
            )
        )
    }

    fun mockPersonAdresseResponse(hentPostadresseResponse: HentPostadresseResponse?) {
        try {
            WireMock.stubFor(
                WireMock.post(WireMock.urlMatching("/person/bidrag-person/adresse/post")).willReturn(
                    aClosedJsonResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(
                            ObjectMapper().writeValueAsString(
                                hentPostadresseResponse ?: HentPostadresseResponse(
                                    "Ramsegata 1",
                                    "Bakredør",
                                    null,
                                    "3939",
                                    "OSLO",
                                    "NO"
                                )
                            )
                        )
                )
            )
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }

    fun mockPersonResponse(personResponse: PersonResponse?, status: HttpStatus) {
        try {
            WireMock.stubFor(
                WireMock.get(WireMock.urlMatching("/person/.*")).willReturn(
                    aClosedJsonResponse()
                        .withStatus(status.value())
                        .withBody(ObjectMapper().writeValueAsString(personResponse))
                )
            )
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
        }
    }

    class VerifyStub {
        fun hentPersonAdresseKalt(personId: String?) {
            val requestPattern = WireMock.postRequestedFor(WireMock.urlMatching("/person/bidrag-person/adresse/post"))
            requestPattern.withRequestBody(ContainsPattern(personId))
            WireMock.verify(requestPattern)
        }

        @JvmOverloads
        fun bidragOrganisasjonGeografiskTilknytningKalt(ident: String? = null) {
            WireMock.verify(
                WireMock.getRequestedFor(
                    WireMock.urlPathMatching(
                        String.format(
                            "/organisasjon/bidrag-organisasjon/arbeidsfordeling/enhetsliste/geografisktilknytning/%s",
                            ident ?: ".*"
                        )
                    )
                )
            )
        }

        fun dokarkivTilknyttSakerKalt(times: Int, journalpostId: Long, vararg contains: String?) {
            val verify = WireMock.putRequestedFor(
                WireMock.urlMatching(
                    "/dokarkivknytt" + String.format(DokarkivKnyttTilSakConsumer.URL_KNYTT_TIL_ANNEN_SAK, journalpostId)
                )
            )
            Arrays.stream(contains).forEach { contain: String? ->
                verify.withRequestBody(
                    ContainsPattern(contain)
                )
            }
            WireMock.verify(WireMock.exactly(times), verify)
        }

        fun dokarkivTilknyttSakerIkkeKalt(journalpostId: Long, vararg contains: String?) {
            dokarkivTilknyttSakerKalt(0, journalpostId, *contains)
        }

        fun dokarkivTilknyttSakerKalt(journalpostId: Long, vararg contains: String?) {
            dokarkivTilknyttSakerKalt(1, journalpostId, *contains)
        }
        fun oppgaveOpprettKalt(vararg contains: String?) {
            val requestPattern = WireMock.postRequestedFor(WireMock.urlMatching("/oppgave/.*"))
            Arrays.stream(contains).forEach { contain: String? -> requestPattern.withRequestBody(ContainsPattern(contain)) }
            WireMock.verify(requestPattern)
        }

        fun oppgaveOppdaterKalt(count: Int?, vararg contains: String?) {
            val requestPattern = WireMock.patchRequestedFor(WireMock.urlMatching("/oppgave/.*"))
            Arrays.stream(contains).forEach { contain: String? -> requestPattern.withRequestBody(ContainsPattern(contain)) }
            WireMock.verify(count!!, requestPattern)
        }

        fun oppgaveSokIkkeKalt() {
            WireMock.verify(0, WireMock.getRequestedFor(WireMock.urlMatching("/oppgave/.*")))
        }

        fun oppgaveOpprettIkkeKalt() {
            WireMock.verify(0, WireMock.postRequestedFor(WireMock.urlMatching("/oppgave/.*")))
        }

        fun oppgaveSokKalt(vararg params: Pair<String, String>) {
            val requestPattern = WireMock.getRequestedFor(WireMock.urlMatching("/oppgave/.*"))
            Arrays.stream(params).forEach { (first, second): Pair<String?, String?> ->
                requestPattern.withQueryParam(
                    first, ContainsPattern(second)
                )
            }
            WireMock.verify(requestPattern)
        }

        fun dokarkivOppdaterDistribusjonsInfoKalt(journalpostId: Long, vararg contains: String?) {
            val requestPattern =
                WireMock.putRequestedFor(WireMock.urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId + "/oppdaterDistribusjonsinfo"))
            Arrays.stream(contains).forEach { contain: String? -> requestPattern.withRequestBody(ContainsPattern(contain)) }
            WireMock.verify(requestPattern)
        }

        fun dokarkivOppdaterIkkeKalt(journalpostId: Long) {
            WireMock.verify(
                0,
                WireMock.putRequestedFor(WireMock.urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId))
            )
        }

        fun dokarkivOppdaterKalt(journalpostId: Long, vararg contains: String?) {
            val requestPattern =
                WireMock.putRequestedFor(WireMock.urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + '/' + journalpostId))
            Arrays.stream(contains).forEach { contain: String? -> requestPattern.withRequestBody(ContainsPattern(contain)) }
            WireMock.verify(requestPattern)
        }

        fun safHentDokumentKalt(journalpostId: Long?, dokumentId: Long?) {
            val requestPattern =
                WireMock.getRequestedFor(WireMock.urlEqualTo(String.format("/saf/rest/hentdokument/%s/%s/ARKIV", journalpostId, dokumentId)))
            WireMock.verify(requestPattern)
        }

        fun dokarkivOpprettKalt(vararg contains: String?) {
            val requestPattern =
                WireMock.postRequestedFor(WireMock.urlEqualTo("/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "?forsoekFerdigstill=true"))
            Arrays.stream(contains).forEach { contain: String? -> requestPattern.withRequestBody(ContainsPattern(contain)) }
            WireMock.verify(requestPattern)
        }

        fun bidragPersonKalt() {
            WireMock.verify(
                WireMock.getRequestedFor(WireMock.urlMatching("/person/.*"))
            )
        }

        fun bidragPersonIkkeKalt() {
            WireMock.verify(
                0,
                WireMock.getRequestedFor(WireMock.urlMatching("/person/.*"))
            )
        }

        fun dokdistFordelingKalt(vararg contains: String?) {
            val requestPattern = WireMock.postRequestedFor(WireMock.urlMatching("/dokdistfordeling/.*"))
            Arrays.stream(contains).forEach { contain: String? -> requestPattern.withRequestBody(ContainsPattern(contain)) }
            WireMock.verify(requestPattern)
        }

        fun dokdistFordelingIkkeKalt() {
            WireMock.verify(
                0,
                WireMock.postRequestedFor(WireMock.urlMatching("/dokdistfordeling/.*"))
            )
        }

        private fun dokarkivFerdigstillKalt(times: Int, journalpostId: Long) {
            WireMock.verify(
                WireMock.exactly(times), WireMock.patchRequestedFor(
                    WireMock.urlMatching(
                        "/dokarkiv" + DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" + journalpostId + "/ferdigstill"
                    )
                )
            )
        }

        fun dokarkivFerdigstillIkkeKalt(journalpostId: Long) {
            dokarkivFerdigstillKalt(0, journalpostId)
        }

        fun dokarkivFerdigstillKalt(journalpostId: Long) {
            dokarkivFerdigstillKalt(1, journalpostId)
        }

        fun dokarkivOpphevFeilregistrerIkkeKalt(journalpostId: Long?) {
            dokarkivOpphevFeilregistrerKalt(journalpostId)
        }

        fun dokarkivOpphevFeilregistrerKalt(journalpostId: Long?) {
            dokarkivOpphevFeilregistrerKalt(1, journalpostId)
        }

        fun dokarkivOpphevFeilregistrerKalt(count: Int?, journalpostId: Long?) {
            WireMock.verify(
                count!!,
                WireMock.patchRequestedFor(
                    WireMock.urlMatching(
                        "/dokarkiv" + String.format(
                            DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
                            journalpostId
                        ) + "/opphevFeilregistrertSakstilknytning"
                    )
                )
            )
        }

        fun dokarkivFeilregistrerKalt(journalpostId: Long?) {
            WireMock.verify(
                WireMock.patchRequestedFor(
                    WireMock.urlMatching(
                        "/dokarkiv" + String.format(
                            DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
                            journalpostId
                        ) + "/feilregistrerSakstilknytning"
                    )
                )
            )
        }

        fun dokarkivFeilregistrerIkkeKalt(journalpostId: Long?) {
            WireMock.verify(
                0,
                WireMock.patchRequestedFor(
                    WireMock.urlMatching(
                        "/dokarkiv" + String.format(
                            DokarkivConsumer.URL_JOURNALPOSTAPI_V1_FEILREGISTRER,
                            journalpostId
                        ) + "/feilregistrerSakstilknytning"
                    )
                )
            )
        }

        fun dokarkivOppdaterDistribusjonsInfoKalt(journalpostId: Long, kanal: JournalpostKanal) {
            WireMock.verify(
                WireMock.patchRequestedFor(
                    WireMock.urlMatching(
                        "/dokarkiv" +
                                DokarkivConsumer.URL_JOURNALPOSTAPI_V1 + "/" +
                                journalpostId + "/oppdaterDistribusjonsinfo"
                    )
                ).withRequestBody(ContainsPattern(kanal.name))
            )
        }

        fun harEnSafKallEtterHentJournalpost() {
            WireMock.verify(
                WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/graphql")).withRequestBody(ContainsPattern("query journalpost"))
            )
        }

        fun harSafKallEtterHentJournalpost(antall: Int?) {
            WireMock.verify(
                antall!!,
                WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/graphql")).withRequestBody(ContainsPattern("query journalpost"))
            )
        }

        fun harSafEnKallEtterDokumentOversiktFagsak() {
            WireMock.verify(
                WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/graphql")).withRequestBody(ContainsPattern("query dokumentoversiktFagsak"))
            )
        }

        fun harEnSafKallEtterTilknyttedeJournalposter() {
            harEnSafKallEtterTilknyttedeJournalposter(1)
        }

        fun harIkkeEnSafKallEtterTilknyttedeJournalposter() {
            harEnSafKallEtterTilknyttedeJournalposter(0)
        }

        private fun harEnSafKallEtterTilknyttedeJournalposter(times: Int) {
            WireMock.verify(
                WireMock.exactly(times),
                WireMock.postRequestedFor(WireMock.urlEqualTo("/saf/graphql")).withRequestBody(ContainsPattern("query tilknyttedeJournalposter"))
            )
        }
    }

    companion object {
        var SAKSNUMMER_JOURNALPOST = "5276661"
        var SAKSNUMMER_TILKNYTTET_1 = "2106585"
        var BRUKER_ENHET = "4899"
        var SAKSNUMMER_TILKNYTTET_2 = "9999999"
    }
}