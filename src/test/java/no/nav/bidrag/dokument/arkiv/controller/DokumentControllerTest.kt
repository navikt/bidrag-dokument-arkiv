package no.nav.bidrag.dokument.arkiv.controller

import io.kotest.matchers.shouldBe
import no.nav.bidrag.dokument.arkiv.dto.Dokument
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_ID
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_TITTEL
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_2_ID
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_FIL
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.dto.DokumentArkivSystemDto
import no.nav.bidrag.dokument.dto.DokumentFormatDto
import no.nav.bidrag.dokument.dto.DokumentMetadata
import no.nav.bidrag.dokument.dto.DokumentStatusDto
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.util.UriComponentsBuilder

class DokumentControllerTest : AbstractControllerTest() {

    @Test
    fun `skal hente dokument`() {
        val journalpostId = 201028011L

        stubs.mockSafHentDokumentResponse()

        val response = httpHeaderTestRestTemplate.getForEntity<ByteArray>(
            UriComponentsBuilder.fromUriString(initUrl()).pathSegment("dokument", "JOARK-$journalpostId/$DOKUMENT_1_ID").build().toUri(),
            null
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe DOKUMENT_FIL.toByteArray()
    }

    @Test
    fun `skal hente dokumentmetadata for et dokument`() {
        val journalpostId = 201028011L
        val safResponse = opprettSafResponse(
            journalpostId = journalpostId.toString(),
            dokumenter = listOf(
                Dokument(
                    dokumentInfoId = DOKUMENT_1_ID,
                    tittel = DOKUMENT_1_TITTEL
                ),
                Dokument(
                    dokumentInfoId = DOKUMENT_2_ID,
                    tittel = DOKUMENT_1_TITTEL
                )
            )
        )

        stubs.mockSafResponseTilknyttedeJournalposter(listOf(TilknyttetJournalpost(journalpostId, JournalStatus.FERDIGSTILT, Sak("123"))))
        stubs.mockSafHentDokumentResponse()
        stubs.mockSafResponseHentJournalpost(safResponse, journalpostId)

        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/dokument/JOARK-$journalpostId/$DOKUMENT_1_ID",
            HttpMethod.OPTIONS,
            null,
            object : ParameterizedTypeReference<List<DokumentMetadata>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body.size shouldBe 1

        val dokumentmetadata = response.body[0]
        dokumentmetadata.dokumentreferanse shouldBe DOKUMENT_1_ID
        dokumentmetadata.journalpostId shouldBe "JOARK-$journalpostId"
        dokumentmetadata.arkivsystem shouldBe DokumentArkivSystemDto.JOARK
        dokumentmetadata.format shouldBe DokumentFormatDto.PDF
        dokumentmetadata.status shouldBe DokumentStatusDto.FERDIGSTILT
    }

    @Test
    fun `skal hente dokumentmetadata for et dokument med referanse`() {
        val journalpostId = 201028011L
        val safResponse = opprettSafResponse(
            journalpostId = journalpostId.toString(),
            dokumenter = listOf(
                Dokument(
                    dokumentInfoId = DOKUMENT_1_ID,
                    tittel = DOKUMENT_1_TITTEL
                ),
                Dokument(
                    dokumentInfoId = DOKUMENT_2_ID,
                    tittel = DOKUMENT_1_TITTEL
                )
            )
        )

        stubs.mockSafResponseTilknyttedeJournalposter(listOf(TilknyttetJournalpost(journalpostId, JournalStatus.FERDIGSTILT, Sak("123"))))
        stubs.mockSafHentDokumentResponse()
        stubs.mockSafResponseHentJournalpost(safResponse, journalpostId)

        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/dokumentreferanse/$DOKUMENT_1_ID",
            HttpMethod.OPTIONS,
            null,
            object : ParameterizedTypeReference<List<DokumentMetadata>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body.size shouldBe 1

        val dokumentmetadata = response.body[0]
        dokumentmetadata.dokumentreferanse shouldBe DOKUMENT_1_ID
        dokumentmetadata.journalpostId shouldBe "JOARK-$journalpostId"
        dokumentmetadata.arkivsystem shouldBe DokumentArkivSystemDto.JOARK
        dokumentmetadata.format shouldBe DokumentFormatDto.PDF
        dokumentmetadata.status shouldBe DokumentStatusDto.FERDIGSTILT
    }

    @Test
    fun `skal hente dokumentmetadata for journalpost med flere dokumenter`() {
        val journalpostId = 201028011L
        val safResponse = opprettSafResponse(
            journalpostId = journalpostId.toString(),
            dokumenter = listOf(
                Dokument(
                    dokumentInfoId = DOKUMENT_1_ID,
                    tittel = DOKUMENT_1_TITTEL
                ),
                Dokument(
                    dokumentInfoId = DOKUMENT_2_ID,
                    tittel = DOKUMENT_1_TITTEL
                )
            )
        )

        stubs.mockSafResponseTilknyttedeJournalposter(listOf(TilknyttetJournalpost(journalpostId, JournalStatus.FERDIGSTILT, Sak("123"))))
        stubs.mockSafHentDokumentResponse()
        stubs.mockSafResponseHentJournalpost(safResponse, journalpostId)

        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/dokument/JOARK-$journalpostId",
            HttpMethod.OPTIONS,
            null,
            object : ParameterizedTypeReference<List<DokumentMetadata>>() {}
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body.size shouldBe 2

        val dokumentmetadata = response.body[0]
        dokumentmetadata.dokumentreferanse shouldBe DOKUMENT_1_ID
        dokumentmetadata.journalpostId shouldBe "JOARK-$journalpostId"
        dokumentmetadata.arkivsystem shouldBe DokumentArkivSystemDto.JOARK
        dokumentmetadata.format shouldBe DokumentFormatDto.PDF
        dokumentmetadata.status shouldBe DokumentStatusDto.FERDIGSTILT

        val dokumentmetadata2 = response.body[1]
        dokumentmetadata2.dokumentreferanse shouldBe DOKUMENT_2_ID
        dokumentmetadata2.journalpostId shouldBe "JOARK-$journalpostId"
        dokumentmetadata2.arkivsystem shouldBe DokumentArkivSystemDto.JOARK
        dokumentmetadata2.format shouldBe DokumentFormatDto.PDF
        dokumentmetadata2.status shouldBe DokumentStatusDto.FERDIGSTILT
    }
}
