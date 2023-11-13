package no.nav.bidrag.dokument.arkiv.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.JournalstatusDto
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.stubs.AVSENDER_ID
import no.nav.bidrag.dokument.arkiv.stubs.AVSENDER_NAVN
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_TITTEL
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.transport.dokument.AvsenderMottakerDto
import no.nav.bidrag.transport.dokument.AvsenderMottakerDtoIdType
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.dokument.ReturDetaljerLog
import no.nav.bidrag.transport.person.PersonDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.io.IOException
import java.time.LocalDate

internal class JournalpostControllerTest : AbstractControllerTest() {
    @Test
    @DisplayName("should map context path with random port")
    fun shouldMapToContextPath() {
        Assertions.assertThat(initUrl()).isEqualTo("http://localhost:$port/bidrag-dokument-arkiv")
    }

    @Test
    @DisplayName("skal ha 400 BAD REQUEST når prefix mangler")
    fun skalHaBadRequestNarPrefixPaJournalpostIdMangler() {
        val journalpostResponseEntity =
            httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(initUrl() + "/journal/1?saknummer=007")
        Assertions.assertThat(journalpostResponseEntity.statusCode)
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    @DisplayName("skal ha 400 BAD REQUEST når prefix er feil")
    fun skalHaBadRequestlNarPrefixPaJournalpostIdErFeil() {
        val journalpostResponseEntity =
            httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(initUrl() + "/journal/BID-1?saksnummer=007")
        Assertions.assertThat(journalpostResponseEntity.statusCode)
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    @DisplayName("skal ha body som er null samt header warning når journalpost ikke finnes")
    @Throws(IOException::class)
    fun skalHaBodySomErNullSamtHeaderWarningNarJournalpostIkkeFinnes() {
        stubs.mockSafResponseHentJournalpost(journalpostSafNotFoundResponse, HttpStatus.OK)
        stubs.mockPersonResponse(PersonDto(PERSON_IDENT, aktørId = AKTOR_IDENT), HttpStatus.OK)
        val journalpostResponseEntity = httpHeaderTestRestTemplate.getForEntity<String>(
            initUrl() + "/journal/JOARK-1?saksnummer=007",
        )
        Assertions.assertThat(journalpostResponseEntity).satisfies(
            {
                org.junit.jupiter.api.Assertions.assertAll(
                    { Assertions.assertThat(it.body).`as`("body").isNull() },
                    {
                        Assertions.assertThat(it.headers[HttpHeaders.WARNING])
                            .`as`("header warning").first()
                            .isEqualTo("Fant ikke journalpost i fagarkivet. journalpostId=910536260")
                    },
                    {
                        Assertions.assertThat(it.statusCode).`as`("status")
                            .isEqualTo(HttpStatus.NOT_FOUND)
                    },
                    { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                )
            },
        )
    }

    @Test
    @DisplayName("skal få 404 NOT FOUND når eksisterende journalpost er knyttet til annen sak")
    @Throws(IOException::class)
    fun skalFaNotFoundNarEksisterendeJournalpostErKnyttetTilAnnenSak() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK)
        stubs.mockPersonResponse(PersonDto(PERSON_IDENT, aktørId = AKTOR_IDENT), HttpStatus.OK)
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "?saksnummer=007")
        Assertions.assertThat(responseEntity).satisfies(
            {
                org.junit.jupiter.api.Assertions.assertAll(
                    { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                    { Assertions.assertThat(it.body).isNull() },
                    { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                )
            },
        )
    }

    @Test
    @DisplayName("skal få 500 INTERNAL SERVER når person api feiler")
    @Throws(IOException::class)
    fun skalFaServerFeilNarPersonTjenestenFeiler() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(
            PersonDto(PERSON_IDENT, aktørId = AKTOR_IDENT),
            HttpStatus.BAD_REQUEST,
        )
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(initUrl() + "/journal/JOARK-" + journalpostIdFraJson)
        Assertions.assertThat(responseEntity).satisfies(
            {
                org.junit.jupiter.api.Assertions.assertAll(
                    {
                        Assertions.assertThat(it.statusCode)
                            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                    },
                    { Assertions.assertThat(it.body).isNull() },
                    { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                    { stubs.verifyStub.bidragPersonKalt() },
                )
            },
        )
    }

    @Test
    @DisplayName("skal hente distribuert Journalpost med adresse")
    @Throws(IOException::class)
    fun skalHenteDistribuertJournalpostMedAdresse() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(responseJournalpostJsonWithAdresse, HttpStatus.OK)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonDto(PERSON_IDENT, aktørId = AKTOR_IDENT), HttpStatus.OK)
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(initUrl() + "/journal/JOARK-" + journalpostIdFraJson)
        val journalpost =
            if (responseEntity.body != null) responseEntity.body!!.journalpost else null
        val distribuertTilAdresse = journalpost!!.distribuertTilAdresse
        assertSoftly {
            Assertions.assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

            Assertions.assertThat(journalpost).isNotNull.extracting { it.journalpostId }
                .isEqualTo("JOARK-$journalpostIdFraJson")

            Assertions.assertThat(journalpost).isNotNull.extracting { it.journalstatus }
                .isEqualTo(JournalstatusDto.EKSPEDERT)

            Assertions.assertThat(distribuertTilAdresse).isNotNull()

            Assertions.assertThat(distribuertTilAdresse!!.adresselinje1)
                .isEqualTo("Testveien 20A")

            Assertions.assertThat(distribuertTilAdresse!!.adresselinje2)
                .isEqualTo("TestLinje2")

            Assertions.assertThat(distribuertTilAdresse!!.adresselinje3)
                .isEqualTo("TestLinje4")

            Assertions.assertThat(distribuertTilAdresse!!.postnummer).isEqualTo("7950")
            Assertions.assertThat(distribuertTilAdresse!!.poststed).isEqualTo("ABELVÆR")
            Assertions.assertThat(distribuertTilAdresse!!.land).isEqualTo("NO")
            stubs.verifyStub.harEnSafKallEtterHentJournalpost()
            stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter()
            stubs.verifyStub.bidragPersonKalt()
        }
    }

    @Test
    @DisplayName("skal hente Journalpost uten saker når den eksisterer")
    @Throws(IOException::class)
    fun skalHenteJournalpostUtenSakerNarDenEksisterer() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostIdFraJson.toString(),
                sak = null,
            ),
        )
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonDto(PERSON_IDENT, aktørId = AKTOR_IDENT), HttpStatus.OK)
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(initUrl() + "/journal/JOARK-" + journalpostIdFraJson)
        val journalpost =
            if (responseEntity.body != null) responseEntity.body!!.journalpost else null
        val saker =
            if (responseEntity.body != null) responseEntity.body!!.sakstilknytninger else null
        Assertions.assertThat(responseEntity).satisfies({
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.OK) },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it?.avsenderNavn }
                        .isEqualTo(AVSENDER_NAVN)
                },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it?.avsenderMottaker }
                        .isEqualTo(
                            AvsenderMottakerDto(
                                AVSENDER_NAVN,
                                AVSENDER_ID,
                                AvsenderMottakerDtoIdType.FNR,
                            ),
                        )
                },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it?.innhold }
                        .isEqualTo(DOKUMENT_1_TITTEL)
                },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it?.journalpostId }
                        .isEqualTo("JOARK-$journalpostIdFraJson")
                },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it?.gjelderAktor?.ident }
                        .isEqualTo(PERSON_IDENT.verdi)
                },
                { Assertions.assertThat(saker).isNotNull.hasSize(0) },
                { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                { stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter() },
                { stubs.verifyStub.bidragPersonKalt() },
            )
        })
    }

    @Test
    @DisplayName("skal hente Journalpost med retur detaljer")
    @Throws(IOException::class)
    fun skalHenteJournalpostMedReturDetaljer() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(
            responseJournalpostJsonWithReturDetaljer,
            HttpStatus.OK,
        )
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonDto(PERSON_IDENT, aktørId = AKTOR_IDENT), HttpStatus.OK)
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(initUrl() + "/journal/JOARK-" + journalpostIdFraJson)
        val journalpost =
            if (responseEntity.body != null) responseEntity.body!!.journalpost else null
        val returDetaljer = journalpost!!.returDetaljer
        val returDetaljerLog = journalpost.returDetaljer!!.logg
        Assertions.assertThat(responseEntity).satisfies({
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.OK) },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it.journalpostId }
                        .isEqualTo("JOARK-$journalpostIdFraJson")
                },
                { Assertions.assertThat(returDetaljer).isNotNull() },
                { Assertions.assertThat(returDetaljerLog!!.size).isEqualTo(3) },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2020-11-15"),
                                "Beskrivelse av retur",
                                false,
                            ),
                        ),
                    ).isTrue()
                },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2020-12-14"),
                                "Beskrivelse av retur mer tekst for å teste lengre verdier",
                                false,
                            ),
                        ),
                    ).isTrue()
                },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2022-12-15"),
                                "Beskrivelse av retur 2 mer tekst for å teste lengre verdier",
                                false,
                            ),
                        ),
                    ).isTrue()
                },
                { Assertions.assertThat(returDetaljer!!.antall).isEqualTo(3) },
                {
                    Assertions.assertThat(returDetaljer!!.dato)
                        .isEqualTo(LocalDate.parse("2020-12-15"))
                },
                { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                { stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter() },
                { stubs.verifyStub.bidragPersonKalt() },
            )
        })
    }

    @Test
    @DisplayName("skal hente Journalpost med låste retur detaljer")
    @Throws(IOException::class)
    fun skalHenteJournalpostMedLaasteReturDetaljer() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(
            "journalpostSafLockedReturDetaljerResponse.json",
            HttpStatus.OK,
        )
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonDto(PERSON_IDENT, aktørId = AKTOR_IDENT), HttpStatus.OK)
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(initUrl() + "/journal/JOARK-" + journalpostIdFraJson)
        val journalpost =
            if (responseEntity.body != null) responseEntity.body!!.journalpost else null
        val returDetaljer = journalpost!!.returDetaljer
        val returDetaljerLog = journalpost.returDetaljer!!.logg
        Assertions.assertThat(responseEntity).satisfies({
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.OK) },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it.journalpostId }
                        .isEqualTo("JOARK-$journalpostIdFraJson")
                },
                { Assertions.assertThat(returDetaljer).isNotNull() },
                { Assertions.assertThat(returDetaljerLog!!.size).isEqualTo(4) },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2020-11-15"),
                                "Beskrivelse av retur",
                                true,
                            ),
                        ),
                    ).isTrue()
                },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2020-12-14"),
                                "Beskrivelse av retur mer tekst for å teste lengre verdier",
                                true,
                            ),
                        ),
                    ).isTrue()
                },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2022-12-15"),
                                "Beskrivelse av retur 2 mer tekst for å teste lengre verdier",
                                true,
                            ),
                        ),
                    ).isTrue()
                },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2022-12-20"),
                                "Beskrivelse av retur uten lås",
                                false,
                            ),
                        ),
                    ).isTrue()
                },
                { Assertions.assertThat(returDetaljer!!.antall).isEqualTo(4) },
                {
                    Assertions.assertThat(returDetaljer!!.dato)
                        .isEqualTo(LocalDate.parse("2020-12-15"))
                },
                { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                { stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter() },
                { stubs.verifyStub.bidragPersonKalt() },
            )
        })
    }

    @Test
    @DisplayName("skal hente Journalpost når den eksisterer")
    @Throws(IOException::class)
    fun skalHenteJournalpostNarDenEksisterer() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(journalpostJournalfortSafResponse, HttpStatus.OK)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonDto(PERSON_IDENT, aktørId = AKTOR_IDENT), HttpStatus.OK)
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "?saksnummer=5276661")
        val journalpost =
            if (responseEntity.body != null) responseEntity.body!!.journalpost else null
        val saker =
            if (responseEntity.body != null) responseEntity.body!!.sakstilknytninger else null
        Assertions.assertThat(responseEntity).satisfies({
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.OK) },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it?.innhold }
                        .isEqualTo("Filosofens bidrag")
                },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it?.journalpostId }
                        .isEqualTo("JOARK-$journalpostIdFraJson")
                },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it?.gjelderAktor }
                        .extracting { it?.ident }
                        .isEqualTo(PERSON_IDENT.verdi)
                },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it?.brevkode?.kode }
                        .isEqualTo("BI01S02")
                },
                {
                    Assertions.assertThat(saker).isNotNull.hasSize(3).contains("2106585")
                        .contains("5276661")
                },
                { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                { stubs.verifyStub.harEnSafKallEtterTilknyttedeJournalposter() },
                { stubs.verifyStub.bidragPersonKalt() },
            )
        })
    }

    @Test
    @DisplayName("skal hente Journalpost når den eksisterer")
    @Throws(IOException::class)
    fun skalHenteJournalpostMedSamhandlerId() {
        val journalpostId = 201028011
        val samhandlerId = "123213"
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.leggTilSamhandlerId(samhandlerId)
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                journalstatus = JournalStatus.JOURNALFOERT,
                tilleggsopplysninger = tilleggsOpplysninger,
                sak = Sak("5276661"),
                avsenderMottaker = AvsenderMottaker(navn = "Samhandler navn"),
            ),
        )
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonDto(PERSON_IDENT, aktørId = AKTOR_IDENT), HttpStatus.OK)
        val responseEntity =
            httpHeaderTestRestTemplate.getForEntity<JournalpostResponse>(initUrl() + "/journal/JOARK-" + journalpostId + "?saksnummer=5276661")
        assertSoftly {
            responseEntity.statusCode shouldBe HttpStatus.OK

            val journalpost = responseEntity.body?.journalpost
            journalpost!!.avsenderMottaker!!.ident shouldBe samhandlerId
        }
    }

    @Test
    @DisplayName("skal hente journalposter for en bidragssak")
    @Throws(IOException::class)
    fun skalHenteJournalposterForEnBidragssak() {
        stubs.mockSafResponseDokumentOversiktFagsak()
        stubs.mockPersonResponse(PersonDto(PERSON_IDENT, aktørId = AKTOR_IDENT), HttpStatus.OK)
        val jouralposterResponseEntity =
            httpHeaderTestRestTemplate.getForEntity<List<JournalpostDto>>(initUrl() + "/sak/5276661/journal?fagomrade=BID")
        org.junit.jupiter.api.Assertions.assertAll(
            {
                Assertions.assertThat(jouralposterResponseEntity).extracting { it.statusCode }
                    .isEqualTo(HttpStatus.OK)
            },
            { Assertions.assertThat(jouralposterResponseEntity.body).hasSize(3) },
            { stubs.verifyStub.bidragPersonKalt() },
            { stubs.verifyStub.harSafEnKallEtterDokumentOversiktFagsak() },
        )
    }
}
