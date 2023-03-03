package no.nav.bidrag.dokument.arkiv.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.dokument.arkiv.dto.Bruker
import no.nav.bidrag.dokument.arkiv.dto.DatoType
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.JournalpostType
import no.nav.bidrag.dokument.arkiv.dto.OppgaveSokResponse
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse
import no.nav.bidrag.dokument.arkiv.dto.ReturDetaljerLogDO
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.stubs.RETUR_DETALJER_DATO_1
import no.nav.bidrag.dokument.arkiv.stubs.RETUR_DETALJER_DATO_2
import no.nav.bidrag.dokument.arkiv.stubs.Stubs
import no.nav.bidrag.dokument.arkiv.stubs.createOppgaveDataWithSaksnummer
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponseWithReturDetaljer
import no.nav.bidrag.dokument.dto.EndreDokument
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand
import no.nav.bidrag.dokument.dto.EndreReturDetaljer
import no.nav.bidrag.dokument.dto.JournalpostDto
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.IOException
import java.time.LocalDate
import java.util.List

class EndreJournalpostControllerTest : AbstractControllerTest() {

    private fun createEndreJournalpostCommand(): EndreJournalpostCommand {
        val endreJournalpostCommand = EndreJournalpostCommand()
        endreJournalpostCommand.avsenderNavn = "Dauden, Svarte"
        endreJournalpostCommand.gjelder = "06127412345"
        endreJournalpostCommand.tittel = "So Tired"
        endreJournalpostCommand.endreDokumenter = listOf(
            EndreDokument("BLABLA", "1", "1", "In a galazy far far away")
        )
        return endreJournalpostCommand
    }

    @Test
    fun `skal ikke kunne endre dokumentdato på notat til fram i tid`() {
        val sak = "200000"
        val journalpostId = 201028011L

        val endreJournalpostCommand: EndreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.tittel = "Ny tittel"
        endreJournalpostCommand.dokumentDato = LocalDate.now().plusDays(2)

        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                journalpostType = JournalpostType.N,
                sak = Sak("123")
            )
        )
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostId,
            HttpMethod.PATCH,
            HttpEntity(endreJournalpostCommand, headerMedEnhet),
            JournalpostDto::class.java
        )

        // then
        assertSoftly {
            oppdaterJournalpostResponseEntity.statusCode shouldBe HttpStatus.BAD_REQUEST
        }
    }

    @Test
    fun `skal endre notat`() {
        val sak = "200000"
        val journalpostId = 201028011L

        val endreJournalpostCommand: EndreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.tittel = "Ny tittel"
        endreJournalpostCommand.dokumentDato = LocalDate.parse("2022-05-20")

        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                journalpostType = JournalpostType.N,
                sak = Sak("123")
            )
        )
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostId,
            HttpMethod.PATCH,
            HttpEntity(endreJournalpostCommand, headerMedEnhet),
            JournalpostDto::class.java
        )

        // then
        assertSoftly {
            oppdaterJournalpostResponseEntity.statusCode shouldBe HttpStatus.OK
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "\"tittel\":\"Ny tittel\"",
                "\"datoDokument\":\"2022-05-20T00:00\"",
            )
        }
    }

    @Test
    @DisplayName("skal endre og journalføre journalpost")
    @Throws(IOException::class, JSONException::class)
    fun skalEndreOgJournalforeJournalpost() {
        val sak = "200000"
        val journalpostId = 201028011L

        val endreJournalpostCommand: EndreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = true
        endreJournalpostCommand.tittel = "Ny tittel"
        endreJournalpostCommand.tilknyttSaker = listOf(sak)

        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                sak = null
            ), null, "AFTER"
        )
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                tittel = endreJournalpostCommand.tittel!!,
                journalstatus = JournalStatus.JOURNALFOERT,
                sak = Sak(sak)
            ), "AFTER", null
        )
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockDokarkivFerdigstillRequest(journalpostId)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostId,
            HttpMethod.PATCH,
            HttpEntity(endreJournalpostCommand, headerMedEnhet),
            JournalpostDto::class.java
        )

        // then
        Assertions.assertAll(
            {
                assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostId,
                    "\"fagsakId\":\"$sak\"",
                    "\"fagsaksystem\":\"BISYS\"",
                    "\"sakstype\":\"FAGSAK\"",
                    "\"bruker\":{\"id\":\"06127412345\",\"idType\":\"FNR\"}",
                    "\"avsenderMottaker\":{\"navn\":\"Dauden, Svarte\"}",
                    "\"dokumenter\":[{\"dokumentInfoId\":\"1\",\"tittel\":\"In a galazy far far away\",\"brevkode\":\"BLABLA\"}]"
                )
            },
            { stubs.verifyStub.dokarkivFerdigstillKalt(journalpostId) },
        )
    }

    @Test
    @DisplayName("skal endre og journalføre journalpost med flere saker")
    @Throws(
        IOException::class,
        JSONException::class
    )
    fun skalEndreOgJournalforeJournalpostMedFlereSaker() {
        val saksnummer1 = "200000"
        val saksnummer2 = "200001"
        val saksnummer3 = "200003"
        val journalpostId = 201028011L

        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = true
        endreJournalpostCommand.gjelder = "12333333333"
        endreJournalpostCommand.tilknyttSaker = listOf(saksnummer1, saksnummer2)

        stubs.mockSokOppgave(OppgaveSokResponse(1, listOf(createOppgaveDataWithSaksnummer(saksnummer3))), HttpStatus.OK)
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                sak = null
            ), null, "AFTER"
        )
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                journalstatus = JournalStatus.JOURNALFOERT,
                sak = Sak(saksnummer1),
                bruker = Bruker(endreJournalpostCommand.gjelder, "FNR"),
                journalforendeEnhet = "4806"
            ), "AFTER", null
        )
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockDokarkivFerdigstillRequest(journalpostId)
        stubs.mockDokarkivTilknyttRequest(journalpostId)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostId,
            HttpMethod.PATCH,
            HttpEntity(endreJournalpostCommand, headerMedEnhet),
            JournalpostDto::class.java
        )

        // then
        Assertions.assertAll(
            {
                assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            { stubs.verifyStub.dokarkivFerdigstillKalt(journalpostId) },
            { stubs.verifyStub.dokarkivTilknyttSakerIkkeKalt(journalpostId, saksnummer1) },
            {
                stubs.verifyStub.dokarkivTilknyttSakerKalt(
                    journalpostId,
                    saksnummer2,
                    "\"journalfoerendeEnhet\":\"4806\"",
                    "\"bruker\":{\"id\":\"${endreJournalpostCommand.gjelder}\",\"idType\":\"FNR\"}"
                )
            },
            { stubs.verifyStub.oppgaveOppdaterKalt(0) },
        )
    }

    @Test
    @DisplayName("skal endre journalført journalpost med flere saker")
    @Throws(
        IOException::class,
        JSONException::class
    )
    fun skalEndreJournalfortJournalpostMedFlereSaker() {
        val existingSaksnummer = Stubs.SAKSNUMMER_JOURNALPOST
        val newSaksnummer = "200000"
        val journalpostIdFraJson = 201028011L

        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.tilknyttSaker = listOf(existingSaksnummer, newSaksnummer)
        endreJournalpostCommand.dokumentDato = LocalDate.of(2020, 2, 3)

        stubs.mockSafResponseHentJournalpost("journalpostJournalfortSafResponse.json", HttpStatus.OK)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson)
        stubs.mockDokarkivTilknyttRequest(journalpostIdFraJson)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
            HttpMethod.PATCH,
            HttpEntity(endreJournalpostCommand, headerMedEnhet),
            JournalpostDto::class.java
        )

        // then
        Assertions.assertAll(
            {
                assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostIdFraJson,
                    "\"tittel\":\"So Tired\"",
                    "\"avsenderMottaker\":{\"navn\":\"Dauden, Svarte\"}",
                    "\"datoMottatt\":\"2020-02-03\"",
                    "\"dokumenter\":[{\"dokumentInfoId\":\"1\",\"tittel\":\"In a galazy far far away\",\"brevkode\":\"BLABLA\"}]"
                )
            },
            {
                stubs.verifyStub.dokarkivTilknyttSakerKalt(
                    journalpostIdFraJson,
                    newSaksnummer,
                    "\"journalfoerendeEnhet\":\"4806\""
                )
            },
            { stubs.verifyStub.dokarkivTilknyttSakerIkkeKalt(journalpostIdFraJson, existingSaksnummer) }
        )
    }

    @Test
    @DisplayName("skal endre journalpost uten journalføring")
    @Throws(IOException::class, JSONException::class)
    fun skalEndreJournalpostUtenJournalforing() {

        val journalpostId = 201028011L
        val endreJournalpostCommand = createEndreJournalpostCommand()
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockSafResponseHentJournalpost(opprettSafResponse(journalpostId = journalpostId.toString()))
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostId)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostId,
            HttpMethod.PATCH,
            HttpEntity(endreJournalpostCommand, headerMedEnhet),
            JournalpostDto::class.java
        )

        // then
        Assertions.assertAll(
            {
                assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            { stubs.verifyStub.dokarkivOppdaterKalt(journalpostId, "") },
            { stubs.verifyStub.dokarkivFerdigstillIkkeKalt(journalpostId) },
            { stubs.verifyStub.oppgaveOpprettIkkeKalt() },
            { stubs.verifyStub.oppgaveSokIkkeKalt() }
        )
    }


    @Test
    @DisplayName("skal endre utgaaende journalpost retur detaljer")
    @Throws(IOException::class, JSONException::class)
    fun skalEndreUtgaaendeJournalpostReturDetaljer() {
        val xEnhet = "1234"
        val journalpostIdFraJson = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        val endreJournalpostCommand = no.nav.bidrag.dokument.arkiv.stubs.createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(
            EndreReturDetaljer(RETUR_DETALJER_DATO_1, null, "Ny beskrivelse 1"),
            EndreReturDetaljer(RETUR_DETALJER_DATO_2, LocalDate.parse("2021-10-10"), "Ny beskrivelse 2")
        )
        val safResponse = opprettUtgaendeSafResponseWithReturDetaljer()
        safResponse.antallRetur = 1
        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson)
        stubs.mockDokarkivTilknyttRequest(journalpostIdFraJson)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
            HttpMethod.PATCH,
            HttpEntity(endreJournalpostCommand, headersMedEnhet),
            JournalpostDto::class.java
        )

        // then
        Assertions.assertAll(
            {
                assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostIdFraJson, "tilleggsopplysninger\":" +
                            "[{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}," +
                            "{\"nokkel\":\"retur0_2021-08-20\",\"verdi\":\"Ny beskrivelse 1\"}," +
                            "{\"nokkel\":\"retur0_2021-10-10\",\"verdi\":\"Ny beskrivelse 2\"}]"
                )
            }
        )
    }

    @Test
    @Throws(IOException::class, JSONException::class)
    fun `Skal legge til ny returdetalj hvis returdetalj for distribuert dokument etter dokumentdato mangler`() {
        val xEnhet = "1234"
        val journalpostId = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02"),
                true
            )
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-10-02"),
                true
            )
        )
        val safResponse = opprettUtgaendeSafResponse(
            journalpostId = journalpostId.toString(), tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(
                DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")
            )
        )
        safResponse.antallRetur = 1
        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockDokarkivFerdigstillRequest(journalpostId)
        stubs.mockDokarkivTilknyttRequest(journalpostId)

        val endreJournalpostCommand = no.nav.bidrag.dokument.arkiv.stubs.createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(null, LocalDate.parse("2021-12-15"), "Ny returdetalj"))
        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostId,
            HttpMethod.PATCH,
            HttpEntity(endreJournalpostCommand, headersMedEnhet),
            JournalpostDto::class.java
        )

        // then
        Assertions.assertAll(
            {
                assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostId, "\"tilleggsopplysninger\":["
                            + "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"},"
                            + "{\"nokkel\":\"Lretur0_2020-01-02\",\"verdi\":\"En god begrunnelse for hvorfor dokument kom i retur\"},"
                            + "{\"nokkel\":\"Lretur0_2020-10-02\",\"verdi\":\"En annen god begrunnelse for hvorfor dokument kom i retur\"},"
                            + "{\"nokkel\":\"retur0_2021-12-15\",\"verdi\":\"Ny returdetalj\"}]"
                )
            }
        )
    }

    @Test
    @Throws(IOException::class, JSONException::class)
    fun `Skal ikke kunne endre laste returdetaljer`() {
        val xEnhet = "1234"
        val journalpostId = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02"),
                true
            )
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-10-02"),
                true
            )
        )
        val safResponse = opprettUtgaendeSafResponse(
            journalpostId = journalpostId.toString(), tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(
                DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")
            )
        )
        safResponse.antallRetur = 1
        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockDokarkivFerdigstillRequest(journalpostId)
        stubs.mockDokarkivTilknyttRequest(journalpostId)

        val endreJournalpostCommand = no.nav.bidrag.dokument.arkiv.stubs.createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer =
            listOf(EndreReturDetaljer(LocalDate.parse("2020-10-02"), LocalDate.parse("2021-12-15"), "Oppdatert returdetalj"))
        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostId,
            HttpMethod.PATCH,
            HttpEntity(endreJournalpostCommand, headersMedEnhet),
            JournalpostDto::class.java
        )

        // then
        Assertions.assertAll(
            {
                assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.BAD_REQUEST)
            },
            {
                assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.headers[HttpHeaders.WARNING] }
                    .`as`("Feilmelding")
                    .isEqualTo(listOf("Ugyldige data: Kan ikke endre låste returdetaljer, Kan ikke endre returdetaljer opprettet før dokumentdato"))
            }
        )
    }
}