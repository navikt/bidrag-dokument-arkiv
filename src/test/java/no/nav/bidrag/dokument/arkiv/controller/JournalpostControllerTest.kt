package no.nav.bidrag.dokument.arkiv.controller

import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.dokument.arkiv.dto.DatoType
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsTidspunkt
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsType
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.HentPostadresseResponse
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.JournalstatusDto
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse
import no.nav.bidrag.dokument.arkiv.dto.ReturDetaljerLogDO
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.stubs.AVSENDER_ID
import no.nav.bidrag.dokument.arkiv.stubs.AVSENDER_NAVN
import no.nav.bidrag.dokument.arkiv.stubs.DATO_DOKUMENT
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_TITTEL
import no.nav.bidrag.dokument.arkiv.stubs.RETUR_DETALJER_DATO_1
import no.nav.bidrag.dokument.arkiv.stubs.RETUR_DETALJER_DATO_2
import no.nav.bidrag.dokument.arkiv.stubs.createDistribuerTilAdresse
import no.nav.bidrag.dokument.arkiv.stubs.createEndreJournalpostCommand
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponseWithReturDetaljer
import no.nav.bidrag.dokument.dto.AvsenderMottakerDto
import no.nav.bidrag.dokument.dto.AvsenderMottakerDtoIdType
import no.nav.bidrag.dokument.dto.DistribuerJournalpostRequest
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse
import no.nav.bidrag.dokument.dto.EndreDokument
import no.nav.bidrag.dokument.dto.EndreJournalpostCommand
import no.nav.bidrag.dokument.dto.EndreReturDetaljer
import no.nav.bidrag.dokument.dto.JournalpostDto
import no.nav.bidrag.dokument.dto.JournalpostResponse
import no.nav.bidrag.dokument.dto.ReturDetaljerLog
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
        val journalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/1?saknummer=007",
            HttpMethod.GET,
            null,
            JournalpostDto::class.java
        )
        Assertions.assertThat(journalpostResponseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    @DisplayName("skal ha 400 BAD REQUEST når prefix er feil")
    fun skalHaBadRequestlNarPrefixPaJournalpostIdErFeil() {
        val journalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/BID-1?saksnummer=007",
            HttpMethod.GET,
            null,
            JournalpostDto::class.java
        )
        Assertions.assertThat(journalpostResponseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    @DisplayName("skal ha body som er null samt header warning når journalpost ikke finnes")
    @Throws(IOException::class)
    fun skalHaBodySomErNullSamtHeaderWarningNarJournalpostIkkeFinnes() {
        stubs.mockSafResponseHentJournalpost(journalpostSafNotFoundResponse, HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        val journalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-1?saksnummer=007",
            HttpMethod.GET,
            null,
            String::class.java
        )
        Assertions.assertThat(Optional.of(journalpostResponseEntity)).hasValueSatisfying(
            Consumer { response: ResponseEntity<String?> ->
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable { Assertions.assertThat(response.body).`as`("body").isNull() },
                    Executable {
                        Assertions.assertThat(response.headers[HttpHeaders.WARNING]).`as`("header warning").first()
                            .isEqualTo("Fant ikke journalpost i fagarkivet. journalpostId=910536260")
                    },
                    Executable { Assertions.assertThat(response.statusCode).`as`("status").isEqualTo(HttpStatus.NOT_FOUND) },
                    Executable { stubs.verifyStub.harEnSafKallEtterHentJournalpost() }
                )
            })
    }

    @Test
    @DisplayName("skal få 404 NOT FOUND når eksisterende journalpost er knyttet til annen sak")
    @Throws(IOException::class)
    fun skalFaNotFoundNarEksisterendeJournalpostErKnyttetTilAnnenSak() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        val responseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "?saksnummer=007",
            HttpMethod.GET,
            null,
            JournalpostResponse::class.java
        )
        Assertions.assertThat(Optional.of(responseEntity)).hasValueSatisfying(
            Consumer { response: ResponseEntity<JournalpostResponse?> ->
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                    Executable { Assertions.assertThat(response.body).isNull() },
                    Executable { stubs.verifyStub.harEnSafKallEtterHentJournalpost() }
                )
            })
    }

    @Test
    @DisplayName("skal få 500 INTERNAL SERVER når person api feiler")
    @Throws(IOException::class)
    fun skalFaServerFeilNarPersonTjenestenFeiler() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(responseJournalpostJson, HttpStatus.OK)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.BAD_REQUEST)
        val responseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
            HttpMethod.GET,
            null,
            JournalpostResponse::class.java
        )
        Assertions.assertThat(Optional.of(responseEntity)).hasValueSatisfying(
            Consumer { response: ResponseEntity<JournalpostResponse?> ->
                org.junit.jupiter.api.Assertions.assertAll(
                    Executable { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                    Executable { Assertions.assertThat(response.body).isNull() },
                    Executable { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                    Executable { stubs.verifyStub.bidragPersonKalt() }
                )
            })
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
            ))
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        val responseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
            HttpMethod.GET,
            null,
            JournalpostResponse::class.java
        )
        val journalpost = if (responseEntity.body != null) responseEntity.body!!.journalpost else null
        val saker = if (responseEntity.body != null) responseEntity.body!!.sakstilknytninger else null
        Assertions.assertThat(Optional.of(responseEntity)).hasValueSatisfying { response: ResponseEntity<JournalpostResponse?> ->
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                Executable { Assertions.assertThat(journalpost).isNotNull.extracting{it?.avsenderNavn}.isEqualTo(AVSENDER_NAVN) },
                Executable { Assertions.assertThat(journalpost).isNotNull.extracting{it?.avsenderMottaker}.isEqualTo(AvsenderMottakerDto(AVSENDER_NAVN, AVSENDER_ID, AvsenderMottakerDtoIdType.FNR)) },
                Executable { Assertions.assertThat(journalpost).isNotNull.extracting{it?.innhold}.isEqualTo(DOKUMENT_1_TITTEL) },
                Executable { Assertions.assertThat(journalpost).isNotNull.extracting{it?.journalpostId}.isEqualTo("JOARK-$journalpostIdFraJson") },
                Executable { Assertions.assertThat(journalpost).isNotNull.extracting{it?.gjelderAktor?.ident}.isEqualTo(PERSON_IDENT) },
                Executable { Assertions.assertThat(saker).isNotNull.hasSize(0) },
                Executable { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                Executable { stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter() },
                Executable { stubs.verifyStub.bidragPersonKalt() }
            )
        }
    }

    @Test
    @DisplayName("skal hente distribuert Journalpost med adresse")
    @Throws(IOException::class)
    fun skalHenteDistribuertJournalpostMedAdresse() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(responseJournalpostJsonWithAdresse, HttpStatus.OK)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        val responseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
            HttpMethod.GET,
            null,
            JournalpostResponse::class.java
        )
        val journalpost = if (responseEntity.body != null) responseEntity.body!!.journalpost else null
        val distribuertTilAdresse = journalpost!!.distribuertTilAdresse
        Assertions.assertThat(Optional.of(responseEntity)).hasValueSatisfying { response: ResponseEntity<JournalpostResponse?> ->
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                Executable { Assertions.assertThat(journalpost).isNotNull.extracting{it.journalpostId}.isEqualTo("JOARK-$journalpostIdFraJson") },
                Executable { Assertions.assertThat(journalpost).isNotNull.extracting{it.journalstatus}.isEqualTo(JournalstatusDto.EKSPEDERT) },
                Executable { Assertions.assertThat(distribuertTilAdresse).isNotNull() },
                Executable { Assertions.assertThat(distribuertTilAdresse!!.adresselinje1).isEqualTo("Testveien 20A") },
                Executable { Assertions.assertThat(distribuertTilAdresse!!.adresselinje2).isEqualTo("TestLinje2") },
                Executable { Assertions.assertThat(distribuertTilAdresse!!.adresselinje3).isEqualTo("TestLinje4") },
                Executable { Assertions.assertThat(distribuertTilAdresse!!.postnummer).isEqualTo("7950") },
                Executable { Assertions.assertThat(distribuertTilAdresse!!.poststed).isEqualTo("ABELVÆR") },
                Executable { Assertions.assertThat(distribuertTilAdresse!!.land).isEqualTo("NO") },
                Executable { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                Executable { stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter() },
                Executable { stubs.verifyStub.bidragPersonKalt() }
            )
        }
    }

    @Test
    @DisplayName("skal hente Journalpost med retur detaljer")
    @Throws(IOException::class)
    fun skalHenteJournalpostMedReturDetaljer() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(responseJournalpostJsonWithReturDetaljer, HttpStatus.OK)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        val responseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
            HttpMethod.GET,
            null,
            JournalpostResponse::class.java
        )
        val journalpost = if (responseEntity.body != null) responseEntity.body!!.journalpost else null
        val returDetaljer = journalpost!!.returDetaljer
        val returDetaljerLog = journalpost.returDetaljer!!.logg
        Assertions.assertThat(Optional.of(responseEntity)).hasValueSatisfying { response: ResponseEntity<JournalpostResponse?> ->
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                Executable {Assertions.assertThat(journalpost).isNotNull.extracting{it.journalpostId}.isEqualTo("JOARK-$journalpostIdFraJson") },
                Executable { Assertions.assertThat(returDetaljer).isNotNull() },
                Executable { Assertions.assertThat(returDetaljerLog!!.size).isEqualTo(3) },
                Executable {
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
                Executable {
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
                Executable {
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
                Executable { Assertions.assertThat(returDetaljer!!.antall).isEqualTo(3) },
                Executable { Assertions.assertThat(returDetaljer!!.dato).isEqualTo(LocalDate.parse("2020-12-15")) },
                Executable { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                Executable { stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter() },
                Executable { stubs.verifyStub.bidragPersonKalt() }
            )
        }
    }

    @Test
    @DisplayName("skal hente Journalpost med låste retur detaljer")
    @Throws(IOException::class)
    fun skalHenteJournalpostMedLaasteReturDetaljer() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost("journalpostSafLockedReturDetaljerResponse.json", HttpStatus.OK)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        val responseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostIdFraJson,
            HttpMethod.GET,
            null,
            JournalpostResponse::class.java
        )
        val journalpost = if (responseEntity.body != null) responseEntity.body!!.journalpost else null
        val returDetaljer = journalpost!!.returDetaljer
        val returDetaljerLog = journalpost.returDetaljer!!.logg
        Assertions.assertThat(Optional.of(responseEntity)).hasValueSatisfying { response: ResponseEntity<JournalpostResponse?> ->
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                Executable {Assertions.assertThat(journalpost).isNotNull.extracting{it.journalpostId}.isEqualTo("JOARK-$journalpostIdFraJson") },
                Executable { Assertions.assertThat(returDetaljer).isNotNull() },
                Executable { Assertions.assertThat(returDetaljerLog!!.size).isEqualTo(4) },
                Executable {
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
                Executable {
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
                Executable {
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
                Executable {
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
                Executable { Assertions.assertThat(returDetaljer!!.antall).isEqualTo(4) },
                Executable { Assertions.assertThat(returDetaljer!!.dato).isEqualTo(LocalDate.parse("2020-12-15")) },
                Executable { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                Executable { stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter() },
                Executable { stubs.verifyStub.bidragPersonKalt() }
            )
        }
    }

    @Test
    @DisplayName("skal hente Journalpost når den eksisterer")
    @Throws(IOException::class)
    fun skalHenteJournalpostNarDenEksisterer() {
        val journalpostIdFraJson = 201028011
        stubs.mockSafResponseHentJournalpost(journalpostJournalfortSafResponse, HttpStatus.OK)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        val responseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/JOARK-" + journalpostIdFraJson + "?saksnummer=5276661",
            HttpMethod.GET,
            null,
            JournalpostResponse::class.java
        )
        val journalpost = if (responseEntity.body != null) responseEntity.body!!.journalpost else null
        val saker = if (responseEntity.body != null) responseEntity.body!!.sakstilknytninger else null
        Assertions.assertThat(Optional.of(responseEntity)).hasValueSatisfying { response: ResponseEntity<JournalpostResponse?> ->
            org.junit.jupiter.api.Assertions.assertAll(
                Executable { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                Executable {
                    Assertions.assertThat(journalpost).isNotNull.extracting{it?.innhold}.isEqualTo("Filosofens bidrag")
                },
                Executable { Assertions.assertThat(journalpost).isNotNull.extracting{it?.journalpostId}.isEqualTo("JOARK-$journalpostIdFraJson") },
                Executable {
                    Assertions.assertThat(journalpost).isNotNull.extracting{it?.gjelderAktor}.extracting{it?.ident}.isEqualTo(PERSON_IDENT)
                },
                Executable {
                    Assertions.assertThat(journalpost).isNotNull.extracting{it?.brevkode?.kode}.isEqualTo("BI01S02")
                },
                Executable { Assertions.assertThat(saker).isNotNull.hasSize(3).contains("2106585").contains("5276661") },
                Executable { stubs.verifyStub.harEnSafKallEtterHentJournalpost() },
                Executable { stubs.verifyStub.harEnSafKallEtterTilknyttedeJournalposter() },
                Executable { stubs.verifyStub.bidragPersonKalt() }
            )
        }
    }

    @Test
    @DisplayName("skal hente journalposter for en bidragssak")
    @Throws(IOException::class)
    fun skalHenteJournalposterForEnBidragssak() {
        stubs.mockSafResponseDokumentOversiktFagsak()
        stubs.mockPersonResponse(PersonResponse(PERSON_IDENT, AKTOR_IDENT), HttpStatus.OK)
        val jouralposterResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/sak/5276661/journal?fagomrade=BID", HttpMethod.GET, null, listeMedJournalposterTypeReference()
        )
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(jouralposterResponseEntity).extracting{ it.statusCode }.isEqualTo(HttpStatus.OK)},
            Executable { Assertions.assertThat(jouralposterResponseEntity.body).hasSize(3) },
            Executable { stubs.verifyStub.bidragPersonKalt() },
            Executable { stubs.verifyStub.harSafEnKallEtterDokumentOversiktFagsak() }
        )
    }

    private fun listeMedJournalposterTypeReference(): ParameterizedTypeReference<List<JournalpostDto>> {
        return object : ParameterizedTypeReference<List<JournalpostDto>>() {}
    }

    @Test
    @DisplayName("skal distribuere journalpost")
    @Throws(IOException::class, JSONException::class)
    fun skalDistribuereJournalpost() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val journalpostIdFraJson = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        stubs.mockSafResponseHentJournalpost("journalpostSafUtgaaendeResponse.json", HttpStatus.OK)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockSafResponseTilknyttedeJournalposter(listOf(TilknyttetJournalpost(journalpostIdFraJson, JournalStatus.FERDIGSTILT, Sak("5276661"))))
        val distribuerTilAdresse = createDistribuerTilAdresse()
        distribuerTilAdresse.adresselinje2 = "Adresselinje2"
        distribuerTilAdresse.adresselinje3 = "Adresselinje3"
        val request = DistribuerJournalpostRequest(distribuerTilAdresse)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson,
            HttpMethod.POST,
            HttpEntity(request, headersMedEnhet),
            JournalpostDto::class.java
        )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            Executable {
                stubs.verifyStub.dokdistFordelingKalt(
                    objectMapper.writeValueAsString(
                        DokDistDistribuerJournalpostRequest(
                            journalpostIdFraJson,
                            "BI01A01",
                            null,
                            request.adresse,
                            null
                        )
                    )
                )
            },
            Executable { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name) },
            Executable { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name) },
            Executable { stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, request.adresse!!.adresselinje1, request.adresse!!.land) },
            Executable { stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}") }
        )
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
        val request = DistribuerJournalpostRequest(distribuerTilAdresse)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson,
            HttpMethod.POST,
            HttpEntity(request, headersMedEnhet),
            DistribuerJournalpostResponse::class.java
        )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<DistribuerJournalpostResponse?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            Executable {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting<DistribuerJournalpostResponse> { obj: ResponseEntity<DistribuerJournalpostResponse?> -> obj.body }
                    .extracting{it.journalpostId}
                    .`as`("journalpostId")
                    .isEqualTo("JOARK-201028011")
            },
            Executable { stubs.verifyStub.dokdistFordelingIkkeKalt() }
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
        val postadresse = HentPostadresseResponse("Ramsegata 1", "Bakredør", null, "3939", "OSLO", "NO")
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        stubs.mockSafResponseHentJournalpost("journalpostSafUtgaaendeResponse.json", HttpStatus.OK)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockSafResponseTilknyttedeJournalposter(listOf(TilknyttetJournalpost(journalpostIdFraJson, JournalStatus.FERDIGSTILT, Sak("5276661"))))
        stubs.mockPersonAdresseResponse(postadresse)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson + "?batchId=" + batchId,
            HttpMethod.POST,
            HttpEntity<Any?>(null, headersMedEnhet),
            JournalpostDto::class.java
        )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            Executable {
                stubs.verifyStub.dokdistFordelingKalt(
                    objectMapper.writeValueAsString(
                        DokDistDistribuerJournalpostRequest(
                            journalpostIdFraJson, "BI01A01", null, DistribuerTilAdresse(
                                postadresse.adresselinje1,
                                postadresse.adresselinje2,
                                postadresse.adresselinje3,
                                postadresse.land, postadresse.postnummer, postadresse.poststed
                            ), batchId
                        )
                    )
                )
            },
            Executable { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name) },
            Executable { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name) },
            Executable { stubs.verifyStub.dokdistFordelingKalt(batchId) },
            Executable { stubs.verifyStub.hentPersonAdresseKalt(mottakerId) },
            Executable { stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "Ramsegata 1", "NO") },
            Executable { stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}") }
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
        stubs.mockSafResponseHentJournalpost("journalpostSafUtgaaendeResponseVedtakTittel.json", HttpStatus.OK)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        val distribuerTilAdresse = createDistribuerTilAdresse()
        distribuerTilAdresse.adresselinje2 = "Adresselinje2"
        distribuerTilAdresse.adresselinje3 = "Adresselinje3"
        val request = DistribuerJournalpostRequest(distribuerTilAdresse)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson,
            HttpMethod.POST,
            HttpEntity(request, headersMedEnhet),
            JournalpostDto::class.java
        )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.OK)
            },
            Executable {
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
            Executable { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name) },
            Executable { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name) },
            Executable { stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, request.adresse!!.adresselinje1, request.adresse!!.land) },
            Executable { stubs.verifyStub.dokarkivOppdaterKalt(journalpostIdFraJson, "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}") }
        )
    }

    @Test
    @DisplayName("skal ikke distribuere journalpost hvis oppdater journalpost")
    @Throws(IOException::class, JSONException::class)
    fun skalIkkeDistribuereJournalpostHvisOppdaterJournalpostFeiler() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val journalpostIdFraJson = 201028011L
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)
        stubs.mockSafResponseHentJournalpost("journalpostSafUtgaaendeResponse.json", HttpStatus.OK)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockSafResponseTilknyttedeJournalposter(listOf(TilknyttetJournalpost(journalpostIdFraJson, JournalStatus.FERDIGSTILT, Sak("5276661"))))
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson, HttpStatus.INTERNAL_SERVER_ERROR)
        val distribuerTilAdresse = createDistribuerTilAdresse()
        distribuerTilAdresse.adresselinje2 = "Adresselinje2"
        distribuerTilAdresse.adresselinje3 = "Adresselinje3"
        val request = DistribuerJournalpostRequest(distribuerTilAdresse)

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson,
            HttpMethod.POST,
            HttpEntity(request, headersMedEnhet),
            JournalpostDto::class.java
        )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            },
            Executable { stubs.verifyStub.dokdistFordelingIkkeKalt() }
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
        stubs.mockSafResponseHentJournalpost("journalpostSafUtgaaendeResponseNoMottaker.json", HttpStatus.OK)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        val request = DistribuerJournalpostRequest(createDistribuerTilAdresse())

        // when
        val oppdaterJournalpostResponseEntity = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson,
            HttpMethod.POST,
            HttpEntity(request, headersMedEnhet),
            JournalpostDto::class.java
        )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.BAD_REQUEST)
            },
            Executable { stubs.verifyStub.dokdistFordelingIkkeKalt() }
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
        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson + "/enabled",
            HttpMethod.GET,
            HttpEntity<Any>(headersMedEnhet),
            JournalpostDto::class.java
        )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(response)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
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
        stubs.mockSafResponseHentJournalpost("journalpostSafUtgaaendeResponseNoMottaker.json", HttpStatus.OK)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        // when
        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson + "/enabled",
            HttpMethod.GET,
            HttpEntity<Any>(headersMedEnhet),
            JournalpostDto::class.java
        )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(response)
                    .extracting { obj: ResponseEntity<JournalpostDto?> -> obj.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.NOT_ACCEPTABLE)
            }
        )
    }
}