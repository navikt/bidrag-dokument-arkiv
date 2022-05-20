package no.nav.bidrag.dokument.arkiv.controller


import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.dokument.arkiv.dto.Dokument
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.OppgaveSokResponse
import no.nav.bidrag.dokument.arkiv.dto.OppgaveType
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_ID
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_TITTEL
import no.nav.bidrag.dokument.arkiv.stubs.JOURNALPOST_ID
import no.nav.bidrag.dokument.arkiv.stubs.JOURNALPOST_ID_3
import no.nav.bidrag.dokument.arkiv.stubs.NY_JOURNALPOST_ID_KNYTT_TIL_SAK
import no.nav.bidrag.dokument.arkiv.stubs.createOppgaveDataWithJournalpostId
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.dto.AvvikType
import no.nav.bidrag.dokument.dto.Avvikshendelse
import no.nav.bidrag.dokument.dto.BehandleAvvikshendelseResponse
import org.assertj.core.api.Assertions
import org.json.JSONException
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.IOException

class AvvikControllerTest : AbstractControllerTest() {
    @Test
    @Throws(IOException::class, JSONException::class)
    fun `skal utfore avvik OVERFOR_TIL_ANNEN_ENHET`() {
        // given
        val xEnhet = "1234"
        val overforTilEnhet = "4833"
        val journalpostIdFraJson = 201028011L
        val avvikHendelse = createAvvikHendelse(AvvikType.OVERFOR_TIL_ANNEN_ENHET, java.util.Map.of("nyttEnhetsnummer", overforTilEnhet))
        stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson)

        val overforEnhetResponse = sendAvvikRequest(xEnhet, journalpostIdFraJson, avvikHendelse)

        // then
        assertAll(
            {
                Assertions.assertThat(overforEnhetResponse)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostIdFraJson,
                    String.format("\"journalfoerendeEnhet\":\"%s\"", overforTilEnhet)
                )
            },
            {
                Mockito.verify(kafkaTemplateMock).send(
                    ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(
                        "JOARK-$journalpostIdFraJson"
                    ), ArgumentMatchers.any()
                )
            }
        )
    }

    @Test
    @DisplayName("skal utføre avvik REGISTRER_RETUR")
    @Throws(IOException::class, JSONException::class)
    fun skalSendeAvvikRegistrerRetur() {
        // given
        val xEnhet = "1234"
        val returDato = "2021-02-03"
        val beskrivelse = "Dette er en beskrivelse i en test"
        val journalpostIdFraJson = 201028011L
        val avvikHendelse = createAvvikHendelse(AvvikType.REGISTRER_RETUR, java.util.Map.of("returDato", returDato))
        avvikHendelse.beskrivelse = beskrivelse
        stubs.mockSafResponseHentJournalpost(responseJournalpostJsonWithReturDetaljer, HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson)

        val overforEnhetResponse = sendAvvikRequest(xEnhet, journalpostIdFraJson, avvikHendelse)

        // then
        assertAll(
            {
                Assertions.assertThat(overforEnhetResponse)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostIdFraJson, "\"tilleggsopplysninger\":["
                            + "{\"nokkel\":\"retur0_2020-11-15\",\"verdi\":\"Beskrivelse av retur\"},"
                            + "{\"nokkel\":\"retur0_2020-12-14\",\"verdi\":\"Beskrivelse av retur\"},"
                            + "{\"nokkel\":\"retur1_2020-12-15\",\"verdi\":\" mer tekst for å teste lengre verdier\"},"
                            + "{\"nokkel\":\"retur1_2020-12-14\",\"verdi\":\" mer tekst for å teste lengre verdier\"},"
                            + "{\"nokkel\":\"retur0_2020-12-15\",\"verdi\":\"Beskrivelse av retur 2\"},"
                            + "{\"nokkel\":\"retur0_2021-02-03\",\"verdi\":\"Dette er en beskrivelse i en test\"}]", "\"datoRetur\":\"2021-02-03\""
                )
            },
            {
                Mockito.verify(kafkaTemplateMock).send(
                    ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(
                        "JOARK-$journalpostIdFraJson"
                    ), ArgumentMatchers.any()
                )
            }
        )
    }

    @Test
    @DisplayName("skal utføre avvik REGISTRER_RETUR with long beskrivelse")
    @Throws(IOException::class, JSONException::class)
    fun skalSendeAvvikRegistrerReturLangBeskrivelse() {
        // given
        val xEnhet = "1234"
        val returDato = "2021-02-03"
        val beskrivelse = ("Dette er en veldig lang beskrivelse i en test. "
                + "Batman nanananana nananana nananana nananana nananan. Batman nanananana nananana nananana nananana nananan. Batman nanananana nananana nananana nananana nananan")
        val journalpostIdFraJson = 201028011L
        val avvikHendelse = createAvvikHendelse(AvvikType.REGISTRER_RETUR, java.util.Map.of("returDato", returDato))
        avvikHendelse.beskrivelse = beskrivelse
        stubs.mockSafResponseHentJournalpost(responseJournalpostJsonUtgaaende, HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson)

        val overforEnhetResponse = sendAvvikRequest(xEnhet, journalpostIdFraJson, avvikHendelse)

        // then
        assertAll(
            {
                Assertions.assertThat(overforEnhetResponse)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostIdFraJson, "\"tilleggsopplysninger\":["
                            + "{\"nokkel\":\"retur0_2021-02-03\",\"verdi\":\"Dette er en veldig lang beskrivelse i en test. Batman nanananana nananana nananana nananana nananan.\"},"
                            + "{\"nokkel\":\"retur1_2021-02-03\",\"verdi\":\" Batman nanananana nananana nananana nananana nananan. Batman nanananana nananana nananana nananana \"},"
                            + "{\"nokkel\":\"retur2_2021-02-03\",\"verdi\":\"nananan\"}]", "\"datoRetur\":\"2021-02-03\""
                )
            },
            {
                Mockito.verify(kafkaTemplateMock).send(
                    ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(
                        "JOARK-$journalpostIdFraJson"
                    ), ArgumentMatchers.any()
                )
            }
        )
    }

    @Test
    @DisplayName("skal ikke sende journalpostHendelse når avvik OVER_TIL_ANNEN_ENHET feiler")
    @Throws(
        IOException::class, JSONException::class
    )
    fun shouldNotSendeKafkaMessageWhenAvvikFails() {
        // given
        val xEnhet = "1234"
        val overforTilEnhet = "4833"
        val journalpostIdFraJson = 201028011L
        val avvikHendelse = createAvvikHendelse(AvvikType.OVERFOR_TIL_ANNEN_ENHET, java.util.Map.of("nyttEnhetsnummer", overforTilEnhet))
        stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson, HttpStatus.BAD_REQUEST)

        val overforEnhetResponse = sendAvvikRequest(xEnhet, journalpostIdFraJson, avvikHendelse)

        // then
        assertAll(
            {
                Assertions.assertThat(overforEnhetResponse)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.BAD_REQUEST)
            },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostIdFraJson,
                    String.format("\"journalfoerendeEnhet\":\"%s\"", overforTilEnhet)
                )
            },
            {
                Mockito.verify(kafkaTemplateMock, Mockito.never()).send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
            }
        )
    }

    @Test
    @DisplayName("skal utføre avvik ENDRE_FAGOMRADE")
    @Throws(IOException::class, JSONException::class)
    fun skalSendeAvvikEndreFagomrade() {
        // given
        val xEnhet = "1234"
        val geografiskEnhet = "1234"
        val nyttFagomrade = "FAR"
        val avvikHendelse = createAvvikHendelse(AvvikType.ENDRE_FAGOMRADE, java.util.Map.of("fagomrade", nyttFagomrade))
        stubs.mockOrganisasjonGeografiskTilknytning(geografiskEnhet)
        stubs.mockSafResponseHentJournalpost(opprettSafResponse())
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)

        val overforEnhetResponse = sendAvvikRequest(xEnhet, JOURNALPOST_ID, avvikHendelse)

        // then
        assertAll(
            {
                Assertions.assertThat(overforEnhetResponse)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            { stubs.verifyStub.dokarkivOppdaterKalt(JOURNALPOST_ID, String.format("\"tema\":\"%s\"", nyttFagomrade)) },
            {
                Mockito.verify(kafkaTemplateMock).send(
                    ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(
                        "JOARK-$JOURNALPOST_ID"
                    ), ArgumentMatchers.any()
                )
            }
        )
    }

    @Test
    @DisplayName("skal utføre avvik ENDRE_FAGOMRADE når journalpost journalført og endres fra BID til FAR")
    @Throws(IOException::class, JSONException::class)
    fun skalEndreFagomradeNarJournalpostJournalfortOgEndresMellomFAROgBID() {
        // given
        val xEnhet = "1234"
        val nyttFagomrade = "FAR"
        val avvikHendelse = createAvvikHendelse(AvvikType.ENDRE_FAGOMRADE, java.util.Map.of("fagomrade", nyttFagomrade))

        stubs.mockSafResponseDokumentOversiktFagsak()
        stubs.mockSafResponseHentJournalpost(opprettSafResponse(journalstatus = JournalStatus.JOURNALFOERT))
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockOpprettOppgave(HttpStatus.OK)
        stubs.mockDokarkivProxyTilknyttRequest(JOURNALPOST_ID, NY_JOURNALPOST_ID_KNYTT_TIL_SAK)
        stubs.mockDokarkivFeilregistrerRequest(JOURNALPOST_ID)

        val overforEnhetResponse = sendAvvikRequest(xEnhet, JOURNALPOST_ID, avvikHendelse)

        // then
        assertAll(
            {
                Assertions.assertThat(overforEnhetResponse)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            { stubs.verifyStub.oppgaveOpprettIkkeKalt() },
            { stubs.verifyStub.dokarkivProxyTilknyttSakerKalt(JOURNALPOST_ID, "FAR") },
            { stubs.verifyStub.dokarkivFeilregistrerKalt(JOURNALPOST_ID) },
            {
                Mockito.verify(kafkaTemplateMock).send(
                    ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(
                        "JOARK-$JOURNALPOST_ID"
                    ), ArgumentMatchers.any()
                )
            }
        )
    }

    @Test
    @Throws(IOException::class, JSONException::class)
    fun `skal ikke knytte til ny sak ved endre fagomrade mellom FAR og BID hvis allerede finnes feilregistrert`() {
        // given
        val xEnhet = "1234"
        val nyttFagomrade = "FAR"
        val avvikHendelse = createAvvikHendelse(AvvikType.ENDRE_FAGOMRADE, java.util.Map.of("fagomrade", nyttFagomrade))

        stubs.mockSafResponseDokumentOversiktFagsak(listOf(
            opprettSafResponse(
                journalpostId = JOURNALPOST_ID.toString(),
                dokumenter = listOf(
                    Dokument(
                        tittel = DOKUMENT_1_TITTEL,
                        dokumentInfoId = DOKUMENT_1_ID
                    )
                ),
                journalstatus = JournalStatus.JOURNALFOERT,
                tema = "BID"
            ),
            opprettSafResponse(
                journalpostId = JOURNALPOST_ID_3.toString(),
                dokumenter = listOf(
                    Dokument(
                    tittel = DOKUMENT_1_TITTEL,
                    dokumentInfoId = DOKUMENT_1_ID
                )
                ),
                tema = "FAR",
                journalstatus = JournalStatus.FEILREGISTRERT
            )
        ))
        stubs.mockSafResponseHentJournalpost(opprettSafResponse(journalstatus = JournalStatus.JOURNALFOERT))
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockOpprettOppgave(HttpStatus.OK)
        stubs.mockDokarkivProxyTilknyttRequest(JOURNALPOST_ID, JOURNALPOST_ID_3)
        stubs.mockDokarkivFeilregistrerRequest(JOURNALPOST_ID)
        stubs.mockDokarkivOpphevFeilregistrerRequest(JOURNALPOST_ID_3)

        val overforEnhetResponse = sendAvvikRequest(xEnhet, JOURNALPOST_ID, avvikHendelse)

        // then
        assertAll(
            {
                Assertions.assertThat(overforEnhetResponse)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            { stubs.verifyStub.oppgaveOpprettIkkeKalt() },
            { stubs.verifyStub.dokarkivProxyTilknyttSakerIkkeKalt(JOURNALPOST_ID) },
            { stubs.verifyStub.dokarkivFeilregistrerKalt(JOURNALPOST_ID) },
            { stubs.verifyStub.dokarkivOpphevFeilregistrerKalt(JOURNALPOST_ID_3) },
            {
                Mockito.verify(kafkaTemplateMock).send(
                    ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(
                        "JOARK-$JOURNALPOST_ID"
                    ), ArgumentMatchers.any()
                )
            }
        )
    }

    @Test
    @DisplayName("skal utføre avvik ENDRE_FAGOMRADE når journalpost journalført")
    @Throws(IOException::class, JSONException::class)
    fun skalSendeAvvikEndreFagomradeNarJournalpostJournalfort() {
        // given
        val xEnhet = "1234"
        val geografiskEnhet = "1234"
        val nyttFagomrade = "AAP"
        val journalpostIdFraJson = 201028011L
        val nyJournalpostId = 301028011L
        val avvikHendelse = createAvvikHendelse(AvvikType.ENDRE_FAGOMRADE, java.util.Map.of("fagomrade", nyttFagomrade))
        stubs.mockOrganisasjonGeografiskTilknytning(geografiskEnhet)
        stubs.mockDokarkivProxyTilknyttRequest(journalpostIdFraJson, nyJournalpostId)
        stubs.mockSafResponseHentJournalpost(journalpostJournalfortSafResponse, HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockOpprettOppgave(HttpStatus.OK)
        stubs.mockDokarkivFeilregistrerRequest(journalpostIdFraJson)

        val overforEnhetResponse = sendAvvikRequest(xEnhet, journalpostIdFraJson, avvikHendelse)

        // then
        assertAll(
            {
                Assertions.assertThat(overforEnhetResponse)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            { stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "\"tilleggsopplysninger\":[{\"nokkel\":\"something\",\"verdi\":\"something\"},{\"nokkel\":\"endretFagomrade\",\"verdi\":\"true\"}]") },
            { stubs.verifyStub.oppgaveOpprettKalt(OppgaveType.VUR.name, nyJournalpostId.toString()) },
            { stubs.verifyStub.dokarkivFeilregistrerKalt(journalpostIdFraJson) },
            {
                Mockito.verify(kafkaTemplateMock).send(
                    ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(
                        "JOARK-$journalpostIdFraJson"
                    ), ArgumentMatchers.any()
                )
            }
        )
    }

    @Test
    @DisplayName("skal utføre avvik FEILFORE_SAK")
    @Throws(IOException::class)
    fun skalSendeAvvikFeilfor() {
        // given
        val xEnhet = "1234"
        val journalpostIdFraJson = 201028011L
        val avvikHendelse = createAvvikHendelse(AvvikType.FEILFORE_SAK, java.util.Map.of())
        stubs.mockSafResponseHentJournalpost(journalpostJournalfortSafResponse, HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson)
        stubs.mockDokarkivFeilregistrerRequest(journalpostIdFraJson)

        val overforEnhetResponse = sendAvvikRequest(xEnhet, journalpostIdFraJson, avvikHendelse)

        // then
       assertAll(
            {
                Assertions.assertThat(overforEnhetResponse)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            { stubs.verifyStub.dokarkivFeilregistrerKalt(journalpostIdFraJson) },
            {
                Mockito.verify(kafkaTemplateMock).send(
                    ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(
                        "JOARK-$journalpostIdFraJson"
                    ), ArgumentMatchers.any()
                )
            }
        )
    }

    @Test
    @DisplayName("skal utføre avvik TREKK_JOURNALPOST")
    @Throws(IOException::class)
    fun skalUtforeAvvikTrekkJournalpostOgFeilregistrere() {
        // given
        val xEnhet = "1234"
        val journalpostIdFraJson = 201028011L
        val avvikHendelse = createAvvikHendelse(AvvikType.TREKK_JOURNALPOST, java.util.Map.of())
        val detaljer: MutableMap<String, String> = HashMap()
        avvikHendelse.detaljer = detaljer
        stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        stubs.mockDokarkivFeilregistrerRequest(journalpostIdFraJson)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockDokarkivFerdigstillRequest(journalpostIdFraJson)

        val overforEnhetResponse = sendAvvikRequest(xEnhet, journalpostIdFraJson, avvikHendelse)

        // then
        assertAll(
            {
                Assertions.assertThat(overforEnhetResponse)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            { stubs.verifyStub.dokarkivFeilregistrerKalt(journalpostIdFraJson) },
            { stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "GENERELL_SAK") },
            { stubs.verifyStub.dokarkivFerdigstillKalt(journalpostIdFraJson) },
            {
                Mockito.verify(kafkaTemplateMock).send(
                    ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(
                        "JOARK-$journalpostIdFraJson"
                    ), ArgumentMatchers.any()
                )
            }
        )
    }

    private fun sendAvvikRequest(enhet: String, journalpostId: Long, avvikHendelse: Avvikshendelse): ResponseEntity<BehandleAvvikshendelseResponse>{
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, enhet)
        return httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostId + "/avvik",
            HttpMethod.POST,
            HttpEntity(avvikHendelse, headersMedEnhet),
            BehandleAvvikshendelseResponse::class.java
        )
    }

    private fun createAvvikHendelse(avvikType: AvvikType, detaljer: Map<String, String>): Avvikshendelse {
        val avvikHendelse = Avvikshendelse()
        avvikHendelse.avvikType = avvikType.name
        avvikHendelse.detaljer = detaljer
        return avvikHendelse
    }
}