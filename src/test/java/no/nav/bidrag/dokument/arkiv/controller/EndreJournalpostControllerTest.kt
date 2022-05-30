package no.nav.bidrag.dokument.arkiv.controller

import no.nav.bidrag.dokument.arkiv.dto.Bruker
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.OppgaveSokResponse
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.stubs.Stubs
import no.nav.bidrag.dokument.arkiv.stubs.createOppgaveDataWithSaksnummer
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.dto.EndreDokument
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand
import no.nav.bidrag.dokument.dto.JournalpostDto
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.IOException
import java.time.LocalDate
import java.util.List

class EndreJournalpostControllerTest: AbstractControllerTest() {

    private fun createEndreJournalpostCommand(): EndreJournalpostCommand {
        val endreJournalpostCommand = EndreJournalpostCommand()
        endreJournalpostCommand.avsenderNavn = "Dauden, Svarte"
        endreJournalpostCommand.gjelder = "06127412345"
        endreJournalpostCommand.tittel = "So Tired"
        endreJournalpostCommand.endreDokumenter = List.of(
            EndreDokument("BLABLA", 1, "In a galazy far far away")
        )
        return endreJournalpostCommand
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
            ),null, "AFTER")
        stubs.mockSafResponseHentJournalpost(opprettSafResponse(
            journalpostId = journalpostId.toString(),
            tittel = endreJournalpostCommand.tittel!!,
            journalstatus = JournalStatus.JOURNALFOERT,
            sak = Sak(sak)
        ),"AFTER", null)
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
            {
                stubs.verifyStub.oppgaveSokKalt(
                    Pair("tema", "BID"),
                    Pair("saksreferanse", sak)
                )
            },
            { stubs.verifyStub.oppgaveOpprettKalt("\"oppgavetype\":\"BEH_SAK\"", endreJournalpostCommand.tittel) }
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
            ),null, "AFTER")
        stubs.mockSafResponseHentJournalpost(opprettSafResponse(
            journalpostId = journalpostId.toString(),
            journalstatus = JournalStatus.JOURNALFOERT,
            sak = Sak(saksnummer1),
            bruker = Bruker(endreJournalpostCommand.gjelder, "FNR"),
            journalforendeEnhet = "4806"
        ),"AFTER", null)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockDokarkivFerdigstillRequest(journalpostId)
        stubs.mockDokarkivProxyTilknyttRequest(journalpostId)

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
             { stubs.verifyStub.dokarkivProxyTilknyttSakerIkkeKalt(journalpostId, saksnummer1) },
             {
                stubs.verifyStub.dokarkivProxyTilknyttSakerKalt(
                    journalpostId,
                    saksnummer2,
                    "\"journalfoerendeEnhet\":\"4806\"",
                    "\"bruker\":{\"id\":\"${endreJournalpostCommand.gjelder}\",\"idType\":\"FNR\"}"
                )
            },
             {
                stubs.verifyStub.oppgaveSokKalt(
                    Pair("tema", "BID"),
                    Pair("saksreferanse", saksnummer1),
                    Pair("saksreferanse", saksnummer2)
                )
            },
             {
                stubs.verifyStub.oppgaveOpprettKalt(
                    "\"oppgavetype\":\"BEH_SAK\"",
                    "\"saksreferanse\":\"200000\""
                )
            },
             {
                stubs.verifyStub.oppgaveOpprettKalt(
                    "\"oppgavetype\":\"BEH_SAK\"",
                    "\"saksreferanse\":\"200001\""
                )
            },
             { stubs.verifyStub.oppgaveOppdaterKalt(1, "Nytt dokument") }
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
        stubs.mockDokarkivProxyTilknyttRequest(journalpostIdFraJson)

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
             { stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson,
                 "\"tittel\":\"So Tired\"",
                 "\"avsenderMottaker\":{\"navn\":\"Dauden, Svarte\"}",
                 "\"datoMottatt\":\"2020-02-03\"",
                 "\"dokumenter\":[{\"dokumentInfoId\":\"1\",\"tittel\":\"In a galazy far far away\",\"brevkode\":\"BLABLA\"}]"
                 ) },
             {
                stubs.verifyStub.dokarkivProxyTilknyttSakerKalt(
                    journalpostIdFraJson,
                    newSaksnummer,
                    "\"journalfoerendeEnhet\":\"4806\""
                )
            },
             { stubs.verifyStub.dokarkivProxyTilknyttSakerIkkeKalt(journalpostIdFraJson, existingSaksnummer) }
        )
    }

    @Test
    @DisplayName("skal endre journalpost uten journalføring")
    @Throws(IOException::class, JSONException::class)
    fun skalEndreJournalpostUtenJournalforing() {

        val journalpostId = 201028011L
        val endreJournalpostCommand = createEndreJournalpostCommand()
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
}