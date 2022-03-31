package no.nav.bidrag.dokument.arkiv.hendelser

import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_AKTOER_ID
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_ENHET
import no.nav.bidrag.dokument.arkiv.stubs.Stubs
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class JournalpostKafkdaEventProducerTest: BaseKafkaHendelseTest() {

    private var stubs = Stubs()

    @Test
    @DisplayName("skal publisere journalpost hendelser")
    fun skalPublisereJournalpostHendelser() {
        val journalpostId = 123213L

        stubs.mockSts()
        stubs.mockSafResponseHentJournalpost(opprettSafResponse(
            journalpostId = journalpostId.toString(), journalstatus = JournalStatus.MOTTATT, journalforendeEnhet = BRUKER_ENHET))
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockBidragOrganisasjonSaksbehandler()
        stubs.mockOrganisasjonGeografiskTilknytning(BRUKER_ENHET)

        sendMessageToJoarkTopic(createHendelseRecord(journalpostId))

        await.atMost(4, TimeUnit.SECONDS).untilAsserted {
            val journalpostHendelse = readFromJournalpostTopic()!!
            assertThat(journalpostHendelse).isNotNull
            assertThat(journalpostHendelse.journalpostId).isEqualTo("JOARK-$journalpostId")
            assertThat(journalpostHendelse.journalstatus).isEqualTo("M")
            assertThat(journalpostHendelse.fagomrade).isEqualTo("BID")
            assertThat(journalpostHendelse.aktorId).isEqualTo(BRUKER_AKTOER_ID)
            assertThat(journalpostHendelse.enhet).isEqualTo(BRUKER_ENHET)
            stubs.verifyStub.dokarkivOppdaterIkkeKalt(journalpostId)
            stubs.verifyStub.bidragOrganisasjonGeografiskTilknytningKalt()
        }

    }

//    @Test
//    @DisplayName("skal publisere journalpost hendelser med aktørid når saf returnerer FNR")
//    fun skalPublisereJournalpostHendelserWhenSafReturnFNR() {
//        val journalpostId = 123213L
//        val expectedJoarkJournalpostId = "JOARK-$journalpostId"
//        val brukerIdFnr = "555555"
//        val brukerIdAktorId = "213213323"
//        val jfEnhet = "4833"
//        mockSafResponse(journalpostId, brukerIdFnr, "FNR", jfEnhet)
//        Mockito.`when`(dokarkivConsumer.endre(ArgumentMatchers.any()))
//            .thenReturn(HttpResponse.from(HttpStatus.OK, OppdaterJournalpostResponse(journalpostId)))
//        Mockito.`when`(personConsumer.hentPerson(ArgumentMatchers.any()))
//            .thenReturn(HttpResponse.from(HttpStatus.OK, PersonResponse(brukerIdFnr, brukerIdAktorId)))
//        Mockito.`when`(organisasjonConsumer.hentGeografiskEnhet(ArgumentMatchers.any()))
//            .thenReturn(HttpResponse.from(HttpStatus.OK, GeografiskTilknytningResponse("4806", "navn")))
//        Mockito.`when`(organisasjonConsumer.hentSaksbehandlerInfo(ArgumentMatchers.any()))
//            .thenReturn(HttpResponse.from(HttpStatus.OK, SaksbehandlerInfoResponse("123213", "navn")))
//        val record = JournalfoeringHendelseRecord()
//        record.journalpostId = journalpostId
//        record.hendelsesType = HendelsesType.JOURNALPOST_MOTTATT.hendelsesType
//        record.temaNytt = "BID"
//        record.mottaksKanal = MottaksKanal.NAV_NO.name
//        hendelseListener!!.listen(record)
//        val jsonCaptor = ArgumentCaptor.forClass(String::class.java)
//        Mockito.verify(kafkaTemplateMock)
//            .send(ArgumentMatchers.eq(topicJournalpost), ArgumentMatchers.eq(expectedJoarkJournalpostId), jsonCaptor.capture())
//        Mockito.verify<Any>(safConsumer).hentJournalpost(ArgumentMatchers.eq(journalpostId))
//        val expectedJournalpostId =
//            String.format(/* !!! Hit visitElement for element type: class org.jetbrains.kotlin.nj2k.tree.JKErrorExpression !!! */. trim { it <= ' ' }, expectedJoarkJournalpostId)
//        val aktoerId =
//            String.format(/* !!! Hit visitElement for element type: class org.jetbrains.kotlin.nj2k.tree.JKErrorExpression !!! */. trim { it <= ' ' }, brukerIdAktorId)
//        Assertions.assertThat(jsonCaptor.value).containsSequence(expectedJournalpostId).containsSequence(aktoerId)
//    }
//
//    @Test
//    @DisplayName("skal ignorere hendelse hvis tema ikke er BID")
//    fun shouldIgnoreWhenHendelseIsNotBID() {
//        val journalpostId1 = 123213L
//        val record = JournalfoeringHendelseRecord()
//        record.journalpostId = journalpostId1
//        record.hendelsesType = HendelsesType.JOURNALPOST_MOTTATT.hendelsesType
//        record.temaNytt = "NOT BID"
//        record.mottaksKanal = MottaksKanal.NAV_NO.name
//        hendelseListener!!.listen(record)
//        Mockito.verify(kafkaTemplateMock, Mockito.never()).send(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
//    }
//
//    @Test
//    @DisplayName("skal feile med JournalpostIkkeFunnetException hvis SAF feiler")
//    fun shouldThrowWhenSafFails() {
//        val journalpostId1 = 123213L
//        val record = JournalfoeringHendelseRecord()
//        record.journalpostId = journalpostId1
//        record.hendelsesType = "JournalpostMottatt"
//        record.temaNytt = "BID"
//        record.mottaksKanal = MottaksKanal.NAV_NO.name
//        Assertions.assertThatExceptionOfType(JournalpostIkkeFunnetException::class.java)
//            .isThrownBy { hendelseListener!!.listen(record) }
//            .withMessage("Fant ikke journalpost med id %s".formatted(journalpostId1))
//    }
//
//    private fun mockSafResponse(journalpostId: Long, brukerId: String, brukerType: String, jfEnhet: String) {
//        val safJournalpostResponse = Journalpost()
//        safJournalpostResponse.journalpostId = journalpostId.toString()
//        safJournalpostResponse.bruker = Bruker(brukerId, brukerType)
//        safJournalpostResponse.journalforendeEnhet = jfEnhet
//        Mockito.`when`(safConsumer.hentJournalpost(journalpostId)).thenReturn(safJournalpostResponse)
//    }
}