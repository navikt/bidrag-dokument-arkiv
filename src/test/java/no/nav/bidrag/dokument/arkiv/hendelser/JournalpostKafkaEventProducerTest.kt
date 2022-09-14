package no.nav.bidrag.dokument.arkiv.hendelser

import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import no.nav.bidrag.dokument.arkiv.dto.TilleggsOpplysninger
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_AKTOER_ID
import no.nav.bidrag.dokument.arkiv.stubs.BRUKER_ENHET
import no.nav.bidrag.dokument.arkiv.stubs.DATO_DOKUMENT
import no.nav.bidrag.dokument.arkiv.stubs.DATO_JOURNALFORT
import no.nav.bidrag.dokument.arkiv.stubs.DATO_RETUR
import no.nav.bidrag.dokument.arkiv.stubs.Stubs
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.Ignore
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

internal class JournalpostKafkdaEventProducerTest: BaseKafkaHendelseTest() {

    @Autowired
    lateinit var stubs: Stubs
    @Test
    @DisplayName("skal publisere journalpost hendelser for mottatt journalpost")
    fun skalPublisereJournalpostHendelser() {
        val journalpostId = 123213L

        stubs.mockSts()
        stubs.mockSafResponseHentJournalpost(opprettSafResponse(
            journalpostId = journalpostId.toString(), journalstatus = JournalStatus.MOTTATT, journalforendeEnhet = BRUKER_ENHET))
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockBidragOrganisasjonSaksbehandler()

        sendMessageToJoarkTopic(createHendelseRecord(journalpostId))

        val journalpostHendelse = readFromJournalpostTopic()!!
        assertThat(journalpostHendelse).isNotNull
        assertThat(journalpostHendelse.journalpostId).isEqualTo("JOARK-$journalpostId")
        assertThat(journalpostHendelse.journalstatus).isEqualTo("M")
        assertThat(journalpostHendelse.fagomrade).isEqualTo("BID")
        assertThat(journalpostHendelse.aktorId).isEqualTo(BRUKER_AKTOER_ID)
        assertThat(journalpostHendelse.enhet).isNull()
        assertThat(journalpostHendelse.dokumentDato).isEqualTo(DATO_DOKUMENT.somDato())
        assertThat(journalpostHendelse.journalfortDato).isNull()
        stubs.verifyStub.dokarkivOppdaterIkkeKalt(journalpostId)
        stubs.verifyStub.harIkkeEnSafKallEtterTilknyttedeJournalposter()
    }

    @Test
    @DisplayName("skal publisere journalpost hendelser for journalført journalpost")
    fun skalPublisereJournalpostHendelserForJournalfortJournalpost() {
        val journalpostId = 123213L
        val sak1 = "123213213"
        val sak2 = "5124123"
        val journalfortAvIdent = "Z123123"
        val journalfortAvNavn = "Saksbehandler navn"
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setJournalfortAvIdent(journalfortAvIdent)
        stubs.mockSts()
        stubs.mockSafResponseTilknyttedeJournalposter(listOf(TilknyttetJournalpost(journalpostId, JournalStatus.FERDIGSTILT, Sak(sak2))))
        stubs.mockSafResponseHentJournalpost(opprettSafResponse(
                journalpostId = journalpostId.toString(),
                journalstatus = JournalStatus.JOURNALFOERT,
                journalforendeEnhet = BRUKER_ENHET,
                sak = Sak(sak1),
                relevanteDatoer = listOf(DATO_DOKUMENT, DATO_JOURNALFORT),
                tilleggsopplysninger = tilleggsOpplysninger,
                journalfortAvNavn = journalfortAvNavn

        ))
        stubs.mockDokarkivOppdaterRequest(journalpostId)
        stubs.mockBidragOrganisasjonSaksbehandler()

        val hendelse = createHendelseRecord(journalpostId);
        hendelse.journalpostStatus = "JOURNALFOERT"
        sendMessageToJoarkTopic(hendelse)

        val journalpostHendelse = readFromJournalpostTopic()!!
        assertThat(journalpostHendelse).isNotNull
        assertThat(journalpostHendelse.journalpostId).isEqualTo("JOARK-$journalpostId")
        assertThat(journalpostHendelse.journalstatus).isEqualTo("J")
        assertThat(journalpostHendelse.fagomrade).isEqualTo("BID")
        assertThat(journalpostHendelse.aktorId).isEqualTo(BRUKER_AKTOER_ID)
        assertThat(journalpostHendelse.enhet).isNull()
        assertThat(journalpostHendelse.dokumentDato).isEqualTo(LocalDateTime.parse(DATO_DOKUMENT.dato).toLocalDate())
        assertThat(journalpostHendelse.journalfortDato).isEqualTo(LocalDateTime.parse(DATO_JOURNALFORT.dato).toLocalDate())
        assertThat(journalpostHendelse.sakstilknytninger?.size).isEqualTo(2)
        assertThat(journalpostHendelse.sakstilknytninger).contains(sak1, sak2)
        assertThat(journalpostHendelse.sporing?.brukerident).isEqualTo(journalfortAvIdent)
        assertThat(journalpostHendelse.sporing?.saksbehandlersNavn).isEqualTo(journalfortAvNavn)
        assertThat(journalpostHendelse.sporing?.enhetsnummer).isEqualTo(BRUKER_ENHET)
        stubs.verifyStub.dokarkivOppdaterIkkeKalt(journalpostId)
        stubs.verifyStub.harEnSafKallEtterTilknyttedeJournalposter()
    }

}