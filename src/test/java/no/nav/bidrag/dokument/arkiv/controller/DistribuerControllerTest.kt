package no.nav.bidrag.dokument.arkiv.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.dokument.arkiv.consumer.BestemKanalResponse
import no.nav.bidrag.dokument.arkiv.consumer.DistribusjonsKanal
import no.nav.bidrag.dokument.arkiv.consumer.dto.VedleggDto
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker
import no.nav.bidrag.dokument.arkiv.dto.DOKDIST_BESTILLING_ID
import no.nav.bidrag.dokument.arkiv.dto.DatoType
import no.nav.bidrag.dokument.arkiv.dto.DigitalpostSendt
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsInfo
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsTidspunkt
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsType
import no.nav.bidrag.dokument.arkiv.dto.DokDistDistribuerJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.EpostVarselSendt
import no.nav.bidrag.dokument.arkiv.dto.EttersendingsoppgaveDo
import no.nav.bidrag.dokument.arkiv.dto.EttersendingsoppgaveDo.EttersendingsoppgaveVedleggDo
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal
import no.nav.bidrag.dokument.arkiv.dto.JournalpostType
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.dto.UtsendingsInfo
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_AKTOER_ID
import no.nav.bidrag.dokument.arkiv.stubs.DATO_DOKUMENT
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_ID
import no.nav.bidrag.dokument.arkiv.stubs.JOURNALPOST_ID
import no.nav.bidrag.dokument.arkiv.stubs.createDistribuerTilAdresse
import no.nav.bidrag.dokument.arkiv.stubs.opprettDokumentSoknadDto
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.domene.land.Landkode2
import no.nav.bidrag.domene.land.Landkode3
import no.nav.bidrag.transport.dokument.DistribuerJournalpostRequest
import no.nav.bidrag.transport.dokument.DistribuerJournalpostResponse
import no.nav.bidrag.transport.dokument.DistribuerTilAdresse
import no.nav.bidrag.transport.dokument.DistribusjonInfoDto
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostStatus
import no.nav.bidrag.transport.dokument.OpprettEttersendingsoppgaveVedleggDto
import no.nav.bidrag.transport.dokument.OpprettEttersendingsppgaveDto
import no.nav.bidrag.transport.person.PersonAdresseDto
import org.assertj.core.api.Assertions
import org.json.JSONException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.io.IOException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal class DistribuerControllerTest : AbstractControllerTest() {

    @BeforeEach
    fun initDistkanalResponse() {
        stubs.mockBestmDistribusjonskanal(
            BestemKanalResponse(
                regel = "",
                regelBegrunnelse = "",
                distribusjonskanal = DistribusjonsKanal.PRINT,
            ),
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
                "verdi" to "asdsadasdsadasdasd",
            ),
        )
        stubs.mockBestmDistribusjonskanal(
            BestemKanalResponse(
                regel = "",
                regelBegrunnelse = "",
                distribusjonskanal = DistribusjonsKanal.PRINT,
            ),
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(
                tilleggsopplysninger = tilleggsopplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ),
            null,
            "ETTER_DIST",
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST",
            "ETTER_DIST2",
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST2",
            "ETTER_DIST3",
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        val distribuerTilAdresse = createDistribuerTilAdresse()
            .copy(
                adresselinje2 = "Adresselinje2",
                adresselinje3 = "Adresselinje3",
            )

        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(request, headersMedEnhet),
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
                        null,
                    ),
                ),
            )
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name)
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name)
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                request.adresse!!.adresselinje1,
                request.adresse!!.land,
            )
            stubs.verifyStub.dokarkivIkkeOppdaterKalt(
                JOURNALPOST_ID,
                "datoDokument",
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                "{\"tilleggsopplysninger\":[" +
                    "{\"nokkel\":\"dokdistBestillingsId\",\"verdi\":\"asdsadasdsadasdasd\"}," +
                    "{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"}," +
                    "{\"nokkel\":\"distAdresse0\",\"verdi\":\"{\\\"adresselinje1\\\":\\\"Adresselinje1\\\",\\\"adresselinje2\\\":\\\"Adresselinje2\\\",\\\"adresselinje3\\\":\\\"Adresselinje3\\\",\\\"la\"}," +
                    "{\"nokkel\":\"distAdresse1\",\"verdi\":\"nd\\\":\\\"NO\\\",\\\"postnummer\\\":\\\"3000\\\",\\\"poststed\\\":\\\"Ingen\\\"}\"}," +
                    "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"},{\"nokkel\":\"distribuertAvIdent\",\"verdi\":\"aud-localhost\"}],\"dokumenter\":[]}",
            )
        }
    }

    @Test
    fun `skal distribuere journalpost med ettersending`() {
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
                "verdi" to "asdsadasdsadasdasd",
            ),
        )
        stubs.mockInnsendingApi(
            opprettDokumentSoknadDto().copy(
                fristForEttersendelse = 27,
                vedleggsListe = listOf(
                    VedleggDto(
                        tittel = "Tittel vedlegg 1",
                        vedleggsnr = "1231",
                        skjemaurl = "http://localhost:8080/vedlegg/1",
                        opprettetdato = OffsetDateTime.of(LocalDateTime.of(2022, 1, 1, 1, 1, 1), ZoneOffset.UTC),
                    ),
                    VedleggDto(
                        tittel = "Tittel vedlegg 2",
                        vedleggsnr = "1231",
                        opprettetdato = OffsetDateTime.of(LocalDateTime.of(2022, 1, 1, 1, 1, 1), ZoneOffset.UTC),
                    ),
                ),
            ),
        )
        stubs.mockHentInnsendingApi(
            listOf(
                opprettDokumentSoknadDto().copy(
                    skjemanr = "NAV 10-07.10",
                    brukerId = BRUKER_AKTOER_ID,
                    opprettetDato = OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC),
                ),
            ),
        )
        stubs.mockBestmDistribusjonskanal(
            BestemKanalResponse(
                regel = "",
                regelBegrunnelse = "",
                distribusjonskanal = DistribusjonsKanal.PRINT,
            ),
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(
                tilleggsopplysninger = tilleggsopplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ),
            null,
            "ETTER_DIST",
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST",
            "ETTER_DIST2",
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST2",
            "ETTER_DIST3",
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        val distribuerTilAdresse = createDistribuerTilAdresse()
            .copy(
                adresselinje2 = "Adresselinje2",
                adresselinje3 = "Adresselinje3",
            )

        val request = DistribuerJournalpostRequest(
            adresse = distribuerTilAdresse,
            ettersendingsoppgave =
            OpprettEttersendingsppgaveDto(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = Språk.NB,
                innsendingsFristDager = 27,
                vedleggsliste = listOf(
                    OpprettEttersendingsoppgaveVedleggDto(
                        tittel = "Vedlegg 1",
                        url = "http://localhost:8080/vedlegg/1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<DistribuerJournalpostResponse>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(request, headersMedEnhet),
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
                        null,
                    ),
                ),
            )
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name)
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name)
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                request.adresse!!.adresselinje1,
                request.adresse!!.land,
            )
            stubs.verifyStub.dokarkivIkkeOppdaterKalt(
                JOURNALPOST_ID,
                "datoDokument",
            )
            response.body?.ettersendingsoppgave?.innsendingsId shouldBe "213213"
            stubs.verifyStub.opprettEttersendingKalt(
                1,
                "{\"brukerId\":\"12345678910\"," +
                    "\"skjemanr\":\"NAV 10-07.17\",\"sprak\":\"nb_NO\",\"tema\":\"BID\"," +
                    "\"vedleggsListe\":[{\"vedleggsnr\":\"NAV 10-07.17\",\"tittel\":\"Vedlegg 1\",\"url\":null,\"opplastingsValgKommentarLedetekst\":null,\"opplastingsValgKommentar\":null}]," +
                    "\"tittel\":\"Tittel\",\"brukernotifikasjonstype\":\"oppgave\",\"koblesTilEksisterendeSoknad\":true,\"innsendingsFristDager\":27,\"mellomlagringDager\":null}",
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                "{\"tilleggsopplysninger\":[{\"nokkel\":\"dokdistBestillingsId\",\"verdi\":\"asdsadasdsadasdasd\"}," +
                    "{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"},{\"nokkel\":\"ettOppgave0\",\"verdi\":\"{\\\"tittel\\\":\\\"Tittel dokument\\\",\\\"skjemaId\\\":\\\"NAV 123\\\",\\\"språk\\\":\\\"nb\\\",\\\"innsendingsId\\\":\\\"213213\\\",\\\"innsendingsF\"}," +
                    "{\"nokkel\":\"ettOppgave1\",\"verdi\":\"ristDager\\\":27,\\\"fristDato\\\":\\\"2022-01-01\\\",\\\"slettesDato\\\":\\\"2022-01-01\\\",\\\"vedleggsliste\\\":[{\\\"tittel\\\":\\\"Tittel\"}," +
                    "{\"nokkel\":\"ettOppgave2\",\"verdi\":\" vedlegg 1\\\",\\\"url\\\":\\\"http://localhost:8080/vedlegg/1\\\",\\\"vedleggsnr\\\":\\\"1231\\\"},{\\\"tittel\\\":\\\"Tittel vedlegg 2\"}," +
                    "{\"nokkel\":\"ettOppgave3\",\"verdi\":\"\\\",\\\"url\\\":null,\\\"vedleggsnr\\\":\\\"1231\\\"}]}\"}," +
                    "{\"nokkel\":\"distAdresse0\",\"verdi\":\"{\\\"adresselinje1\\\":\\\"Adresselinje1\\\",\\\"adresselinje2\\\":\\\"Adresselinje2\\\",\\\"adresselinje3\\\":\\\"Adresselinje3\\\",\\\"la\"}," +
                    "{\"nokkel\":\"distAdresse1\",\"verdi\":\"nd\\\":\\\"NO\\\",\\\"postnummer\\\":\\\"3000\\\",\\\"poststed\\\":\\\"Ingen\\\"}\"}," +
                    "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}," +
                    "{\"nokkel\":\"distribuertAvIdent\",\"verdi\":\"aud-localhost\"}],\"dokumenter\":[]}",
            )
        }
    }

    @Test
    fun `skal opprette ettersending hvis journalpost allerede er distribuert`() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)

        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.setJournalfortAvIdent("Z99999")
        tilleggsopplysninger.setDistribusjonBestillt()
        tilleggsopplysninger.addInnsendingsOppgave(
            EttersendingsoppgaveDo(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = "nb",
                innsendingsFristDager = 1,
                vedleggsliste = listOf(
                    EttersendingsoppgaveVedleggDo(
                        tittel = "Vedlegg 1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )
        tilleggsopplysninger.add(
            mapOf(
                "nokkel" to "dokdistBestillingsId",
                "verdi" to "asdsadasdsadasdasd",
            ),
        )
        stubs.mockInnsendingApi(opprettDokumentSoknadDto().copy(fristForEttersendelse = 27))
        stubs.mockHentInnsendingApi(
            listOf(
                opprettDokumentSoknadDto().copy(
                    skjemanr = "NAV 10-07.10",
                    brukerId = BRUKER_AKTOER_ID,
                    opprettetDato = OffsetDateTime.of(LocalDateTime.now().minusDays(5), ZoneOffset.UTC),
                ),
            ),
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(
                tilleggsopplysninger = tilleggsopplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ).copy(
                relevanteDatoer = listOf(
                    DatoType(LocalDateTime.now().toString(), "DATO_JOURNALFOERT"),
                ),
            ),
        )

        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        val request = DistribuerJournalpostRequest(
            ettersendingsoppgave =
            OpprettEttersendingsppgaveDto(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = Språk.NB,
                innsendingsFristDager = 27,
                vedleggsliste = listOf(
                    OpprettEttersendingsoppgaveVedleggDto(
                        tittel = "Vedlegg 1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<DistribuerJournalpostResponse>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(request, headersMedEnhet),
        )

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            stubs.verifyStub.dokdistFordelingIkkeKalt()
            stubs.verifyStub.opprettEttersendingKalt(1)

            response.body?.ettersendingsoppgave?.innsendingsId shouldBe "213213"
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                "{\"tilleggsopplysninger\":[{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"}," +
                    "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}," +
                    "{\"nokkel\":\"dokdistBestillingsId\",\"verdi\":\"asdsadasdsadasdasd\"}," +
                    "{\"nokkel\":\"ettOppgave0\",\"verdi\":\"{\\\"tittel\\\":\\\"Tittel dokument\\\",\\\"skjemaId\\\":\\\"NAV 123\\\",\\\"språk\\\":\\\"nb\\\",\\\"innsendingsId\\\":\\\"213213\\\",\\\"innsendingsF\"}," +
                    "{\"nokkel\":\"ettOppgave1\",\"verdi\":\"ristDager\\\":27,\\\"fristDato\\\":\\\"2022-01-01\\\",\\\"slettesDato\\\":\\\"2022-01-01\\\",\\\"vedleggsliste\\\":[{\\\"tittel\\\":\\\"Tittel\"}," +
                    "{\"nokkel\":\"ettOppgave2\",\"verdi\":\" vedlegg 1\\\",\\\"url\\\":null,\\\"vedleggsnr\\\":\\\"1231\\\"},{\\\"tittel\\\":\\\"Tittel vedlegg 2\\\",\\\"url\\\":null,\\\"vedleggsnr\\\":\\\"12\"}," +
                    "{\"nokkel\":\"ettOppgave3\",\"verdi\":\"31\\\"}]}\"}],\"dokumenter\":[]}",
            )
        }
    }

    @Test
    fun `skal opprette ettersending hvis journalpost ikke er distribuert og er eldre enn 2 dager`() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)

        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.setJournalfortAvIdent("Z99999")

        tilleggsopplysninger.addInnsendingsOppgave(
            EttersendingsoppgaveDo(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = "nb",
                innsendingsFristDager = 1,
                vedleggsliste = listOf(
                    EttersendingsoppgaveVedleggDo(
                        tittel = "Vedlegg 1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )
        tilleggsopplysninger.add(
            mapOf(
                "nokkel" to "dokdistBestillingsId",
                "verdi" to "asdsadasdsadasdasd",
            ),
        )
        stubs.mockInnsendingApi()
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(
                tilleggsopplysninger = tilleggsopplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ).copy(
                relevanteDatoer = listOf(
                    DatoType(LocalDateTime.now().minusDays(10).toString(), "DATO_JOURNALFOERT"),
                ),
            ),
        )
        stubs.mockBestmDistribusjonskanal(
            BestemKanalResponse(
                regel = "",
                regelBegrunnelse = "",
                distribusjonskanal = DistribusjonsKanal.PRINT,
            ),
        )

        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        val distribuerTilAdresse = createDistribuerTilAdresse()
            .copy(
                adresselinje2 = "Adresselinje2",
                adresselinje3 = "Adresselinje3",
            )
        val request = DistribuerJournalpostRequest(
            adresse = distribuerTilAdresse,
            ettersendingsoppgave =
            OpprettEttersendingsppgaveDto(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = Språk.NB,
                innsendingsFristDager = 27,
                vedleggsliste = listOf(
                    OpprettEttersendingsoppgaveVedleggDto(
                        tittel = "Vedlegg 1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(request, headersMedEnhet),
        )

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            stubs.verifyStub.dokdistFordelingKalt()
            stubs.verifyStub.opprettEttersendingKalt(1)

            stubs.verifyStub.dokarkivOppdaterKalt(JOURNALPOST_ID)
        }
    }

    @Test
    @Disabled
    fun `skal ikke opprette ettersending hvis det finnes ettersending opprettet etter journalpost`() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)

        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.setJournalfortAvIdent("Z99999")
        tilleggsopplysninger.setDistribusjonBestillt()

        tilleggsopplysninger.addInnsendingsOppgave(
            EttersendingsoppgaveDo(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = "nb",
                innsendingsFristDager = 1,
                vedleggsliste = listOf(
                    EttersendingsoppgaveVedleggDo(
                        tittel = "Vedlegg 1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )
        tilleggsopplysninger.add(
            mapOf(
                "nokkel" to "dokdistBestillingsId",
                "verdi" to "asdsadasdsadasdasd",
            ),
        )
        stubs.mockInnsendingApi()
        stubs.mockHentInnsendingApi(
            listOf(
                opprettDokumentSoknadDto().copy(
                    skjemanr = "NAV 10-07.17",
                    innsendingsId = "INNSENDING_ID",
                    brukerId = BRUKER_AKTOER_ID,
                    opprettetDato = OffsetDateTime.of(LocalDateTime.now(), ZoneOffset.UTC),
                ),
                opprettDokumentSoknadDto().copy(
                    skjemanr = "NAV 10-07.17",
                    brukerId = BRUKER_AKTOER_ID,
                    opprettetDato = OffsetDateTime.of(LocalDateTime.now().minusDays(4), ZoneOffset.UTC),
                ),
            ),
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(
                tilleggsopplysninger = tilleggsopplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ).copy(
                relevanteDatoer = listOf(
                    DatoType(LocalDateTime.now().toString(), "DATO_JOURNALFOERT"),
                ),
            ),
        )

        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        val request = DistribuerJournalpostRequest(
            ettersendingsoppgave =
            OpprettEttersendingsppgaveDto(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = Språk.NB,
                innsendingsFristDager = 27,
                vedleggsliste = listOf(
                    OpprettEttersendingsoppgaveVedleggDto(
                        tittel = "Vedlegg 1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<DistribuerJournalpostResponse>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(request, headersMedEnhet),
        )

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            stubs.verifyStub.dokdistFordelingIkkeKalt()
            stubs.verifyStub.opprettEttersendingKalt(0)
            stubs.verifyStub.hentEttersendingKalt(1)

            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                "{\"tilleggsopplysninger\":[{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"}," +
                    "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}," +
                    "{\"nokkel\":\"dokdistBestillingsId\",\"verdi\":\"asdsadasdsadasdasd\"}," +
                    "{\"nokkel\":\"ettOppgave0\",\"verdi\":\"{\\\"tittel\\\":\\\"Tittel dokument\\\",\\\"skjemaId\\\":\\\"NAV 10-07.17\\\",\\\"språk\\\":\\\"nb\\\",\\\"innsendingsId\\\":\\\"INNSENDING_ID\\\",\\\"\"}," +
                    "{\"nokkel\":\"ettOppgave1\",\"verdi\":\"innsendingsFristDager\\\":14,\\\"fristDato\\\":\\\"2022-01-01\\\",\\\"slettesDato\\\":\\\"2022-01-01\\\",\\\"vedleggsliste\\\":[{\\\"tit\"}," +
                    "{\"nokkel\":\"ettOppgave2\",\"verdi\":\"tel\\\":\\\"Tittel vedlegg 1\\\",\\\"vedleggsnr\\\":\\\"1231\\\"},{\\\"tittel\\\":\\\"Tittel vedlegg 2\\\",\\\"vedleggsnr\\\":\\\"1231\\\"}]}\"}],\"dokumenter\":[]}",
            )
            response.body.ettersendingsoppgave?.innsendingsId shouldBe "INNSENDING_ID"
        }
    }

    @Test
    fun `skal ikke opprette ettersending hvis journalpost allerede er distribuert og er eldre enn 2 dager`() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)

        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.setJournalfortAvIdent("Z99999")
        tilleggsopplysninger.setDistribusjonBestillt()

        tilleggsopplysninger.addInnsendingsOppgave(
            EttersendingsoppgaveDo(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = "nb",
                innsendingsFristDager = 1,
                vedleggsliste = listOf(
                    EttersendingsoppgaveVedleggDo(
                        tittel = "Vedlegg 1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )
        tilleggsopplysninger.add(
            mapOf(
                "nokkel" to "dokdistBestillingsId",
                "verdi" to "asdsadasdsadasdasd",
            ),
        )
        stubs.mockInnsendingApi()
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(
                tilleggsopplysninger = tilleggsopplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ).copy(
                relevanteDatoer = listOf(
                    DatoType(LocalDateTime.now().minusDays(10).toString(), "DATO_JOURNALFOERT"),
                ),
            ),
        )

        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        val request = DistribuerJournalpostRequest(
            ettersendingsoppgave =
            OpprettEttersendingsppgaveDto(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = Språk.NB,
                innsendingsFristDager = 27,
                vedleggsliste = listOf(
                    OpprettEttersendingsoppgaveVedleggDto(
                        tittel = "Vedlegg 1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<DistribuerJournalpostResponse>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(request, headersMedEnhet),
        )

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            stubs.verifyStub.dokdistFordelingIkkeKalt()
            stubs.verifyStub.opprettEttersendingKalt(0)

            stubs.verifyStub.dokarkivOppdaterIkkeKalt(JOURNALPOST_ID)
            response.body.ettersendingsoppgave?.innsendingsId shouldBe null
        }
    }

    @Test
    fun `skal ikke opprette ettersending hvis journalpost allerede er distribuert og ettersending opprettet uten ettersending forespørsel`() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)

        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.setJournalfortAvIdent("Z99999")
        tilleggsopplysninger.setDistribusjonBestillt()

        tilleggsopplysninger.addInnsendingsOppgave(
            EttersendingsoppgaveDo(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = "nb",
                innsendingsId = "innsending_id",
                innsendingsFristDager = 1,
                vedleggsliste = listOf(
                    EttersendingsoppgaveVedleggDo(
                        tittel = "Vedlegg 1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )
        tilleggsopplysninger.add(
            mapOf(
                "nokkel" to "dokdistBestillingsId",
                "verdi" to "asdsadasdsadasdasd",
            ),
        )
        stubs.mockInnsendingApi()
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(
                tilleggsopplysninger = tilleggsopplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ).copy(
                relevanteDatoer = listOf(
                    DatoType(LocalDateTime.now().toString(), "DATO_JOURNALFOERT"),
                ),
            ),
        )

        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        val request = DistribuerJournalpostRequest()

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<DistribuerJournalpostResponse>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(request, headersMedEnhet),
        )

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            stubs.verifyStub.dokdistFordelingIkkeKalt()
            stubs.verifyStub.opprettEttersendingKalt(0)

            stubs.verifyStub.dokarkivOppdaterIkkeKalt(JOURNALPOST_ID)
            response.body.ettersendingsoppgave?.innsendingsId shouldBe "innsending_id"
        }
    }

    @Test
    fun `skal ikke opprette ettersending hvis journalpost allerede er distribuert og ettersending opprettet`() {
        // given
        val xEnhet = "1234"
        val bestillingId = "TEST_BEST_ID"
        val headersMedEnhet = HttpHeaders()
        headersMedEnhet.add(EnhetFilter.X_ENHET_HEADER, xEnhet)

        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.setJournalfortAvIdent("Z99999")
        tilleggsopplysninger.setDistribusjonBestillt()

        tilleggsopplysninger.addInnsendingsOppgave(
            EttersendingsoppgaveDo(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = "nb",
                innsendingsId = "innsending_id",
                innsendingsFristDager = 1,
                vedleggsliste = listOf(
                    EttersendingsoppgaveVedleggDo(
                        tittel = "Vedlegg 1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )
        tilleggsopplysninger.add(
            mapOf(
                "nokkel" to "dokdistBestillingsId",
                "verdi" to "asdsadasdsadasdasd",
            ),
        )
        stubs.mockInnsendingApi()
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(
                tilleggsopplysninger = tilleggsopplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ).copy(
                relevanteDatoer = listOf(
                    DatoType(LocalDateTime.now().toString(), "DATO_JOURNALFOERT"),
                ),
            ),
        )

        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        val request = DistribuerJournalpostRequest(
            ettersendingsoppgave =
            OpprettEttersendingsppgaveDto(
                tittel = "Tittel",
                skjemaId = "NAV 10-07.17",
                språk = Språk.NB,
                innsendingsFristDager = 27,
                vedleggsliste = listOf(
                    OpprettEttersendingsoppgaveVedleggDto(
                        tittel = "Vedlegg 1",
                        vedleggsnr = "NAV 10-07.17",
                    ),
                ),
            ),
        )

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<DistribuerJournalpostResponse>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(request, headersMedEnhet),
        )

        // then
        assertSoftly {
            response.statusCode shouldBe HttpStatus.OK
            stubs.verifyStub.dokdistFordelingIkkeKalt()
            stubs.verifyStub.opprettEttersendingKalt(0)

            stubs.verifyStub.dokarkivOppdaterIkkeKalt(JOURNALPOST_ID)
            response.body.ettersendingsoppgave?.innsendingsId shouldBe "innsending_id"
        }
    }

    @Test
    fun `skal distribuere journalpost digitalt uten adresse`() {
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
                "verdi" to "asdsadasdsadasdasd",
            ),
        )

        stubs.mockBestmDistribusjonskanal(
            BestemKanalResponse(
                regel = "",
                regelBegrunnelse = "",
                distribusjonskanal = DistribusjonsKanal.DITT_NAV,
            ),
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(
                tilleggsopplysninger = tilleggsopplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ),
            null,
            "ETTER_DIST",
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST",
            "ETTER_DIST2",
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST2",
            "ETTER_DIST3",
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        // when
        val response = httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(null, headersMedEnhet),
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
                        null,
                        null,
                    ),
                ),
            )
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name)
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name)
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
            )
            stubs.verifyStub.dokarkivIkkeOppdaterKalt(
                JOURNALPOST_ID,
                "datoDokument",
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                "{\"tilleggsopplysninger\":[" +
                    "{\"nokkel\":\"dokdistBestillingsId\",\"verdi\":\"asdsadasdsadasdasd\"}," +
                    "{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"},{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}," +
                    "{\"nokkel\":\"origDistDigitalt\",\"verdi\":\"true\"}," +
                    "{\"nokkel\":\"distribuertAvIdent\",\"verdi\":\"aud-localhost\"}],\"dokumenter\":[]}",
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
                "verdi" to "asdsadasdsadasdasd",
            ),
        )

        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysninger),
            null,
            "ETTER_DIST",
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST",
            "ETTER_DIST2",
        )
        stubs.mockSafResponseHentJournalpost(
            opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsopplysningerEtterDist),
            "ETTER_DIST2",
            "ETTER_DIST3",
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(JOURNALPOST_ID)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    JOURNALPOST_ID,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        val distribuerTilAdresse = createDistribuerTilAdresse().copy(
            adresselinje2 = "Adresselinje2",
            adresselinje3 = "Adresselinje3",
        )
        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + JOURNALPOST_ID,
            HttpEntity(request, headersMedEnhet),
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
                        null,
                    ),
                ),
            )
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name)
            stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name)
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                request.adresse!!.adresselinje1,
                request.adresse!!.land,
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                "datoDokument",
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                JOURNALPOST_ID,
                "{\"tilleggsopplysninger\":[" +
                    "{\"nokkel\":\"dokdistBestillingsId\",\"verdi\":\"asdsadasdsadasdasd\"}," +
                    "{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"}," +
                    "{\"nokkel\":\"distAdresse0\",\"verdi\":\"{\\\"adresselinje1\\\":\\\"Adresselinje1\\\",\\\"adresselinje2\\\":\\\"Adresselinje2\\\",\\\"adresselinje3\\\":\\\"Adresselinje3\\\",\\\"la\"}," +
                    "{\"nokkel\":\"distAdresse1\",\"verdi\":\"nd\\\":\\\"NO\\\",\\\"postnummer\\\":\\\"3000\\\",\\\"poststed\\\":\\\"Ingen\\\"}\"}," +
                    "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"},{\"nokkel\":\"distribuertAvIdent\",\"verdi\":\"aud-localhost\"}],\"dokumenter\":[]}",
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
            avsenderMottaker = AvsenderMottaker(navn = "Samhandler Navnesen"),
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
                    Sak("5276661"),
                ),
            ),
        )
        val distribuerTilAdresse = createDistribuerTilAdresse()
            .copy(
                adresselinje2 = "Adresselinje2",
                adresselinje3 = "Adresselinje3",
            )
        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostId,
            HttpEntity(request, headersMedEnhet),

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
                        null,
                    ),
                ),
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}",
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
            avsenderMottaker = AvsenderMottaker(navn = "Samhandler Navnesen"),
        )
        stubs.mockSafResponseHentJournalpost(safresponse)
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    journalpostId,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        val distribuerTilAdresse = createDistribuerTilAdresse()
            .copy(
                adresselinje2 = "Adresselinje2",
                adresselinje3 = "Adresselinje3",
            )
        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val response = httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostId,
            HttpEntity(request, headersMedEnhet),
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
                        null,
                    ),
                ),
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}",
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
                    digitalpostSendt = DigitalpostSendt("test@nav.no"),
                ),
                tilleggsopplysninger = tilleggsOpplysninger,
                relevanteDatoer = listOf(DATO_DOKUMENT),
            ),
            journalpostId,
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
                    epostVarselSendt = EpostVarselSendt("test@nav.no", "tittel", "varslingtekst"),
                ),
                tilleggsopplysninger = tilleggsOpplysninger,
                relevanteDatoer = listOf(DATO_DOKUMENT),
            ),
            journalpostId,
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
    fun `skal ikke markere journalpost distribuert lokalt hvis allerede har tittel`() {
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
        )
        stubs.mockSafResponseHentJournalpost(safresponse)
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setJournalfortAvIdent("Z99999")
        stubs.mockSafResponseHentJournalpost(
            safresponse.copy(
                tilleggsopplysninger = tilleggsOpplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ),
            null,
            "ETTER_DIST",
        )
        stubs.mockSafResponseHentJournalpost(
            safresponse.copy(
                tilleggsopplysninger = tilleggsOpplysninger,
                journalstatus = JournalStatus.EKSPEDERT,
                dokumenter = safresponse.dokumenter.mapIndexed { index, dokument ->
                    if (index == 0) {
                        dokument.copy(tittel = "Tittel (dokumentet er sendt per post med vedlegg)")
                    } else {
                        dokument
                    }
                },
            ),
            "ETTER_DIST",
            null,
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    journalpostId,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        stubs.mockDokarkivOppdaterDistribusjonsInfoRequest(journalpostId)
        val request = DistribuerJournalpostRequest(lokalUtskrift = true)

        // when
        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostId,
            HttpMethod.POST,
            HttpEntity(request, headersMedEnhet),
            JournalpostDto::class.java,
        )

        response.statusCode shouldBe HttpStatus.OK

        assertSoftly {
            stubs.verifyStub.dokdistFordelingIkkeKalt()
            stubs.verifyStub.dokarkivOppdaterDistribusjonsInfoKalt(
                journalpostId,
                "{\"settStatusEkspedert\":true,\"utsendingsKanal\":\"L\"}",
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "{\"tilleggsopplysninger\":[{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"},{\"nokkel\":\"distribuertAvIdent\",\"verdi\":\"aud-localhost\"}],\"dokumenter\":[]}",
            )
            stubs.verifyStub.dokarkivIkkeOppdaterKalt(
                journalpostId,
                "{\"dokumenter\":[{\"dokumentInfoId\":\"$DOKUMENT_1_ID\",\"tittel\":\"Tittel på dokument 1 (dokumentet er sendt per post med vedlegg)\"}]}",
            )
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
            journalstatus = JournalStatus.FERDIGSTILT,
        )
        stubs.mockSafResponseHentJournalpost(safresponse)
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setJournalfortAvIdent("Z99999")
        stubs.mockSafResponseHentJournalpost(
            safresponse.copy(
                tilleggsopplysninger = tilleggsOpplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ),
            null,
            "ETTER_DIST",
        )
        stubs.mockSafResponseHentJournalpost(
            safresponse.copy(
                tilleggsopplysninger = tilleggsOpplysninger,
                journalstatus = JournalStatus.EKSPEDERT,
            ),
            "ETTER_DIST",
            null,
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    journalpostId,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        stubs.mockDokarkivOppdaterDistribusjonsInfoRequest(journalpostId)
        val request = DistribuerJournalpostRequest(lokalUtskrift = true)

        // when
        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostId,
            HttpMethod.POST,
            HttpEntity(request, headersMedEnhet),
            JournalpostDto::class.java,
        )

        response.statusCode shouldBe HttpStatus.OK

        assertSoftly {
            stubs.verifyStub.dokdistFordelingIkkeKalt()
            stubs.verifyStub.dokarkivOppdaterDistribusjonsInfoKalt(
                journalpostId,
                "{\"settStatusEkspedert\":true,\"utsendingsKanal\":\"L\"}",
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "{\"tilleggsopplysninger\":[{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"},{\"nokkel\":\"distribuertAvIdent\",\"verdi\":\"aud-localhost\"}],\"dokumenter\":[]}",
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "{\"dokumenter\":[{\"dokumentInfoId\":\"$DOKUMENT_1_ID\",\"tittel\":\"Tittel på dokument 1 (dokumentet er sendt per post med vedlegg)\"}]}",
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
            tema = "FAR",
        )
        stubs.mockSafResponseHentJournalpost(safresponse)
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setJournalfortAvIdent("Z99999")
        stubs.mockSafResponseHentJournalpost(
            safresponse.copy(
                tilleggsopplysninger = tilleggsOpplysninger,
                relevanteDatoer = listOf(DatoType(LocalDateTime.now().toString(), "DATO_DOKUMENT")),
            ),
            null,
            "ETTER_DIST",
        )
        stubs.mockSafResponseHentJournalpost(
            safresponse.copy(
                tilleggsopplysninger = tilleggsOpplysninger,
                journalstatus = JournalStatus.EKSPEDERT,
            ),
            "ETTER_DIST",
            null,
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockSafResponseTilknyttedeJournalposter(
            listOf(
                TilknyttetJournalpost(
                    journalpostId,
                    JournalStatus.FERDIGSTILT,
                    Sak("5276661"),
                ),
            ),
        )
        stubs.mockDokarkivOppdaterDistribusjonsInfoRequest(journalpostId)
        val request = DistribuerJournalpostRequest(lokalUtskrift = true)

        // when
        val response = httpHeaderTestRestTemplate.exchange(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostId,
            HttpMethod.POST,
            HttpEntity(request, headersMedEnhet),
            JournalpostDto::class.java,
        )

        response.statusCode shouldBe HttpStatus.OK

        assertSoftly {
            stubs.verifyStub.dokdistFordelingIkkeKalt()
            stubs.verifyStub.dokarkivOppdaterDistribusjonsInfoKalt(
                journalpostId,
                "{\"settStatusEkspedert\":true,\"utsendingsKanal\":\"L\"}",
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "{\"tilleggsopplysninger\":[{\"nokkel\":\"journalfortAvIdent\",\"verdi\":\"Z99999\"},{\"nokkel\":\"distribuertAvIdent\",\"verdi\":\"aud-localhost\"}],\"dokumenter\":[]}",
            )
            stubs.verifyStub.dokarkivIkkeOppdaterKalt(
                journalpostId,
                "{\"dokumenter\":[{\"dokumentInfoId\":\"$DOKUMENT_1_ID\",\"tittel\":\"Tittel på dokument 1 (dokumentet er sendt per post med vedlegg)\"}]}",
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
        val distribuerTilAdresse = createDistribuerTilAdresse().copy(
            adresselinje2 = "Adresselinje2",
            adresselinje3 = "Adresselinje3",
        )

        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val oppdaterJournalpostResponseEntity =
            httpHeaderTestRestTemplate.postForEntity<DistribuerJournalpostResponse>(
                initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson,
                HttpEntity(request, headersMedEnhet),
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
            { stubs.verifyStub.dokdistFordelingIkkeKalt() },
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
            adresselinje1 = "Ramsegata 1",
            adresselinje2 = "Bakredør",
            adresselinje3 = null,
            postnummer = "3939",
            poststed = "OSLO",
            land = Landkode2("NO"),
            land3 = Landkode3("NOR"),
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
                    Sak("5276661"),
                ),
            ),
        )
        stubs.mockPersonAdresseResponse(postadresse)

        // when
        val oppdaterJournalpostResponseEntity =
            httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
                initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson + "?batchId=" + batchId,
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
                            "BI01A01",
                            null,
                            DistribuerTilAdresse(
                                postadresse.adresselinje1,
                                postadresse.adresselinje2,
                                postadresse.adresselinje3,
                                postadresse.land.verdi,
                                postadresse.postnummer,
                                postadresse.poststed,
                            ),
                            batchId,
                        ),
                    ),
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
                    "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}",
                )
            },
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
            HttpStatus.OK,
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockDokarkivOppdaterRequest(journalpostIdFraJson)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        val distribuerTilAdresse = createDistribuerTilAdresse()
            .copy(
                adresselinje2 = "Adresselinje2",
                adresselinje3 = "Adresselinje3",
            )
        val request = DistribuerJournalpostRequest(adresse = distribuerTilAdresse)

        // when
        val oppdaterJournalpostResponseEntity =
            httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
                initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson,
                HttpEntity(request, headersMedEnhet),
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
                            null,
                        ),
                    ),
                )
            },
            { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsType.VEDTAK.name) },
            { stubs.verifyStub.dokdistFordelingKalt(DistribusjonsTidspunkt.KJERNETID.name) },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostIdFraJson,
                    request.adresse!!.adresselinje1,
                    request.adresse!!.land,
                )
            },
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostIdFraJson,
                    "{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}",
                )
            },
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
            HttpStatus.OK,
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        stubs.mockSafResponseTilknyttedeJournalposter(HttpStatus.OK)
        val request = DistribuerJournalpostRequest(adresse = createDistribuerTilAdresse())

        // when
        val oppdaterJournalpostResponseEntity =
            httpHeaderTestRestTemplate.postForEntity<JournalpostDto>(
                initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson,
                HttpEntity(request, headersMedEnhet),
            )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            {
                Assertions.assertThat(oppdaterJournalpostResponseEntity)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.BAD_REQUEST)
            },
            { stubs.verifyStub.dokdistFordelingIkkeKalt() },
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
            },
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
            HttpStatus.OK,
        )
        stubs.mockDokdistFordelingRequest(HttpStatus.OK, bestillingId)
        // when
        val response = httpHeaderTestRestTemplate.getForEntity<JournalpostDto>(
            initUrl() + "/journal/distribuer/JOARK-" + journalpostIdFraJson + "/enabled",
            HttpEntity<Any>(headersMedEnhet),
        )

        // then
        org.junit.jupiter.api.Assertions.assertAll(
            {
                Assertions.assertThat(response)
                    .extracting { it.statusCode }
                    .`as`("statusCode")
                    .isEqualTo(HttpStatus.NOT_ACCEPTABLE)
            },
        )
    }
}
