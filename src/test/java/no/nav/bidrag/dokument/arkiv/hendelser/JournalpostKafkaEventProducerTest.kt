package no.nav.bidrag.dokument.arkiv.hendelser

import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_AKTOER_ID
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_ENHET
import no.nav.bidrag.dokument.arkiv.stubs.Stubs
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.Ignore
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

@Disabled
internal class JournalpostKafkdaEventProducerTest: BaseKafkaHendelseTest() {

    @Autowired
    lateinit var stubs: Stubs
    @Test
    @Disabled("Not working when runinning on pipeline")
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

}