package no.nav.bidrag.dokument.arkiv.hendelser

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivTest
import no.nav.bidrag.dokument.arkiv.dto.Bruker
import no.nav.bidrag.dokument.arkiv.dto.BrukerType
import no.nav.bidrag.dokument.arkiv.dto.Dokument
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.kafka.HendelseListener
import no.nav.bidrag.dokument.arkiv.model.JoarkHendelseType
import no.nav.bidrag.dokument.arkiv.stubs.AVSENDER_ID
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_AKTOER_ID
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_ENHET
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_FNR
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_TYPE_AKTOERID
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_ID
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_TITTEL
import no.nav.bidrag.dokument.arkiv.stubs.Stubs
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.dto.HendelseType
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import no.nav.bidrag.domain.ident.AktørId
import no.nav.bidrag.domain.ident.PersonIdent
import no.nav.bidrag.transport.person.PersonDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(value = [BidragDokumentArkivConfig.PROFILE_KAFKA_TEST, BidragDokumentArkivConfig.PROFILE_TEST, BidragDokumentArkivTest.PROFILE_INTEGRATION])
@SpringBootTest(classes = [BidragDokumentArkivTest::class])
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
class JoarkHendelseTest {
    @MockBean
    lateinit var kafkaTemplateMock: KafkaTemplate<String, String>

    @Autowired
    lateinit var hendelseListener: HendelseListener

    @Autowired
    lateinit var objectMapper: ObjectMapper

    val stubs: Stubs = Stubs()

    @Value("\${TOPIC_JOURNALPOST}")
    lateinit var topicJournalpost: String

    @AfterEach
    fun cleanupMocks() {
        Mockito.reset(kafkaTemplateMock)
        WireMock.reset()
        WireMock.resetToDefault()
    }

    @Test
    fun `skal behandle og publisere journalpost hendelse for mottat journalpost`() {
        val journalpostId = 123213L
        val expectedJoarkJournalpostId = "JOARK-$journalpostId"
        stubs.mockSts()
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                journalstatus = JournalStatus.MOTTATT,
                journalforendeEnhet = null,
                bruker = Bruker(BRUKER_AKTOER_ID, BRUKER_TYPE_AKTOERID),
                sak = null
            )
        )

        val record = createHendelseRecord(journalpostId)

        hendelseListener.listenJournalforingHendelse(record)
        val jsonCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(kafkaTemplateMock).send(ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(expectedJoarkJournalpostId), jsonCaptor.capture())
        val journalpostHendelse = objectMapper.readValue(jsonCaptor.value, JournalpostHendelse::class.java)

        assertAll(
            "JournalpostHendelse",
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalpostId).isEqualTo(expectedJoarkJournalpostId) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::enhet).isNull() },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::fnr).isEqualTo(AVSENDER_ID) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::dokumentDato).isEqualTo(no.nav.bidrag.dokument.arkiv.stubs.DATO_DOKUMENT.somDato()) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalfortDato).isNull() },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::hendelseType).isEqualTo(HendelseType.ENDRING) },
            { assertThat(journalpostHendelse.sporing?.brukerident).isNull() },
            { assertThat(journalpostHendelse.sporing?.saksbehandlersNavn).isEqualTo("bidrag-dokument-arkiv") },
            { assertThat(journalpostHendelse.sporing?.enhetsnummer).isEqualTo("9999") },
            { assertThat(journalpostHendelse.sakstilknytninger).isEmpty() },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::aktorId).isEqualTo(BRUKER_AKTOER_ID) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalstatus).isEqualTo("M") },
            { stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter() }
        )
    }

    @Test
    fun `skal behandle og publisere journalpost hendelse for journalfort journalpost`() {
        val journalpostId = 123213L
        val tilknyttetJournalpostId = 12344213L
        val jfEnhet = "4806"
        val sak1 = "12321323"
        val sak2 = "2143444"
        val journalfortAvIdent = "Z123123"
        val journalfortAvNavn = "Saksbehandler navn"
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setJournalfortAvIdent(journalfortAvIdent)
        val expectedJoarkJournalpostId = "JOARK-$journalpostId"
        stubs.mockSts()
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                journalstatus = JournalStatus.JOURNALFOERT,
                journalforendeEnhet = jfEnhet,
                bruker = Bruker(BRUKER_AKTOER_ID, BRUKER_TYPE_AKTOERID),
                sak = Sak(sak1),
                relevanteDatoer = listOf(no.nav.bidrag.dokument.arkiv.stubs.DATO_DOKUMENT, no.nav.bidrag.dokument.arkiv.stubs.DATO_JOURNALFORT),
                tilleggsopplysninger = tilleggsOpplysninger,
                journalfortAvNavn = journalfortAvNavn
            )
        )
        stubs.mockSafResponseTilknyttedeJournalposter(listOf(TilknyttetJournalpost(tilknyttetJournalpostId, journalstatus = JournalStatus.JOURNALFOERT, sak = Sak(sak2))))

        val record = createHendelseRecord(journalpostId)
        record.journalpostStatus = "JOURNALFOERT"
        record.hendelsesType = JoarkHendelseType.ENDELIG_JOURNALFORT.hendelsesType

        hendelseListener.listenJournalforingHendelse(record)
        val jsonCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(kafkaTemplateMock).send(ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(expectedJoarkJournalpostId), jsonCaptor.capture())
        val journalpostHendelse = objectMapper.readValue(jsonCaptor.value, JournalpostHendelse::class.java)

        assertAll(
            "JournalpostHendelse",
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalpostId).isEqualTo(expectedJoarkJournalpostId) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::enhet).isEqualTo(jfEnhet) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::fnr).isEqualTo(AVSENDER_ID) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::hendelseType).isEqualTo(HendelseType.JOURNALFORING) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::dokumentDato).isEqualTo(no.nav.bidrag.dokument.arkiv.stubs.DATO_DOKUMENT.somDato()) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalfortDato).isEqualTo(no.nav.bidrag.dokument.arkiv.stubs.DATO_JOURNALFORT.somDato()) },
            { assertThat(journalpostHendelse.sporing?.brukerident).isEqualTo(journalfortAvIdent) },
            { assertThat(journalpostHendelse.sporing?.saksbehandlersNavn).isEqualTo(journalfortAvNavn) },
            { assertThat(journalpostHendelse.sporing?.enhetsnummer).isEqualTo(jfEnhet) },
            { assertThat(journalpostHendelse.sakstilknytninger).isNotEmpty() },
            { assertThat(journalpostHendelse.sakstilknytninger).contains(sak1, sak2) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::aktorId).isEqualTo(BRUKER_AKTOER_ID) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalstatus).isEqualTo("J") },
            { stubs.verifyStub.harEnSafKallEtterTilknyttedeJournalposter() }
        )
    }

    @Test
    fun `skal publisere journalposthendelse med fnr fra avsender hvis journalpost ikke har bruker`() {
        val journalpostId = 123213L
        val expectedJoarkJournalpostId = "JOARK-$journalpostId"
        val personEnhet = "4844"
        stubs.mockSts()
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                bruker = null,
                journalstatus = JournalStatus.MOTTATT,
                journalforendeEnhet = BRUKER_ENHET
            )
        )
        stubs.mockSafResponseTilknyttedeJournalposter(listOf())
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockBidragOrganisasjonSaksbehandler()
        stubs.mockOrganisasjonGeografiskTilknytning(personEnhet)

        val record = createHendelseRecord(journalpostId)

        hendelseListener.listenJournalforingHendelse(record)
        val jsonCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(kafkaTemplateMock).send(ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(expectedJoarkJournalpostId), jsonCaptor.capture())
        val journalpostHendelse = objectMapper.readValue(jsonCaptor.value, JournalpostHendelse::class.java)

        assertAll(
            "JournalpostHendelse",
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalpostId).isEqualTo(expectedJoarkJournalpostId) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::enhet).isEqualTo(BRUKER_ENHET) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::aktorId).isNull() },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::fnr).isEqualTo(AVSENDER_ID) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalstatus).isEqualTo("M") }
        )
    }

    @Test
    fun `skal publisere journalposthendelse med fnr fra bruker`() {
        val journalpostId = 123213L
        val expectedJoarkJournalpostId = "JOARK-$journalpostId"
        val personEnhet = "4844"
        stubs.mockSts()
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                bruker = Bruker(BRUKER_FNR, BrukerType.FNR.name),
                journalstatus = JournalStatus.MOTTATT,
                journalforendeEnhet = BRUKER_ENHET
            )
        )
        stubs.mockSafResponseTilknyttedeJournalposter(listOf())
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockBidragOrganisasjonSaksbehandler()
        stubs.mockOrganisasjonGeografiskTilknytning(personEnhet)

        val record = createHendelseRecord(journalpostId)

        hendelseListener.listenJournalforingHendelse(record)
        val jsonCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(kafkaTemplateMock).send(ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(expectedJoarkJournalpostId), jsonCaptor.capture())
        val journalpostHendelse = objectMapper.readValue(jsonCaptor.value, JournalpostHendelse::class.java)

        assertAll(
            "JournalpostHendelse",
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalpostId).isEqualTo(expectedJoarkJournalpostId) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::enhet).isEqualTo(BRUKER_ENHET) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::aktorId).isNull() },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::fnr).isEqualTo(BRUKER_FNR) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalstatus).isEqualTo("M") },
            { stubs.verifyStub.bidragPersonIkkeKalt() }
        )
    }

    @Test
    fun `skal publisere journalpost hendelse med enhet`() {
        val journalpostId = 123213L
        val expectedJoarkJournalpostId = "JOARK-$journalpostId"
        stubs.mockSts()
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                journalstatus = JournalStatus.MOTTATT,
                journalforendeEnhet = BRUKER_ENHET
            )
        )
        stubs.mockSafResponseTilknyttedeJournalposter(listOf())
        stubs.mockBidragOrganisasjonSaksbehandler()
        stubs.mockPersonResponse(PersonDto(PersonIdent("123"), aktørId = AktørId("12321")), HttpStatus.OK)

        val record = createHendelseRecord(journalpostId)

        hendelseListener.listenJournalforingHendelse(record)
        val jsonCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(kafkaTemplateMock).send(ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(expectedJoarkJournalpostId), jsonCaptor.capture())
        val journalpostHendelse = objectMapper.readValue(jsonCaptor.value, JournalpostHendelse::class.java)

        assertAll(
            "JournalpostHendelse",
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalpostId).isEqualTo(expectedJoarkJournalpostId) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::enhet).isEqualTo(BRUKER_ENHET) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::aktorId).isEqualTo(BRUKER_AKTOER_ID) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalstatus).isEqualTo("M") },
            { stubs.verifyStub.dokarkivOppdaterIkkeKalt(journalpostId) }
        )
    }

    @Test
    fun `skal ikke behandle hendelse nar det ikke omhandler tema BID`() {
        val journalpostId = 123213L

        val record = createHendelseRecord(journalpostId)
        record.temaNytt = "AAP"
        record.temaGammelt = "AAP"

        hendelseListener.listenJournalforingHendelse(record)

        verify(kafkaTemplateMock, Mockito.never()).send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    @Test
    fun `skal ikke behandle hendelse nar kanal er NAV_NO_CHAT`() {
        val journalpostId = 123213L

        val record = createHendelseRecord(journalpostId)
        record.mottaksKanal = JournalpostKanal.NAV_NO_CHAT.name

        hendelseListener.listenJournalforingHendelse(record)

        verify(kafkaTemplateMock, Mockito.never()).send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    @Test
    fun `skal ikke behandle hendelse nar journalpost tilhorer NKS`() {
        val journalpostId = 123213L
        stubs.mockSts()
        stubs.mockPersonResponse(PersonDto(PersonIdent("123"), aktørId = AktørId("12321")), HttpStatus.OK)
        stubs.mockSafResponseTilknyttedeJournalposter(listOf(TilknyttetJournalpost(123123L, JournalStatus.FERDIGSTILT, Sak("5276661"))))
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                journalstatus = JournalStatus.MOTTATT,
                journalforendeEnhet = BRUKER_ENHET,
                dokumenter = listOf(
                    Dokument(
                        dokumentInfoId = DOKUMENT_1_ID,
                        brevkode = "CRM_MELDINGSKJEDE",
                        tittel = DOKUMENT_1_TITTEL
                    )
                )
            )
        )

        val record = createHendelseRecord(journalpostId)

        hendelseListener.listenJournalforingHendelse(record)

        verify(kafkaTemplateMock, Mockito.never()).send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }
}
