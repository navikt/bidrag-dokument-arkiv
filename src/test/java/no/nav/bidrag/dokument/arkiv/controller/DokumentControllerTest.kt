package no.nav.bidrag.dokument.arkiv.controller


import io.kotest.matchers.shouldBe
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.Dokument
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal
import no.nav.bidrag.dokument.arkiv.dto.OppgaveEnhet
import no.nav.bidrag.dokument.arkiv.dto.OppgaveSokResponse
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.stubs.DATO_DOKUMENT
import no.nav.bidrag.dokument.arkiv.stubs.DATO_REGISTRERT
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_ID
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_TITTEL
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_2_ID
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_FIL
import no.nav.bidrag.dokument.arkiv.stubs.JOURNALPOST_ID
import no.nav.bidrag.dokument.arkiv.stubs.JOURNALPOST_ID_3
import no.nav.bidrag.dokument.arkiv.stubs.NY_JOURNALPOST_ID_KNYTT_TIL_SAK
import no.nav.bidrag.dokument.arkiv.stubs.createOppgaveDataWithJournalpostId
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeDistribuertSafResponse
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.dokument.dto.AvvikType
import no.nav.bidrag.dokument.dto.Avvikshendelse
import no.nav.bidrag.dokument.dto.BehandleAvvikshendelseResponse
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse
import no.nav.bidrag.dokument.dto.DokumentArkivSystemDto
import no.nav.bidrag.dokument.dto.DokumentDto
import no.nav.bidrag.dokument.dto.DokumentFormatDto
import no.nav.bidrag.dokument.dto.DokumentMetadata
import no.nav.bidrag.dokument.dto.DokumentStatusDto
import no.nav.bidrag.dokument.dto.JournalpostDto
import org.assertj.core.api.Assertions
import org.json.JSONException
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.IOException
import java.util.Base64

class DokumentControllerTest : AbstractControllerTest() {

    @Test
    fun `skal hente dokument`() {
        val journalpostId = 201028011L

        stubs.mockSafHentDokumentResponse()

        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/dokument/JOARK-$journalpostId/$DOKUMENT_1_ID",
            HttpMethod.GET,
            null,
            ByteArray::class.java
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
                    tittel = DOKUMENT_1_TITTEL,
                ),
                Dokument(
                    dokumentInfoId = DOKUMENT_2_ID,
                    tittel = DOKUMENT_1_TITTEL,
                )
            ),
        );

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
                    tittel = DOKUMENT_1_TITTEL,
                ),
                Dokument(
                    dokumentInfoId = DOKUMENT_2_ID,
                    tittel = DOKUMENT_1_TITTEL,
                )
            ),
        );

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
                    tittel = DOKUMENT_1_TITTEL,
                ),
                Dokument(
                    dokumentInfoId = DOKUMENT_2_ID,
                    tittel = DOKUMENT_1_TITTEL,
                )
            ),
        );

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