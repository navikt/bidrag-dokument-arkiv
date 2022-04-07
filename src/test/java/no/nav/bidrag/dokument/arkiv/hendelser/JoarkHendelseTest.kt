package no.nav.bidrag.dokument.arkiv.hendelser

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal
import no.nav.bidrag.dokument.arkiv.dto.Bruker
import no.nav.bidrag.dokument.arkiv.dto.Dokument
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.MottaksKanal
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse
import no.nav.bidrag.dokument.arkiv.kafka.HendelseListener
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_AKTOER_ID
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_ENHET
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_FNR
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_ID
import no.nav.bidrag.dokument.arkiv.stubs.DOKUMENT_1_TITTEL
import no.nav.bidrag.dokument.arkiv.stubs.Stubs
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.dto.JournalpostHendelse
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

@ActiveProfiles(value = [BidragDokumentArkivConfig.PROFILE_KAFKA_TEST, BidragDokumentArkivConfig.PROFILE_TEST, BidragDokumentArkivLocal.PROFILE_INTEGRATION])
@SpringBootTest(classes = [BidragDokumentArkivLocal::class])
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
class JoarkHendelseTest {
    @MockBean
    lateinit var kafkaTemplateMock: KafkaTemplate<String, String>

    @Autowired
    lateinit var hendelseListener: HendelseListener

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var stubs: Stubs

    @Value("\${TOPIC_JOURNALPOST}")
    lateinit var topicJournalpost: String


    @AfterEach
    fun cleanupMocks(){
        Mockito.reset(kafkaTemplateMock)
    }

    @Test
    fun `skal oppdatere journalforende enhet hvis det ikke stemmer med person enhet`() {
        val journalpostId = 123213L
        val expectedJoarkJournalpostId = "JOARK-$journalpostId"
        val personEnhet = "4844"
        stubs.mockSts()
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                journalstatus = JournalStatus.MOTTATT,
                journalforendeEnhet = BRUKER_ENHET
            )
        )
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockBidragOrganisasjonSaksbehandler()
        stubs.mockOrganisasjonGeografiskTilknytning(personEnhet)

        val record = createHendelseRecord(journalpostId)

        hendelseListener.listen(record)
        val jsonCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(kafkaTemplateMock).send(ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(expectedJoarkJournalpostId), jsonCaptor.capture())
        val journalpostHendelse = objectMapper.readValue(jsonCaptor.value, JournalpostHendelse::class.java)

        assertAll("JournalpostHendelse",
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalpostId).isEqualTo(expectedJoarkJournalpostId) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::enhet).isEqualTo(personEnhet) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::aktorId).isEqualTo(BRUKER_AKTOER_ID) },
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::journalstatus).isEqualTo("M") },
            { stubs.verifyStub.dokarkivOppdaterKalt(journalpostId, personEnhet) }
        )
    }

    @Test
    fun `skal sende aktorid hvis SAF returnerer personnummer`() {
        val journalpostId = 123213L
        val expectedJoarkJournalpostId = "JOARK-$journalpostId"
        stubs.mockSts()
        stubs.mockSafResponseHentJournalpost(
            opprettSafResponse(
                journalpostId = journalpostId.toString(),
                journalstatus = JournalStatus.MOTTATT,
                journalforendeEnhet = BRUKER_ENHET,
                bruker = Bruker(BRUKER_FNR, "FNR")
            )
        )
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockPersonResponse(PersonResponse(BRUKER_FNR, BRUKER_AKTOER_ID), HttpStatus.OK)
        stubs.mockBidragOrganisasjonSaksbehandler()
        stubs.mockOrganisasjonGeografiskTilknytning(BRUKER_ENHET)

        val record = createHendelseRecord(journalpostId)

        hendelseListener.listen(record)
        val jsonCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(kafkaTemplateMock).send(ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(expectedJoarkJournalpostId), jsonCaptor.capture())
        val journalpostHendelse = objectMapper.readValue(jsonCaptor.value, JournalpostHendelse::class.java)

        assertAll("JournalpostHendelse",
            { assertThat(journalpostHendelse).extracting(JournalpostHendelse::aktorId).isEqualTo(BRUKER_AKTOER_ID) },
            { stubs.verifyStub.bidragPersonKalt() },
        )
    }

    @Test
    fun `skal ikke behandle hendelse nar det ikke omhandler tema BID`() {
        val journalpostId = 123213L

        val record = createHendelseRecord(journalpostId)
        record.temaNytt = "AAP"
        record.temaGammelt = "AAP"

        hendelseListener.listen(record)

        verify(kafkaTemplateMock, Mockito.never()).send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    @Test
    fun `skal ikke behandle hendelse nar kanal er NAV_NO_CHAT`() {
        val journalpostId = 123213L

        val record = createHendelseRecord(journalpostId)
        record.mottaksKanal = MottaksKanal.NAV_NO_CHAT.name

        hendelseListener.listen(record)

        verify(kafkaTemplateMock, Mockito.never()).send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    @Test
    fun `skal ikke behandle hendelse nar journalpost tilhorer NKS`() {
        val journalpostId = 123213L
        stubs.mockSts()
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

        hendelseListener.listen(record)

        verify(kafkaTemplateMock, Mockito.never()).send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }
}