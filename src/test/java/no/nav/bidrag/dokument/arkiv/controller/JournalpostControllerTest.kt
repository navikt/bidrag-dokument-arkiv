package no.nav.bidrag.dokument.arkiv.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker
import no.nav.bidrag.dokument.arkiv.dto.DOKDIST_BESTILLING_ID
import no.nav.bidrag.dokument.arkiv.dto.DatoType
import no.nav.bidrag.dokument.arkiv.dto.DigitalpostSendt
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsInfo
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsTidspunkt
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsType
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.EpostVarselSendt
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal
import no.nav.bidrag.dokument.arkiv.dto.JournalpostType
import no.nav.bidrag.dokument.arkiv.dto.JournalstatusDto
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.dto.UtsendingsInfo
import no.nav.bidrag.dokument.arkiv.stubs.AVSENDER_ID
import no.nav.bidrag.dokument.arkiv.stubs.AVSENDER_NAVN
import no.nav.bidrag.dokument.arkiv.stubs.DATO_DOKUMENT
import no.nav.bidrag.dokument.arkiv.stubs.DATO_EKSPEDERT
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_ID
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_TITTEL
import no.nav.bidrag.dokument.arkiv.stubs.JOURNALPOST_ID
import no.nav.bidrag.dokument.arkiv.stubs.createDistribuerTilAdresse
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.transport.dokument.AvsenderMottakerDto
import no.nav.bidrag.transport.dokument.AvsenderMottakerDtoIdType
import no.nav.bidrag.transport.dokument.DistribuerJournalpostRequest
import no.nav.bidrag.transport.dokument.DistribuerJournalpostResponse
import no.nav.bidrag.transport.dokument.DistribuerTilAdresse
import no.nav.bidrag.transport.dokument.DistribusjonInfoDto
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.dokument.JournalpostStatus
import no.nav.bidrag.transport.dokument.ReturDetaljerLog
import no.nav.bidrag.domain.enums.Adressetype
import no.nav.bidrag.domain.string.Adresselinje1
import no.nav.bidrag.domain.string.Adresselinje2
import no.nav.bidrag.domain.string.Landkode2
import no.nav.bidrag.domain.string.Landkode3
import no.nav.bidrag.domain.string.Postnummer
import no.nav.bidrag.domain.string.Poststed
import no.nav.bidrag.transport.person.PersonAdresseDto
import no.nav.bidrag.transport.person.PersonDto
import org.assertj.core.api.Assertions
import org.json.JSONException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.function.Consumer

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
            initUrl() + "/journal/JOARK-1?saksnummer=007"
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
                    { stubs.verifyStub.harEnSafKallEtterHentJournalpost() }
                )
            }
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
                    { stubs.verifyStub.harEnSafKallEtterHentJournalpost() }
                )
            }
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
            HttpStatus.BAD_REQUEST
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
                    { stubs.verifyStub.bidragPersonKalt() }
                )
            }
        )
    }

    @Test
    @DisplayName("skal hente Journalpost uten saker når den eksisterer")
    @Throws(IOException::class)
    fun skalHenteJournalpostUtenSakerNarDenEksisterer() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostIdFraJson.toString(),
                sak = null
            )
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
                                AvsenderMottakerDtoIdType.FNR
                            )
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
                { stubs.verifyStub.bidragPersonKalt() }
            )
        })
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
        Assertions.assertThat(responseEntity).satisfies({
            org.junit.jupiter.api.Assertions.assertAll(
                { Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.OK) },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it.journalpostId }
                        .isEqualTo("JOARK-$journalpostIdFraJson")
                },
                {
                    Assertions.assertThat(journalpost).isNotNull.extracting { it.journalstatus }
                        .isEqualTo(JournalstatusDto.EKSPEDERT)
                },
                { Assertions.assertThat(distribuertTilAdresse).isNotNull() },
                {
                    Assertions.assertThat(distribuertTilAdresse!!.adresselinje1)
                        .isEqualTo("Testveien 20A")
                },
                {
                    Assertions.assertThat(distribuertTilAdresse!!.adresselinje2)
                        .isEqualTo("TestLinje2")
                },
                {
                    Assertions.assertThat(distribuertTilAdresse!!.adresselinje3)
                        .isEqualTo("TestLinje4")
                },
                { Assertions.assertThat(distribuertTilAdresse!!.postnummer).isEqualTo("7950") },
                { Assertions.assertThat(distribuertTilAdresse!!.poststed).isEqualTo("ABELVÆR") },
                { Assertions.assertThat(distribuertTilAdresse!!.land).isEqualTo("NO") },
                { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                { stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter() },
                { stubs.verifyStub.bidragPersonKalt() }
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
            HttpStatus.OK
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
                                false
                            )
                        )
                    ).isTrue()
                },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2020-12-14"),
                                "Beskrivelse av retur mer tekst for å teste lengre verdier",
                                false
                            )
                        )
                    ).isTrue()
                },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2022-12-15"),
                                "Beskrivelse av retur 2 mer tekst for å teste lengre verdier",
                                false
                            )
                        )
                    ).isTrue()
                },
                { Assertions.assertThat(returDetaljer!!.antall).isEqualTo(3) },
                {
                    Assertions.assertThat(returDetaljer!!.dato)
                        .isEqualTo(LocalDate.parse("2020-12-15"))
                },
                { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                { stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter() },
                { stubs.verifyStub.bidragPersonKalt() }
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
            HttpStatus.OK
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
                                true
                            )
                        )
                    ).isTrue()
                },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2020-12-14"),
                                "Beskrivelse av retur mer tekst for å teste lengre verdier",
                                true
                            )
                        )
                    ).isTrue()
                },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2022-12-15"),
                                "Beskrivelse av retur 2 mer tekst for å teste lengre verdier",
                                true
                            )
                        )
                    ).isTrue()
                },
                {
                    Assertions.assertThat(
                        returDetaljerLog!!.contains(
                            ReturDetaljerLog(
                                LocalDate.parse("2022-12-20"),
                                "Beskrivelse av retur uten lås",
                                false
                            )
                        )
                    ).isTrue()
                },
                { Assertions.assertThat(returDetaljer!!.antall).isEqualTo(4) },
                {
                    Assertions.assertThat(returDetaljer!!.dato)
                        .isEqualTo(LocalDate.parse("2020-12-15"))
                },
                { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                { stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter() },
                { stubs.verifyStub.bidragPersonKalt() }
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
                { stubs.verifyStub.bidragPersonKalt() }
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
                avsenderMottaker = AvsenderMottaker(navn = "Samhandler navn")
            )
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
            { stubs.verifyStub.harSafEnKallEtterDokumentOversiktFagsak() }
        )
    }

    @Test
    fun `skal distribuere journalpost`() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)

        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.setJournalfortAvIdent("Z99999")

        val tilleggsopplysningerEtterDist = TilleggsOpplysninger()
        tilleggsopplysningerEtterDist.add(
            mapOf(
                "nokkel" to "dokdistBestillingsId",
                "verdi" to "asdsadasdsadasdasd"
            )
        )

        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(
                tilleggsopplysninger = tilleggsopplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT"))
            ),
            null,
            "ETTER_DIST"
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST",
            "ETTER_DIST2"
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST2",
            "ETTER_DIST3"
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661")
                )
            )
        )
        val distribuerTilAdresse = createDistribuerTilAdresse()
        distribuerTilAdresse.adresselinje2 = "Adresselinje2"
        distribuerTilAdresse.adresselinje3 = "Adresselinje3"
        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(request, headersMedEnhet)
        )

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            stubs.verifyStub.dokdistFordelingKalt(
                objectMapper.writeValueAsString(
                    DokDistDistribuerJournalpostRequest(
                        JOURNALPOST_ID,
                        "BI01A01",
                        null,
                        request.adresse,
                        null
                    )
                )
            )
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name)
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name)
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                request.adresse!!.adresselinje1,
                request.adresse!!.land
            )
            stubs.verifyStub.dokarkivIkkeOppdaterKalt(
                JOURNALPOST_ID,
                "datoDokument"
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                "{\"tilleggsopplysninger\":[" +
                        "{\"nokkel\":\"dokdistBestillingsId\",\"verdi\":\"asdsadasdsadasdasd\"}," +
                        "{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"}," +
                        "{\"nokkel\":\"distAdresse0\",\"verdi\":\"{\\\"adresselinje1\\\":\\\"Adresselinje1\\\",\\\"adresselinje2\\\":\\\"Adresselinje2\\\",\\\"adresselinje3\\\":\\\"Adresselinje3\\\",\\\"la\"}," +
                        "{\"nokkel\":\"distAdresse1\",\"verdi\":\"nd\\\":\\\"NO\\\",\\\"postnummer\\\":\\\"3000\\\",\\\"poststed\\\":\\\"Ingen\\\"}\"}," +
                        "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"},{\"nokkel\":\"distribuertAvIdent\",\"verdi\":\"aud-localhost\"}],\"dokumenter\":[]}"
            )
        }
    }

    @Test
    fun `skal distribuere journalpost og oppdatere dokumentdato`() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)

        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.setJournalfortAvIdent("Z99999")

        val tilleggsopplysningerEtterDist = TilleggsOpplysninger()
        tilleggsopplysningerEtterDist.add(
            mapOf(
                "nokkel" to "dokdistBestillingsId",
                "verdi" to "asdsadasdsadasdasd"
            )
        )

        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysninger),
            null,
            "ETTER_DIST"
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST",
            "ETTER_DIST2"
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST2",
            "ETTER_DIST3"
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661")
                )
            )
        )
        val distribuerTilAdresse = createDistribuerTilAdresse()
        distribuerTilAdresse.adresselinje2 = "Adresselinje2"
        distribuerTilAdresse.adresselinje3 = "Adresselinje3"
        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(request, headersMedEnhet)
        )

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            stubs.verifyStub.dokdistFordelingKalt(
                objectMapper.writeValueAsString(
                    DokDistDistribuerJournalpostRequest(
                        JOURNALPOST_ID,
                        "BI01A01",
                        null,
                        request.adresse,
                        null
                    )
                )
            )
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name)
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name)
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                request.adresse!!.adresselinje1,
                request.adresse!!.land
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                "datoDokument"
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                "{\"tilleggsopplysninger\":[" +
                        "{\"nokkel\":\"dokdistBestillingsId\",\"verdi\":\"asdsadasdsadasdasd\"}," +
                        "{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"}," +
                        "{\"nokkel\":\"distAdresse0\",\"verdi\":\"{\\\"adresselinje1\\\":\\\"Adresselinje1\\\",\\\"adresselinje2\\\":\\\"Adresselinje2\\\",\\\"adresselinje3\\\":\\\"Adresselinje3\\\",\\\"la\"}," +
                        "{\"nokkel\":\"distAdresse1\",\"verdi\":\"nd\\\":\\\"NO\\\",\\\"postnummer\\\":\\\"3000\\\",\\\"poststed\\\":\\\"Ingen\\\"}\"}," +
                        "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"},{\"nokkel\":\"distribuertAvIdent\",\"verdi\":\"aud-localhost\"}],\"dokumenter\":[]}"
            )
        }
    }

    @Test
    fun skalDistribuereJournalpostMedTemaFAR() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val journalpostId = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        val safresponse = opprettSafResponse(
            journalpostId.toString(),
            journalpostType = JournalpostType.U,
            journalstatus = JournalStatus.FERDIGSTILT,
            avsenderMottaker = AvsenderMottaker(navn = "Samhandler Navnesen")
        )
            .copy(tema = "FAR")
        stubs.mockSafResponseHentJournalpost(safresponse)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    journalpostId,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661")
                )
            )
        )
        val distribuerTilAdresse = createDistribuerTilAdresse()
        distribuerTilAdresse.adresselinje2 = "Adresselinje2"
        distribuerTilAdresse.adresselinje3 = "Adresselinje3"
        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostId,
            HttpEntity(request, headersMedEnhet)

        )

        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            stubs.verifyStub.dokdistFordelingKalt(
                objectMapper.writeValueAsString(
                    DokDistDistribuerJournalpostRequest(
                        journalpostId,
                        "BI01A06",
                        null,
                        request.adresse,
                        null
                    )
                )
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}"
            )
        }
    }

    @Test
    @DisplayName("skal distribuere journalpost med bare mottakernavn")
    @Throws(IOException::class, JSONException::class)
    fun skalDistribuereJournalpostMedBareMottakerNavn() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val journalpostId = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        val safresponse = opprettSafResponse(
            journalpostId.toString(),
            journalpostType = JournalpostType.U,
            journalstatus = JournalStatus.FERDIGSTILT,
            avsenderMottaker = AvsenderMottaker(navn = "Samhandler Navnesen")
        )
        stubs.mockSafResponseHentJournalpost(safresponse)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    journalpostId,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661")
                )
            )
        )
        val distribuerTilAdresse = createDistribuerTilAdresse()
        distribuerTilAdresse.adresselinje2 = "Adresselinje2"
        distribuerTilAdresse.adresselinje3 = "Adresselinje3"
        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostId,
            HttpEntity(request, headersMedEnhet)
        )

        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            stubs.verifyStub.dokdistFordelingKalt(
                objectMapper.writeValueAsString(
                    DokDistDistribuerJournalpostRequest(
                        journalpostId,
                        "BI01A06",
                        null,
                        request.adresse,
                        null
                    )
                )
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}"
            )
        }
    }

    @Test
    fun `skal hente distribusjonsinfo`() {
        // given
        val journalpostId = 201028011L
        val bestillingId = "asdasdasdsadsadas"
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribuertAvIdent("Z99999")
        tilleggsOpplysninger.add(mapOf("nokkel" to DOKDIST_BESTILLING_ID, "verdi" to bestillingId))
        stubs.mockSafHentDistribusjonsInfo(
            DistribusjonsInfo(
                journalstatus = "EKSPEDERT",
                journalposttype = "U",
                kanal = JournalpostKanal.NAV_NO,
                utsendingsinfo = UtsendingsInfo(
                    digitalpostSendt = DigitalpostSendt("test@nav.no")
                ),
                tilleggsopplysninger = tilleggsOpplysninger,
                relevanteDatoer = listOf(DATO_DOKUMENT)
            ),
            journalpostId
        )
        // when
        val response =
            httpHeaderTestRestTemplate.getForEntity<DistribusjonInfoDto>(initUrl() + "/journal/distribuer/info/JOARK-" + journalpostId)

        response.statusCode shouldBe HttpStatus.OK

        assertSoftly {
            val responseBody = response.body!!
            responseBody.journalstatus shouldBe JournalpostStatus.EKSPEDERT
            responseBody.kanal shouldBe "NAV_NO"
            responseBody.utsendingsinfo?.adresse shouldBe "test@nav.no"
            responseBody.distribuertDato shouldBe LocalDateTime.parse(DATO_DOKUMENT.dato)
            responseBody.distribuertAvIdent shouldBe "Z99999"
            responseBody.bestillingId shouldBe bestillingId
        }
    }

    @Test
    fun `skal hente distribusjonsinfo med epostvarsel og distribuert status`() {
        // given
        val journalpostId = 201028011L
        val bestillingId = "asdasdasdsadsadas"
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribuertAvIdent("Z99999")
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.add(mapOf("nokkel" to DOKDIST_BESTILLING_ID, "verdi" to bestillingId))
        stubs.mockSafHentDistribusjonsInfo(
            DistribusjonsInfo(
                journalstatus = "FERDIGSTILT",
                journalposttype = "U",
                kanal = JournalpostKanal.NAV_NO,
                utsendingsinfo = UtsendingsInfo(
                    epostVarselSendt = EpostVarselSendt("test@nav.no", "tittel", "varslingtekst")
                ),
                tilleggsopplysninger = tilleggsOpplysninger,
                relevanteDatoer = listOf(DATO_DOKUMENT)
            ),
            journalpostId
        )
        // when
        val response =
            httpHeaderTestRestTemplate.getForEntity<DistribusjonInfoDto>(initUrl() + "/journal/distribuer/info/JOARK-" + journalpostId)

        response.statusCode shouldBe HttpStatus.OK

        assertSoftly {
            val responseBody = response.body!!
            responseBody.journalstatus shouldBe JournalpostStatus.DISTRIBUERT
            responseBody.kanal shouldBe "NAV_NO"
            responseBody.utsendingsinfo?.adresse shouldBe "test@nav.no"
            responseBody.utsendingsinfo?.tittel shouldBe "tittel"
            responseBody.utsendingsinfo?.varslingstekst shouldBe "varslingtekst"
            responseBody.distribuertDato shouldBe LocalDateTime.parse(DATO_DOKUMENT.dato)
            responseBody.distribuertAvIdent shouldBe "Z99999"
            responseBody.bestillingId shouldBe bestillingId
        }
    }

    @Test
    fun `skal markere journalpost distribuert lokalt`() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val journalpostId = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        val safresponse = opprettSafResponse(
            journalpostId.toString(),
            journalpostType = JournalpostType.U,
            journalstatus = JournalStatus.FERDIGSTILT
        )
        stubs.mockSafResponseHentJournalpost(safresponse)
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setJournalfortAvIdent("Z99999")
        stubs.mockSafResponseHentJournalpost(
            safresponse.copy(
                tilleggsopplysninger = tilleggsOpplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT"))
            ),
            null,
            "ETTER_DIST"
        )
        stubs.mockSafResponseHentJournalpost(
            safresponse.copy(
                tilleggsopplysninger = tilleggsOpplysninger,
                journalstatus = JournalStatus.EKSPEDERT
            ),
            "ETTER_DIST",
            null
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    journalpostId,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661")
                )
            )
        )
        stubs.mockDokarkivOppdaterDistribusjonsInfoRequest(journalpostId)
        val request = DistribuerJournalpostRequest(lokalUtskrift = true)

        // when
        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostId,
            HttpMethod.POST,
            HttpEntity(request, headersMedEnhet),
            JournalpostDto::class.java
        )

        response.statusCode shouldBe HttpStatus.OK

        assertSoftly {
            stubs.verifyStub.dokdistFordelingIkkeKalt()
            stubs.verifyStub.dokarkivOppdaterDistribusjonsInfoKalt(
                journalpostId,
                "{\"settStatusEkspedert\":true,\"utsendingsKanal\":\"L\"}"
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "{\"tilleggsopplysninger\":[{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"},{\"nokkel\":\"distribuertAvIdent\",\"verdi\":\"aud-localhost\"}],\"dokumenter\":[]}"
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "{\"dokumenter\":[{\"dokumentInfoId\":\"$DOKUMENT_1_ID\",\"tittel\":\"Tittel på dokument 1 (dokumentet er sendt per post med vedlegg)\"}]}"
            )
        }
    }

    @Test
    fun `skal markere journalpost distribuert lokalt farskap`() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val journalpostId = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        val safresponse = opprettSafResponse(
            journalpostId.toString(),
            journalpostType = JournalpostType.U,
            journalstatus = JournalStatus.FERDIGSTILT,
            tema = "FAR"
        )
        stubs.mockSafResponseHentJournalpost(safresponse)
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setJournalfortAvIdent("Z99999")
        stubs.mockSafResponseHentJournalpost(
            safresponse.copy(
                tilleggsopplysninger = tilleggsOpplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT"))
            ),
            null,
            "ETTER_DIST"
        )
        stubs.mockSafResponseHentJournalpost(
            safresponse.copy(
                tilleggsopplysninger = tilleggsOpplysninger,
                journalstatus = JournalStatus.EKSPEDERT
            ),
            "ETTER_DIST",
            null
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    journalpostId,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661")
                )
            )
        )
        stubs.mockDokarkivOppdaterDistribusjonsInfoRequest(journalpostId)
        val request = DistribuerJournalpostRequest(lokalUtskrift = true)

        // when
        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostId,
            HttpMethod.POST,
            HttpEntity(request, headersMedEnhet),
            JournalpostDto::class.java
        )

        response.statusCode shouldBe HttpStatus.OK

        assertSoftly {
            stubs.verifyStub.dokdistFordelingIkkeKalt()
            stubs.verifyStub.dokarkivOppdaterDistribusjonsInfoKalt(
                journalpostId,
                "{\"settStatusEkspedert\":true,\"utsendingsKanal\":\"L\"}"
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "{\"tilleggsopplysninger\":[{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"},{\"nokkel\":\"distribuertAvIdent\",\"verdi\":\"aud-localhost\"}],\"dokumenter\":[]}"
            )
            stubs.verifyStub.dokarkivIkkeOppdaterKalt(
                journalpostId,
                "{\"dokumenter\":[{\"dokumentInfoId\":\"$DOKUMENT_1_ID\",\"tittel\":\"Tittel på dokument 1 (dokumentet er sendt per post med vedlegg)\"}]}"
            )
        }
    }

    @Test
    @DisplayName("skal ikke distribuere journalpost hvis allerede distribuert")
    @Throws(IOException::class, JSONException::class)
    fun skalIkkeDistribuereJournalpostHvisAlleredeDistribuert() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val journalpostIdFraJson = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        stubs.mockSafResponseHentJournalpost(responseJournalpostJsonWithAdresse, HttpStatus.OK)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        val distribuerTilAdresse = createDistribuerTilAdresse()
        distribuerTilAdresse.adresselinje2 = "Adresselinje2"
        distribuerTilAdresse.adresselinje3 = "Adresselinje3"
        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val oppdaterJournalpostResponseEntity =
            httpHeaderTestRestTemplate.postForEntity<DistribuerJournalpostResponse>(
                initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson,
                HttpEntity(request, headersMedEnhet)
            )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting<DistribuerJournalpostResponse> { it.body }
                    .extracting { it.journalpostId }
                    .`as`("journalpostId")
                    .isEqualTo("JOARK-201028011")
            },
            { stubs.verifyStub.dokdistFordelingIkkeKalt() }
        )
    }

    @Test
    @DisplayName("skal distribuere journalpost med batchid")
    @Throws(IOException::class, JSONException::class)
    fun skalDistribuereJournalpostMedBatchId() {
        // given
        val xEnhet = "1234"
        val batchId = "FB201"
        val bestillingId = "TEST_BEST_ID"
        val mottakerId = "12312321312321"
        val journalpostIdFraJson = 201028011L
        val headersMedEnhet = HttpHeaders()
        val postadresse = PersonAdresseDto(
            adressetype = Adressetype.BOSTEDSADRESSE,
            adresselinje1 = Adresselinje1("Ramsegata 1"),
            adresselinje2 = Adresselinje2("Bakredør"),
            adresselinje3 = null,
            postnummer = Postnummer("3939"),
            poststed = Poststed("OSLO"),
            land = Landkode2("NO"),
            land3 = Landkode3("NOR")
        )

        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        stubs.mockSafResponseHentJournalpost("journalpostSafUtgaaendeResponse.json", HttpStatus.OK)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    journalpostIdFraJson,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661")
                )
            )
        )
        stubs.mockPersonAdresseResponse(postadresse)

        // when
        val oppdaterJournalpostResponseEntity =
            httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson + "?batchId=" + batchId)

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            {
                stubs.verifyStub.dokdistFordelingKalt(
                    objectMapper.writeValueAsString(
                        DokDistDistribuerJournalpostRequest(
                            journalpostIdFraJson,
                            "BI01A01",
                            null,
                            DistribuerTilAdresse(
                                postadresse.adresselinje1?.verdi,
                                postadresse.adresselinje2?.verdi,
                                postadresse.adresselinje3?.verdi,
                                postadresse.land.verdi,
                                postadresse.postnummer?.verdi,
                                postadresse.poststed?.verdi
                            ),
                            batchId
                        )
                    )
                )
            },
            { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name) },
            { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name) },
            { stubs.verifyStub.dokdistFordelingKalt(batchId) },
            { stubs.verifyStub.hentPersonAdresseKalt(mottakerId) },
            { stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "Ramsegata 1", "NO") },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostIdFraJson,
                    "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}"
                )
            }
        )
    }

    @Test
    @DisplayName("skal distribuere journalpost med tittel vedtak")
    @Throws(IOException::class, JSONException::class)
    fun skalDistribuereJournalpostMedTittelVedtak() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val journalpostIdFraJson = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        stubs.mockSafResponseHentJournalpost(
            "journalpostSafUtgaaendeResponseVedtakTittel.json",
            HttpStatus.OK
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        val distribuerTilAdresse = createDistribuerTilAdresse()
        distribuerTilAdresse.adresselinje2 = "Adresselinje2"
        distribuerTilAdresse.adresselinje3 = "Adresselinje3"
        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val oppdaterJournalpostResponseEntity =
            httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
                initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson,
                HttpEntity(request, headersMedEnhet)
            )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            {
                stubs.verifyStub.dokdistFordelingKalt(
                    objectMapper.writeValueAsString(
                        DokDistDistribuerJournalpostRequest(
                            journalpostIdFraJson,
                            "BI01H03",
                            "Brev som inneholder Vedtak",
                            request.adresse,
                            null
                        )
                    )
                )
            },
            { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name) },
            { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name) },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostIdFraJson,
                    request.adresse!!.adresselinje1,
                    request.adresse!!.land
                )
            },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostIdFraJson,
                    "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}"
                )
            }
        )
    }

    @Test
    @DisplayName("skal feile med BAD REQUEST når ugyldig journalpost distribuert")
    @Throws(IOException::class, JSONException::class)
    fun skalFeileMedBadRequestNaarUgyldigJournalpostDistribuert() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val journalpostIdFraJson = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        stubs.mockSafResponseHentJournalpost(
            "journalpostSafUtgaaendeResponseNoMottaker.json",
            HttpStatus.OK
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        val request = DistribuerJournalpostRequest(adresse = createDistribuerTilAdresse())

        // when
        val oppdaterJournalpostResponseEntity =
            httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
                initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson,
                HttpEntity(request, headersMedEnhet)
            )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.BAD_REQUEST)
            },
            { stubs.verifyStub.dokdistFordelingIkkeKalt() }
        )
    }

    @Test
    @DisplayName("skal få OK fra kan distribuere kall hvis gyldig journalpost for distribusjon")
    @Throws(IOException::class, JSONException::class)
    fun skalFaaOkFraKanDistribuereKall() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val journalpostIdFraJson = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        stubs.mockSafResponseHentJournalpost("journalpostSafUtgaaendeResponse.json", HttpStatus.OK)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        // when
        val response =
            httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson + "/enabled")

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            {
                Assertions.assertThat(response)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            }
        )
    }

    @Test
    @DisplayName("skal få NOT_ACCEPTABLE fra kan distribuere kall hvis ugyldig journalpost for distribusjon")
    @Throws(IOException::class, JSONException::class)
    fun skalFaaNotAcceptableFraKanDistribuereKall() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val journalpostIdFraJson = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        stubs.mockSafResponseHentJournalpost(
            "journalpostSafUtgaaendeResponseNoMottaker.json",
            HttpStatus.OK
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        // when
        val response = httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson + "/enabled",
            HttpEntity<Any>(headersMedEnhet)
        )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            {
                Assertions.assertThat(response)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.NOT_ACCEPTABLE)
            }
        )
    }
}
