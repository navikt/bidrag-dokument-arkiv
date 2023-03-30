package no.nav.bidrag.dokument.arkiv.dto

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.bidrag.dokument.arkiv.stubs.TITTEL_HOVEDDOKUMENT
import no.nav.bidrag.dokument.arkiv.stubs.createJoarkOpprettJournalpostRequest
import org.junit.jupiter.api.Test

class JoarkOpprettJournalpostValidatorTest {

    @Test
    fun `Validering skal feile hvis journalposttype er tom`() {
        val request = createJoarkOpprettJournalpostRequest().copy(journalpostType = null)
        val result = shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        result.message shouldBe "Journalposttype må settes"
    }

    @Test
    fun `Validering skal feile hvis gjelder ikke er satt`() {
        val request = createJoarkOpprettJournalpostRequest().copy(bruker = null)
        val result = shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        result.message shouldBe "Journalpost må ha satt brukerid"
    }

    @Test
    fun `Validering skal feile hvis avsender ikke er satt`() {
        val request = createJoarkOpprettJournalpostRequest().copy(avsenderMottaker = null)
        val result = shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        val request2 = createJoarkOpprettJournalpostRequest().copy(avsenderMottaker = JoarkOpprettJournalpostRequest.OpprettJournalpostAvsenderMottaker())
        val result2 = shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request2) }

        result.message shouldBe "Journalpost må ha satt avsender/mottaker"
        result2.message shouldBe "Journalpost må ha satt avsender/mottaker"
    }

    @Test
    fun `Validering skal feile hvis tema er satt til noe annet en Bidragstema`() {
        val request = createJoarkOpprettJournalpostRequest().copy(tema = "NOE_ANNET")
        val result = shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request, true) }

        result.message shouldBe "Journalpost som skal ferdigstilles må ha tema BID/FAR"
    }

    @Test
    fun `Validering skal feile hvis tittel ikke er satt på dokument`() {
        val request = createJoarkOpprettJournalpostRequest().copy(
            dokumenter = listOf(
                JoarkOpprettJournalpostRequest.Dokument(
                    tittel = "",
                    dokumentvarianter = listOf(
                        JoarkOpprettJournalpostRequest.DokumentVariant(
                            fysiskDokument = "Innhold på dokumentet".toByteArray()
                        )
                    )
                )
            )
        )
        val result = shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        result.message shouldBe "Alle dokumenter må ha tittel"
    }

    @Test
    fun `Validering skal feile hvis fysiskDokument ikke er satt på dokument`() {
        val request = createJoarkOpprettJournalpostRequest().copy(dokumenter = listOf(JoarkOpprettJournalpostRequest.Dokument(tittel = TITTEL_HOVEDDOKUMENT)))
        val result = shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        result.message shouldBe "Dokument \"Tittel på hoveddokument\" må minst ha en dokumentvariant"
    }

    @Test
    fun `Validering skal feile hvis journalførendeenhet mangler på journalpost som skal journalføres`() {
        val request = createJoarkOpprettJournalpostRequest().copy(sak = null)
        val result = shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request, true) }

        result.message shouldBe "Journalpost som skal ferdigstilles må ha satt journalførendeEnhet"
    }

    @Test
    fun `Validering skal feile hvis sak mangler på journalpost som skal journalføres`() {
        val request = createJoarkOpprettJournalpostRequest().copy(journalfoerendeEnhet = "4214")
        val result = shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request, true) }

        result.message shouldBe "Journalpost som skal ferdigstilles må ha minst en sak"
    }
}
