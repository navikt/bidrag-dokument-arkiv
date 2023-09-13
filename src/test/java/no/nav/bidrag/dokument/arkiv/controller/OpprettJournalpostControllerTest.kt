package no.nav.bidrag.dokument.arkiv.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.bidrag.dokument.arkiv.dto.DokumentInfo
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.stubs.BEHANDLINGSTEMA
import no.nav.bidrag.dokument.arkiv.stubs.DATO_MOTTATT
import no.nav.bidrag.dokument.arkiv.stubs.GJELDER_ID
import no.nav.bidrag.dokument.arkiv.stubs.REFID
import no.nav.bidrag.dokument.arkiv.stubs.TITTEL_HOVEDDOKUMENT
import no.nav.bidrag.dokument.arkiv.stubs.TITTEL_VEDLEGG1
import no.nav.bidrag.dokument.arkiv.stubs.createOpprettJournalpostRequest
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.dto.AvsenderMottakerDto
import no.nav.bidrag.dokument.dto.AvsenderMottakerDtoIdType
import no.nav.bidrag.dokument.dto.JournalpostType
import no.nav.bidrag.dokument.dto.OpprettDokumentDto
import no.nav.bidrag.dokument.dto.OpprettJournalpostResponse
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.util.*

internal class OpprettJournalpostControllerTest : AbstractControllerTest() {

    @Test
    fun `skal opprette inngående journalpost`() {
        val request = createOpprettJournalpostRequest()

        val nyJpId = 123123123L
        stubs.mockDokarkivOpprettRequest(
            nyJpId,
            ferdigstill = false,
            dokumentList = request.dokumenter.map { DokumentInfo("DOK_ID_${it.tittel}") }
        )

        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journalpost",
            HttpMethod.POST,
            HttpEntity(request),
            OpprettJournalpostResponse::class.java
        )

        response.statusCode shouldBe HttpStatus.OK

        val responseBody = response.body!!
        assertSoftly {
            responseBody.journalpostId shouldBe nyJpId.toString()
            responseBody.dokumenter shouldHaveSize 2
            stubs.verifyStub.dokarkivOpprettKalt(
                false,
                "{\"tittel\":\"$TITTEL_HOVEDDOKUMENT\"," +
                    "\"journalpostType\":\"INNGAAENDE\"," +
                    "\"behandlingstema\":\"$BEHANDLINGSTEMA\"," +
                    "\"eksternReferanseId\":\"$REFID\"," +
                    "\"tilleggsopplysninger\":[]," +
                    "\"tema\":\"BID\"," +
                    "\"kanal\":\"NAV_NO\"," +
                    "\"datoMottatt\":\"$DATO_MOTTATT\"," +
                    "\"bruker\":{\"id\":\"$GJELDER_ID\",\"idType\":\"FNR\"}," +
                    "\"dokumenter\":[" +
                    "{\"tittel\":\"$TITTEL_HOVEDDOKUMENT\"," +
                    "\"dokumentvarianter\":[{\"filtype\":\"PDFA\",\"variantformat\":\"ARKIV\",\"fysiskDokument\":\"SW5uaG9sZCBww6UgZG9rdW1lbnRldA==\"}]}," +
                    "{\"tittel\":\"$TITTEL_VEDLEGG1\"," +
                    "\"dokumentvarianter\":[{\"filtype\":\"PDFA\",\"variantformat\":\"ARKIV\",\"fysiskDokument\":\"SW5uaG9sZCBww6UgZG9rdW1lbnRldCB2ZWRsZWdn\"}]}]," +
                    "\"avsenderMottaker\":{\"id\":\"$GJELDER_ID\",\"idType\":\"FNR\"}}"
            )
        }
    }

    @Test
    fun `skal opprette og journalføre utgående journalpost`() {
        val saksnummer1 = "132213"
        val saksnummer2 = "1233333"
        val request = createOpprettJournalpostRequest()
            .copy(
                skalFerdigstilles = true,
                journalposttype = JournalpostType.UTGÅENDE,
                tilknyttSaker = listOf(saksnummer1, saksnummer2),
                journalførendeEnhet = "4806"
            )

        val nyJpId = 123123123L

        val journalpost = opprettSafResponse(nyJpId.toString()).copy(
            sak = Sak(saksnummer1)
        )

        stubs.mockSafResponseHentJournalpost(journalpost)
        stubs.mockDokarkivOppdaterRequest(nyJpId)
        stubs.mockDokarkivTilknyttRequest(nyJpId)
        stubs.mockDokarkivOpprettRequest(
            nyJpId,
            ferdigstill = true,
            dokumentList = request.dokumenter.map { DokumentInfo("DOK_ID_${it.tittel}") }
        )

        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journalpost",
            HttpMethod.POST,
            HttpEntity(request),
            OpprettJournalpostResponse::class.java
        )

        response.statusCode shouldBe HttpStatus.OK

        val responseBody = response.body!!
        assertSoftly {
            responseBody.journalpostId shouldBe nyJpId.toString()
            responseBody.dokumenter shouldHaveSize 2
            stubs.verifyStub.dokarkivOpprettKalt(
                true,
                "\"sak\":{\"fagsakId\":\"$saksnummer1\",\"fagsaksystem\":\"BISYS\",\"sakstype\":\"FAGSAK\"}",
                "\"tittel\":\"$TITTEL_HOVEDDOKUMENT\"",
                "\"journalfoerendeEnhet\":\"4806\"",
                "\"journalpostType\":\"UTGAAENDE\"",
                "\"avsenderMottaker\":{\"id\":\"12345678910\",\"idType\":\"FNR\"}}"
            )
            stubs.verifyStub.dokarkivTilknyttSakerKalt(1, nyJpId)
            stubs.verifyStub.dokarkivTilknyttSakerKalt(nyJpId, saksnummer2)
            stubs.verifyStub.dokarkivOppdaterKalt(nyJpId, "aud-localhost")
        }
    }

    @Test
    fun `skal opprette og journalføre utgående journalpost ved å hente dokumenter fra annen kilde`() {
        val saksnummer1 = "132213"
        val saksnummer2 = "1233333"
        val request = createOpprettJournalpostRequest()
            .copy(
                skalFerdigstilles = true,
                journalposttype = JournalpostType.UTGÅENDE,
                tilknyttSaker = listOf(saksnummer1, saksnummer2),
                journalførendeEnhet = "4806",
                dokumenter = listOf(
                    OpprettDokumentDto(
                        tittel = TITTEL_HOVEDDOKUMENT,
                        dokumentreferanse = "DOKREF1"
                    ),
                    OpprettDokumentDto(
                        tittel = TITTEL_VEDLEGG1,
                        dokumentreferanse = "DOKREF2"
                    )
                )
            )

        val nyJpId = 123123123L

        val journalpost = opprettSafResponse(nyJpId.toString()).copy(
            sak = Sak(saksnummer1)
        )
        stubs.mockBidragDokumentHentDokumentResponse("DOKREF1", "DOK1".toByteArray())
        stubs.mockBidragDokumentHentDokumentResponse("DOKREF2", "DOK2".toByteArray())
        stubs.mockSafResponseHentJournalpost(journalpost)
        stubs.mockDokarkivOppdaterRequest(nyJpId)
        stubs.mockDokarkivTilknyttRequest(nyJpId)
        stubs.mockDokarkivOpprettRequest(
            nyJpId,
            ferdigstill = true,
            dokumentList = request.dokumenter.map { DokumentInfo("DOK_ID_${it.tittel}") }
        )

        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journalpost",
            HttpMethod.POST,
            HttpEntity(request),
            OpprettJournalpostResponse::class.java
        )

        response.statusCode shouldBe HttpStatus.OK

        val responseBody = response.body!!
        assertSoftly {
            responseBody.journalpostId shouldBe nyJpId.toString()
            responseBody.dokumenter shouldHaveSize 2
            stubs.verifyStub.dokarkivOpprettKalt(
                true,
                "\"sak\":{\"fagsakId\":\"$saksnummer1\",\"fagsaksystem\":\"BISYS\",\"sakstype\":\"FAGSAK\"}",
                "\"tittel\":\"$TITTEL_HOVEDDOKUMENT\"",
                "\"journalfoerendeEnhet\":\"4806\"",
                "\"journalpostType\":\"UTGAAENDE\"",
                "\"avsenderMottaker\":{\"id\":\"12345678910\",\"idType\":\"FNR\"}}",
                "\"dokumentvarianter\":[{\"filtype\":\"PDFA\",\"variantformat\":\"ARKIV\",\"fysiskDokument\":\"${
                Base64.getEncoder().encodeToString("DOK1".toByteArray())
                }\"}]},{\"tittel\":\"Tittel på vedlegg\",\"dokumentvarianter\":[{\"filtype\":\"PDFA\",\"variantformat\":\"ARKIV\",\"fysiskDokument\":\"${
                Base64.getEncoder().encodeToString("DOK2".toByteArray())
                }\"}]}]"
            )
            stubs.verifyStub.dokarkivTilknyttSakerKalt(1, nyJpId)
            stubs.verifyStub.dokarkivTilknyttSakerKalt(nyJpId, saksnummer2)
            stubs.verifyStub.dokarkivOppdaterKalt(nyJpId, "aud-localhost")
            stubs.verifyStub.bidragDokumentHentKalt("DOKREF1")
            stubs.verifyStub.bidragDokumentHentKalt("DOKREF2")
        }
    }

    @Test
    fun `skal opprette og journalføre utgående journalpost med journalpost tittel`() {
        val saksnummer1 = "132213"
        val saksnummer2 = "1233333"
        val request = createOpprettJournalpostRequest()
            .copy(
                skalFerdigstilles = true,
                journalposttype = JournalpostType.UTGÅENDE,
                tilknyttSaker = listOf(saksnummer1, saksnummer2),
                journalførendeEnhet = "4806",
                tittel = "Journalpost tittel"
            )

        val nyJpId = 123123123L

        val journalpost = opprettSafResponse(nyJpId.toString()).copy(
            sak = Sak(saksnummer1)
        )

        stubs.mockSafResponseHentJournalpost(journalpost)
        stubs.mockDokarkivOppdaterRequest(nyJpId)
        stubs.mockDokarkivTilknyttRequest(nyJpId)
        stubs.mockDokarkivOpprettRequest(
            nyJpId,
            ferdigstill = true,
            dokumentList = request.dokumenter.map { DokumentInfo("DOK_ID_${it.tittel}") }
        )

        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journalpost",
            HttpMethod.POST,
            HttpEntity(request),
            OpprettJournalpostResponse::class.java
        )

        response.statusCode shouldBe HttpStatus.OK

        val responseBody = response.body!!
        assertSoftly {
            responseBody.journalpostId shouldBe nyJpId.toString()
            responseBody.dokumenter shouldHaveSize 2
            stubs.verifyStub.dokarkivOpprettKalt(
                true,
                "\"sak\":{\"fagsakId\":\"$saksnummer1\",\"fagsaksystem\":\"BISYS\",\"sakstype\":\"FAGSAK\"}",
                "\"tittel\":\"Journalpost tittel\"",
                "\"journalfoerendeEnhet\":\"4806\"",
                "\"journalpostType\":\"UTGAAENDE\"",
                "\"avsenderMottaker\":{\"id\":\"12345678910\",\"idType\":\"FNR\"}}"
            )
            stubs.verifyStub.dokarkivTilknyttSakerKalt(1, nyJpId)
            stubs.verifyStub.dokarkivTilknyttSakerKalt(nyJpId, saksnummer2)
            stubs.verifyStub.dokarkivOppdaterKalt(nyJpId, "aud-localhost")
        }
    }

    @Test
    fun `skal opprette og journalføre utgående journalpost med samhandlerId`() {
        val saksnummer1 = "132213"
        val samhandlerId = "80000213123"
        val request = createOpprettJournalpostRequest()
            .copy(
                skalFerdigstilles = true,
                avsenderMottaker = AvsenderMottakerDto(
                    ident = samhandlerId,
                    navn = "Samhandler navnesen",
                    type = AvsenderMottakerDtoIdType.SAMHANDLER
                ),
                journalposttype = JournalpostType.UTGÅENDE,
                tilknyttSaker = listOf(saksnummer1),
                journalførendeEnhet = "4806"
            )

        val nyJpId = 123123123L

        val journalpost = opprettSafResponse(nyJpId.toString()).copy(
            sak = Sak(saksnummer1)
        )

        stubs.mockSafResponseHentJournalpost(journalpost)
        stubs.mockDokarkivOppdaterRequest(nyJpId)
        stubs.mockDokarkivTilknyttRequest(nyJpId)
        stubs.mockDokarkivOpprettRequest(
            nyJpId,
            ferdigstill = true,
            dokumentList = request.dokumenter.map { DokumentInfo("DOK_ID_${it.tittel}") }
        )

        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journalpost",
            HttpMethod.POST,
            HttpEntity(request),
            OpprettJournalpostResponse::class.java
        )

        response.statusCode shouldBe HttpStatus.OK

        val responseBody = response.body!!
        assertSoftly {
            responseBody.journalpostId shouldBe nyJpId.toString()
            responseBody.dokumenter shouldHaveSize 2
            stubs.verifyStub.dokarkivOpprettKalt(
                true,
                "\"sak\":{\"fagsakId\":\"$saksnummer1\",\"fagsaksystem\":\"BISYS\",\"sakstype\":\"FAGSAK\"}",
                "\"tittel\":\"$TITTEL_HOVEDDOKUMENT\"",
                "\"journalfoerendeEnhet\":\"4806\"",
                "\"journalpostType\":\"UTGAAENDE\"",
                "\"avsenderMottaker\":{\"navn\":\"Samhandler navnesen\"}",
                "\"tilleggsopplysninger\":[{\"nokkel\":\"samhandlerId\",\"verdi\":\"$samhandlerId\"}]"
            )
            stubs.verifyStub.dokarkivOppdaterKalt(nyJpId, "aud-localhost")
        }
    }

    @Test
    fun `skal opprette og journalføre notat`() {
        val saksnummer1 = "132213"
        val saksnummer2 = "1233333"
        val request = createOpprettJournalpostRequest()
            .copy(
                skalFerdigstilles = true,
                journalposttype = JournalpostType.NOTAT,
                tilknyttSaker = listOf(saksnummer1, saksnummer2),
                datoDokument = LocalDateTime.parse("2022-01-05T10:52:03"),
                journalførendeEnhet = "4806"
            )

        val nyJpId = 123123123L

        val journalpost = opprettSafResponse(nyJpId.toString()).copy(
            sak = Sak(saksnummer1)
        )
        stubs.mockSafResponseHentJournalpost(journalpost)
        stubs.mockDokarkivOppdaterRequest(nyJpId)
        stubs.mockDokarkivTilknyttRequest(nyJpId)
        stubs.mockDokarkivOpprettRequest(
            nyJpId,
            ferdigstill = true,
            dokumentList = request.dokumenter.map { DokumentInfo("DOK_ID_${it.tittel}") }
        )

        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journalpost",
            HttpMethod.POST,
            HttpEntity(request),
            OpprettJournalpostResponse::class.java
        )

        response.statusCode shouldBe HttpStatus.OK

        val responseBody = response.body!!
        assertSoftly {
            responseBody.journalpostId shouldBe nyJpId.toString()
            responseBody.dokumenter shouldHaveSize 2
            stubs.verifyStub.dokarkivOpprettKalt(
                true,
                "{\"sak\":{\"fagsakId\":\"132213\",\"fagsaksystem\":\"BISYS\",\"sakstype\":\"FAGSAK\"}",
                "\"tittel\":\"Tittel på hoveddokument\"",
                "\"datoDokument\":\"2022-01-05T10:52:03\"",
                "\"journalpostType\":\"NOTAT\"",
                "\"bruker\":{\"id\":\"12345678910\",\"idType\":\"FNR\"}"
            )
            stubs.verifyStub.dokarkivOpprettKaltNotContains(true, "avsenderMottaker")
            stubs.verifyStub.dokarkivTilknyttSakerKalt(1, nyJpId)
            stubs.verifyStub.dokarkivTilknyttSakerKalt(nyJpId, saksnummer2)
            stubs.verifyStub.dokarkivOppdaterKalt(nyJpId, "aud-localhost")
        }
    }

    @Test
    fun `skal opprette og journalføre notat med saksbehandlerident`() {
        val saksnummer1 = "132213"
        val saksnummer2 = "1233333"
        val request = createOpprettJournalpostRequest()
            .copy(
                skalFerdigstilles = true,
                journalposttype = JournalpostType.NOTAT,
                tilknyttSaker = listOf(saksnummer1, saksnummer2),
                journalførendeEnhet = "4806",
                saksbehandlerIdent = "Z9494124"
            )

        val nyJpId = 123123123L

        val journalpost = opprettSafResponse(nyJpId.toString()).copy(
            sak = Sak(saksnummer1)
        )
        stubs.mockBidragOrganisasjonSaksbehandler(navn = "Hansen, Hans")
        stubs.mockSafResponseHentJournalpost(journalpost)
        stubs.mockDokarkivOppdaterRequest(nyJpId)
        stubs.mockDokarkivFerdigstillRequest(nyJpId)
        stubs.mockDokarkivTilknyttRequest(nyJpId)
        stubs.mockDokarkivOpprettRequest(
            nyJpId,
            ferdigstill = false,
            dokumentList = request.dokumenter.map { DokumentInfo("DOK_ID_${it.tittel}") }
        )

        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journalpost",
            HttpMethod.POST,
            HttpEntity(request),
            OpprettJournalpostResponse::class.java
        )

        response.statusCode shouldBe HttpStatus.OK

        val responseBody = response.body!!
        assertSoftly {
            responseBody.journalpostId shouldBe nyJpId.toString()
            responseBody.dokumenter shouldHaveSize 2
            stubs.verifyStub.dokarkivOpprettKalt(
                false,
                "{\"sak\":{\"fagsakId\":\"132213\",\"fagsaksystem\":\"BISYS\",\"sakstype\":\"FAGSAK\"}",
                "\"tittel\":\"Tittel på hoveddokument\"",
                "\"journalpostType\":\"NOTAT\"",
                "\"bruker\":{\"id\":\"12345678910\",\"idType\":\"FNR\"}"
            )
            stubs.verifyStub.dokarkivFerdigstillKalt(
                1,
                nyJpId,
                "{\"journalfoerendeEnhet\":\"4806\"," +
                    "\"journalfortAvNavn\":\"Hansen, Hans\"," +
                    "\"opprettetAvNavn\":\"Hansen, Hans\"," +
                    "\"datoJournal\":null}"
            )
            stubs.verifyStub.dokarkivOpprettKaltNotContains(false, "avsenderMottaker")
            stubs.verifyStub.dokarkivTilknyttSakerKalt(1, nyJpId)
            stubs.verifyStub.dokarkivTilknyttSakerKalt(nyJpId, saksnummer2)
            stubs.verifyStub.dokarkivOppdaterKalt(nyJpId, "Z9494124")
        }
    }

    @Nested
    inner class Feilhåndtering {
        @Test
        fun `skal feile hvis journalpost opprettet uten journalposttype`() {
            val request = createOpprettJournalpostRequest().copy(journalposttype = null)

            val nyJpId = 123123123L
            stubs.mockDokarkivOpprettRequest(
                nyJpId,
                ferdigstill = false,
                dokumentList = request.dokumenter.map { DokumentInfo("DOK_ID_${it.tittel}") }
            )

            val response = httpHeaderTestRestTemplate.exchange(
                initUrl() + "/journalpost",
                HttpMethod.POST,
                HttpEntity(request),
                OpprettJournalpostResponse::class.java
            )

            response.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }
}
