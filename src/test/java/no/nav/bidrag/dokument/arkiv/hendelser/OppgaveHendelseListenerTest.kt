package no.nav.bidrag.dokument.arkiv.hendelser

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.assertions.assertSoftly
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivTest
import no.nav.bidrag.dokument.arkiv.dto.DatoType
import no.nav.bidrag.dokument.arkiv.dto.FysiskpostSendt
import no.nav.bidrag.dokument.arkiv.dto.OppgaveData
import no.nav.bidrag.dokument.arkiv.dto.ReturDetaljerLogDO
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.dto.UtsendingsInfo
import no.nav.bidrag.dokument.arkiv.kafka.HendelseListener
import no.nav.bidrag.dokument.arkiv.kafka.dto.OppgaveKafkaHendelse
import no.nav.bidrag.dokument.arkiv.model.OppgaveStatus
import no.nav.bidrag.dokument.arkiv.model.Oppgavestatuskategori
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_AKTOER_ID
import no.nav.bidrag.dokument.arkiv.stubs.Stubs
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.dokument.arkiv.utils.DateUtils
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@ActiveProfiles(
    value = [BidragDokumentArkivConfig.PROFILE_KAFKA_TEST, BidragDokumentArkivConfig.PROFILE_TEST, BidragDokumentArkivTest.PROFILE_INTEGRATION],
)
@SpringBootTest(classes = [BidragDokumentArkivTest::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
class OppgaveHendelseListenerTest {
    @MockBean
    lateinit var kafkaTemplateMock: KafkaTemplate<String, String>

    @Autowired
    lateinit var hendelseListener: HendelseListener

    @Autowired
    lateinit var objectMapper: ObjectMapper

    val stubs: Stubs = Stubs()

    @AfterEach
    fun cleanupMocks() {
        WireMock.reset()
        WireMock.resetToDefault()
        Mockito.reset(kafkaTemplateMock)
    }

    @Test
    fun `skal opprette returlogg og oppdatere saksreferanse paa oppgave`() {
        stubs.mockSts()
        stubs.mockBidragOrganisasjonSaksbehandler()
        stubs.mockOppdaterOppgave(HttpStatus.OK)
        val journalpostId = 201028011L
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02"),
                true,
            ),
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-10-02"),
                true,
            ),
        )
        val safResponse = opprettUtgaendeSafResponse(
            journalpostId = journalpostId.toString(),
            tilleggsopplysninger = tilleggsOpplysninger,
            relevanteDatoer = listOf(
                DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT"),
            ),
        ).copy(utsendingsinfo = UtsendingsInfo(fysiskpostSendt = FysiskpostSendt("Adresselinje1\n3033 Drammen\nNO")))
        safResponse.antallRetur = 1

        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockDokarkivOppdaterRequest(journalpostId)

        val oppgaveData = createOppgaveData(journalpostId = journalpostId.toString())
        stubs.mockHentOppgave(oppgaveData.id, oppgaveData)

        val consumerRecord =
            ConsumerRecord(
                "test",
                0,
                0L,
                "key",
                objectMapper.writeValueAsString(oppgaveData.toHendelse()),
            )
        hendelseListener.lesOppgaveOpprettetHendelse(consumerRecord)

        Assertions.assertAll(
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostId,
                    "\"tilleggsopplysninger\":" +
                        "[{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}," +
                        "{\"nokkel\":\"Lretur0_2020-01-02\",\"verdi\":\"En god begrunnelse for hvorfor dokument kom i retur\"}," +
                        "{\"nokkel\":\"Lretur0_2020-10-02\",\"verdi\":\"En annen god begrunnelse for hvorfor dokument kom i retur\"}," +
                        "{\"nokkel\":\"retur0_${DateUtils.formatDate(LocalDate.now())}\",\"verdi\":\"Returpost\"}]",
                )
            },
            { stubs.verifyStub.oppgaveOppdaterKalt(1, safResponse.hentSaksnummer()) },
        )
    }

    @Test
    fun `skal oppdatere saksreferanse oppgave med retry`() {
        stubs.mockSts()
        stubs.mockBidragOrganisasjonSaksbehandler()
        stubs.mockOppdaterOppgave(HttpStatus.CONFLICT, null, "correct")
        stubs.mockOppdaterOppgave(HttpStatus.OK, "correct", null)
        val journalpostId = 201028011L
        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.setDistribusjonBestillt()
        val safResponse = opprettUtgaendeSafResponse(
            journalpostId = journalpostId.toString(),
            tilleggsopplysninger = tilleggsopplysninger,
            relevanteDatoer = listOf(
                DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT"),
            ),
        )
        safResponse.antallRetur = 1

        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockDokarkivOppdaterRequest(journalpostId)

        val oppgaveData = createOppgaveData(versjon = 20, journalpostId = journalpostId.toString())
        stubs.mockHentOppgave(oppgaveData.id, oppgaveData, null, "fetch1")
        stubs.mockHentOppgave(oppgaveData.id, oppgaveData.copy(versjon = 22), "fetch1", null)

        val consumerRecord =
            ConsumerRecord(
                "test",
                0,
                0L,
                "key",
                objectMapper.writeValueAsString(oppgaveData.toHendelse()),
            )
        hendelseListener.lesOppgaveOpprettetHendelse(consumerRecord)

        assertSoftly {
            stubs.verifyStub.oppgaveOppdaterKalt(2, safResponse.hentSaksnummer())
            stubs.verifyStub.oppgaveOppdaterKalt(1, safResponse.hentSaksnummer(), "\"versjon\":22")
        }
    }

    @Test
    fun skalIkkeOppdatereSakHvisSamme() {
        stubs.mockSts()
        stubs.mockBidragOrganisasjonSaksbehandler()
        stubs.mockOppdaterOppgave(HttpStatus.OK)
        val journalpostId = 201028011L
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02"),
                true,
            ),
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-10-02"),
                true,
            ),
        )
        val safResponse = opprettUtgaendeSafResponse(
            journalpostId = journalpostId.toString(),
            tilleggsopplysninger = tilleggsOpplysninger,
            relevanteDatoer = listOf(
                DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT"),
            ),
        ).copy(utsendingsinfo = UtsendingsInfo(fysiskpostSendt = FysiskpostSendt("Adresselinje1\n3033 Drammen\nNO")))

        safResponse.antallRetur = 1
        safResponse.sak = Sak("5276661")

        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        val oppgaveData =
            createOppgaveData(journalpostId = journalpostId.toString(), saksref = "5276661")
        stubs.mockHentOppgave(oppgaveData.id, oppgaveData)

        val consumerRecord =
            ConsumerRecord(
                "test",
                0,
                0L,
                "key",
                objectMapper.writeValueAsString(oppgaveData.toHendelse()),
            )
        hendelseListener.lesOppgaveOpprettetHendelse(consumerRecord)

        Assertions.assertAll(
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostId,
                    "\"tilleggsopplysninger\":" +
                        "[{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}," +
                        "{\"nokkel\":\"Lretur0_2020-01-02\",\"verdi\":\"En god begrunnelse for hvorfor dokument kom i retur\"}," +
                        "{\"nokkel\":\"Lretur0_2020-10-02\",\"verdi\":\"En annen god begrunnelse for hvorfor dokument kom i retur\"}," +
                        "{\"nokkel\":\"retur0_${DateUtils.formatDate(LocalDate.now())}\",\"verdi\":\"Returpost\"}]",
                )
            },
            { stubs.verifyStub.oppgaveOpprettIkkeKalt() },
        )
    }

    @Test
    fun shouldRetryWhenJournalpostHasNotReturStatus() {
        stubs.mockSts()
        stubs.mockBidragOrganisasjonSaksbehandler()
        val journalpostId = 201028011L
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        val safResponse = opprettUtgaendeSafResponse(
            journalpostId = journalpostId.toString(),
            tilleggsopplysninger = tilleggsOpplysninger,
            relevanteDatoer = listOf(
                DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT"),
            ),
        ).copy(utsendingsinfo = UtsendingsInfo(fysiskpostSendt = FysiskpostSendt("Adresselinje1\n3033 Drammen\nNO")))

        safResponse.antallRetur = 1

        stubs.mockSafResponseHentJournalpost(opprettUtgaendeSafResponse(), null, "NO_RETUR")
        stubs.mockSafResponseHentJournalpost(safResponse, "NO_RETUR", "NO_RETUR2")
        stubs.mockDokarkivOppdaterRequest(journalpostId)

        val oppgaveData = createOppgaveData(journalpostId = journalpostId.toString())
        stubs.mockHentOppgave(oppgaveData.id, oppgaveData)

        val consumerRecord =
            ConsumerRecord(
                "test",
                0,
                0L,
                "key",
                objectMapper.writeValueAsString(oppgaveData.toHendelse()),
            )
        hendelseListener.lesOppgaveOpprettetHendelse(consumerRecord)

        Assertions.assertAll(
            {
                stubs.verifyStub.harSafKallEtterHentJournalpost(2)
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostId,
                    "\"tilleggsopplysninger\":" +
                        "[{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}," +
                        "{\"nokkel\":\"retur0_${DateUtils.formatDate(LocalDate.now())}\",\"verdi\":\"Returpost\"}]",
                )
            },
        )
    }

    @Test
    fun shouldNotAddReturLoggWhenAlreadyExists() {
        stubs.mockSts()
        stubs.mockBidragOrganisasjonSaksbehandler()
        val journalpostId = 201028011L
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02"),
                true,
            ),
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2021-08-20"),
                false,
            ),
        )
        val safResponse = opprettUtgaendeSafResponse(
            journalpostId = journalpostId.toString(),
            tilleggsopplysninger = tilleggsOpplysninger,
            relevanteDatoer = listOf(
                DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT"),
            ),
        )
        safResponse.antallRetur = 1

        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockDokarkivOppdaterRequest(journalpostId)

        val oppgaveData = createOppgaveData(journalpostId = journalpostId.toString())
        stubs.mockHentOppgave(oppgaveData.id, oppgaveData)

        val consumerRecord =
            ConsumerRecord(
                "test",
                0,
                0L,
                "key",
                objectMapper.writeValueAsString(oppgaveData.toHendelse()),
            )
        hendelseListener.lesOppgaveOpprettetHendelse(consumerRecord)

        Assertions.assertAll(
            {
                stubs.verifyStub.dokarkivOppdaterIkkeKalt(journalpostId)
            },
        )
    }

    @Test
    fun `skal oppdatere oppgave og returlogg med kommentar hvis retur kommer fra navno`() {
        stubs.mockSts()
        stubs.mockBidragOrganisasjonSaksbehandler()
        stubs.mockOppdaterOppgave(HttpStatus.CONFLICT, null, "correct")
        stubs.mockOppdaterOppgave(HttpStatus.OK, "correct", null)
        val journalpostId = 201028011L
        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.setDistribusjonBestillt()
        tilleggsopplysninger.setOriginalDistribuertDigitalt()
        val safResponse = opprettUtgaendeSafResponse(
            journalpostId = journalpostId.toString(),
            tilleggsopplysninger = tilleggsopplysninger,
            relevanteDatoer = listOf(
                DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT"),
            ),
        )
        safResponse.antallRetur = 1

        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockDokarkivOppdaterRequest(journalpostId)

        val oppgaveData = createOppgaveData(versjon = 20, journalpostId = journalpostId.toString())
        stubs.mockHentOppgave(oppgaveData.id, oppgaveData, null, "fetch1")
        stubs.mockHentOppgave(oppgaveData.id, oppgaveData.copy(versjon = 22), "fetch1", null)

        val consumerRecord =
            ConsumerRecord(
                "test",
                0,
                0L,
                "key",
                objectMapper.writeValueAsString(oppgaveData.toHendelse()),
            )
        hendelseListener.lesOppgaveOpprettetHendelse(consumerRecord)

        assertSoftly {
            stubs.verifyStub.oppgaveOppdaterKalt(2, safResponse.hentSaksnummer())
            stubs.verifyStub.oppgaveOppdaterKalt(
                1,
                safResponse.hentSaksnummer(),
                "\"versjon\":22",
                "Mottaker har ikke åpnet forsendelsen via www.nav.no innen 40 timer. Ingen postadresse er registrert. Vurder om mottaker har adresse forsendelsen kan sendes til",
            )
            stubs.verifyStub.dokarkivOppdaterKalt(
                journalpostId,
                "\"tilleggsopplysninger\":" +
                    "[{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"},{\"nokkel\":\"origDistDigitalt\",\"verdi\":\"true\"}," +
                    "{\"nokkel\":\"retur0_${DateUtils.formatDate(LocalDate.now())}\",\"verdi\":\"Distribusjon feilet, mottaker mangler postadresse\"}]",
            )
        }
    }

    @Test
    fun shouldUnlockReturLogWhenSameDateExists() {
        stubs.mockSts()
        stubs.mockBidragOrganisasjonSaksbehandler()
        val journalpostId = 201028011L
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02"),
                true,
            ),
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.now(),
                true,
            ),
        )
        val safResponse = opprettUtgaendeSafResponse(
            journalpostId = journalpostId.toString(),
            tilleggsopplysninger = tilleggsOpplysninger,
            relevanteDatoer = listOf(
                DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT"),
            ),
        )
        safResponse.antallRetur = 1

        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockDokarkivOppdaterRequest(journalpostId)

        val oppgaveData = createOppgaveData(journalpostId = journalpostId.toString())
        stubs.mockHentOppgave(oppgaveData.id, oppgaveData)
        val consumerRecord =
            ConsumerRecord(
                "test",
                0,
                0L,
                "key",
                objectMapper.writeValueAsString(oppgaveData.toHendelse()),
            )
        hendelseListener.lesOppgaveOpprettetHendelse(consumerRecord)

        Assertions.assertAll(
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostId,
                    "\"tilleggsopplysninger\":" +
                        "[{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}," +
                        "{\"nokkel\":\"Lretur0_2020-01-02\",\"verdi\":\"En god begrunnelse for hvorfor dokument kom i retur\"}," +
                        "{\"nokkel\":\"retur0_${DateUtils.formatDate(LocalDate.now())}\",\"verdi\":\"En annen god begrunnelse for hvorfor dokument kom i retur\"}",
                )
            },
        )
    }

    fun createOppgaveData(
        id: Long = 1,
        versjon: Int = 1,
        journalpostId: String? = "123213",
        tildeltEnhetsnr: String? = "4806",
        statuskategori: Oppgavestatuskategori = Oppgavestatuskategori.AAPEN,
        status: OppgaveStatus? = null,
        oppgavetype: String = "RETUR",
        tema: String = "BID",
        aktoerId: String = BRUKER_AKTOER_ID,
        beskrivelse: String? = null,
        tilordnetRessurs: String? = null,
        saksref: String = "123213123",
    ) = OppgaveData(
        id = id,
        versjon = versjon,
        journalpostId = journalpostId,
        tildeltEnhetsnr = "4806",
        status = status ?: when (statuskategori) {
            Oppgavestatuskategori.AAPEN -> OppgaveStatus.OPPRETTET
            Oppgavestatuskategori.AVSLUTTET -> OppgaveStatus.FERDIGSTILT
        },
        oppgavetype = oppgavetype,
        tema = tema,
        tilordnetRessurs = tilordnetRessurs,
        beskrivelse = beskrivelse,
        aktoerId = aktoerId,
        saksreferanse = saksref,
    )

    fun OppgaveData.toHendelse(type: OppgaveKafkaHendelse.Hendelse.Hendelsestype? = null) = OppgaveKafkaHendelse(
        hendelse = OppgaveKafkaHendelse.Hendelse(
            type ?: when (status) {
                OppgaveStatus.FERDIGSTILT -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_FERDIGSTILT
                OppgaveStatus.OPPRETTET -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_OPPRETTET
                OppgaveStatus.UNDER_BEHANDLING -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_ENDRET
                OppgaveStatus.FEILREGISTRERT -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_FEILREGISTRERT
                else -> OppgaveKafkaHendelse.Hendelse.Hendelsestype.OPPGAVE_ENDRET
            },
            LocalDateTime.now(),
        ),
        utfortAv = OppgaveKafkaHendelse.UtfortAv(tilordnetRessurs, tildeltEnhetsnr),
        oppgave = OppgaveKafkaHendelse.Oppgave(
            id,
            1,
            kategorisering = OppgaveKafkaHendelse.Kategorisering(
                tema ?: "BID",
                oppgavetype = oppgavetype ?: "JFR",
            ),
            bruker = OppgaveKafkaHendelse.Bruker(
                aktoerId,
                OppgaveKafkaHendelse.Bruker.IdentType.FOLKEREGISTERIDENT,
            ),
        ),
    )
}
