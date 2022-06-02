package no.nav.bidrag.dokument.arkiv.hendelser

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal
import no.nav.bidrag.dokument.arkiv.dto.DatoType
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse
import no.nav.bidrag.dokument.arkiv.dto.ReturDetaljerLogDO
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.kafka.HendelseListener
import no.nav.bidrag.dokument.arkiv.model.OppgaveHendelse
import no.nav.bidrag.dokument.arkiv.model.OppgaveStatus
import no.nav.bidrag.dokument.arkiv.stubs.Stubs
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.dokument.arkiv.utils.DateUtils
import no.nav.bidrag.dokument.dto.JournalpostDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ActiveProfiles(value = [BidragDokumentArkivConfig.PROFILE_KAFKA_TEST, BidragDokumentArkivConfig.PROFILE_TEST, BidragDokumentArkivLocal.PROFILE_INTEGRATION])
@SpringBootTest(classes = [BidragDokumentArkivLocal::class])
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
class OppgaveHendelseListenerTest {
    @MockBean
    lateinit var kafkaTemplateMock: KafkaTemplate<String, String>

    @Autowired
    lateinit var hendelseListener: HendelseListener

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var stubs: Stubs

    @AfterEach
    fun cleanupMocks(){
        WireMock.reset()
        WireMock.resetToDefault()
        Mockito.reset(kafkaTemplateMock)
    }

    @Test
    fun shouldOppretteReturLog(){
        stubs.mockSts()
        stubs.mockBidragOrganisasjonSaksbehandler()
        val journalpostId = 201028011L
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
        val safResponse = opprettUtgaendeSafResponse(journalpostId = journalpostId.toString(), tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(
            DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")
        ))
        safResponse.antallRetur = 1

        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockDokarkivOppdaterRequest(journalpostId)


        val oppgaveHendelse = createReturOppgaveHendelse(journalpostId.toString())
        val consumerRecord = ConsumerRecord("test", 0, 0L, "key", objectMapper.writeValueAsString(oppgaveHendelse))
        hendelseListener.lesOppgaveOpprettetHendelse(consumerRecord)

        Assertions.assertAll(
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostId, "\"tilleggsopplysninger\":" +
                            "[{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}," +
                            "{\"nokkel\":\"Lretur0_2020-01-02\",\"verdi\":\"En god begrunnelse for hvorfor dokument kom i retur\"}," +
                            "{\"nokkel\":\"Lretur0_2020-10-02\",\"verdi\":\"En annen god begrunnelse for hvorfor dokument kom i retur\"}," +
                            "{\"nokkel\":\"retur0_${DateUtils.formatDate(LocalDate.now())}\",\"verdi\":\"Returpost\"}]"
                )
            }
        )
    }


    @Test
    fun shouldNotAddReturLoggWhenAlreadyExists(){
        stubs.mockSts()
        stubs.mockBidragOrganisasjonSaksbehandler()
        val journalpostId = 201028011L
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
                LocalDate.parse("2021-08-20"),
                false
            )
        )
        val safResponse = opprettUtgaendeSafResponse(journalpostId = journalpostId.toString(), tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(
            DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")
        ))
        safResponse.antallRetur = 1

        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockDokarkivOppdaterRequest(journalpostId)


        val oppgaveHendelse = createReturOppgaveHendelse(journalpostId.toString())
        val consumerRecord = ConsumerRecord("test", 0, 0L, "key", objectMapper.writeValueAsString(oppgaveHendelse))
        hendelseListener.lesOppgaveOpprettetHendelse(consumerRecord)

        Assertions.assertAll(
            {
                stubs.verifyStub.dokarkivOppdaterIkkeKalt(journalpostId)
            }
        )
    }

    @Test
    fun shouldUnlockReturLogWhenSameDateExists(){
        stubs.mockSts()
        stubs.mockBidragOrganisasjonSaksbehandler()
        val journalpostId = 201028011L
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
                LocalDate.now(),
                true
            )
        )
        val safResponse = opprettUtgaendeSafResponse(journalpostId = journalpostId.toString(), tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(
            DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")
        ))
        safResponse.antallRetur = 1

        stubs.mockSafResponseHentJournalpost(safResponse)
        stubs.mockDokarkivOppdaterRequest(journalpostId)


        val oppgaveHendelse = createReturOppgaveHendelse(journalpostId.toString())
        val consumerRecord = ConsumerRecord("test", 0, 0L, "key", objectMapper.writeValueAsString(oppgaveHendelse))
        hendelseListener.lesOppgaveOpprettetHendelse(consumerRecord)

        Assertions.assertAll(
            {
                stubs.verifyStub.dokarkivOppdaterKalt(
                    journalpostId, "\"tilleggsopplysninger\":" +
                            "[{\"nokkel\":\"distribusjonBestilt\",\"verdi\":\"true\"}," +
                            "{\"nokkel\":\"Lretur0_2020-01-02\",\"verdi\":\"En god begrunnelse for hvorfor dokument kom i retur\"}," +
                            "{\"nokkel\":\"retur0_${DateUtils.formatDate(LocalDate.now())}\",\"verdi\":\"En annen god begrunnelse for hvorfor dokument kom i retur\"}"
                )
            }
        )
    }


    fun createReturOppgaveHendelse(journalpostId: String): OppgaveHendelse {
        return OppgaveHendelse(
            journalpostId = journalpostId,
            id = 1,
            oppgavetype = "RETUR",
            status = OppgaveStatus.OPPRETTET,
            tema = "BID"
        )
    }
}