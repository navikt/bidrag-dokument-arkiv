package no.nav.bidrag.dokument.arkiv.dto

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.bidrag.dokument.arkiv.stubs.createOpprettJournalpostRequest
import no.nav.bidrag.transport.dokument.AvsenderMottakerDto
import no.nav.bidrag.transport.dokument.JournalpostType
import no.nav.bidrag.transport.dokument.OpprettDokumentDto
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class OpprettJournalpostValidatorTest {

    @Test
    @Disabled
    fun `Validering skal feile hvis journalposttype er tom`() {
        val request =
            createOpprettJournalpostRequest().copy(journalposttype = JournalpostType.INNGÅENDE)
        val result =
            shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        result.message shouldBe "Journalposttype må settes"
    }

    @Test
    fun `Validering skal feile hvis gjelder ikke er satt`() {
        val request = createOpprettJournalpostRequest().copy(gjelder = null, gjelderIdent = null)
        val result =
            shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        result.message shouldBe "Journalpost må ha satt gjelder ident"
    }

    @Test
    fun `Validering skal feile hvis avsender ikke er satt`() {
        val request = createOpprettJournalpostRequest().copy(avsenderMottaker = null)
        val result =
            shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        val request2 =
            createOpprettJournalpostRequest().copy(avsenderMottaker = AvsenderMottakerDto())
        val result2 =
            shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request2) }

        result.message shouldBe "Journalpost må ha satt avsender/mottaker navn eller ident"
        result2.message shouldBe "Journalpost må ha satt avsender/mottaker navn eller ident"
    }

    @Test
    fun `Validering skal feile hvis tema er satt til noe annet en Bidragstema`() {
        val request =
            createOpprettJournalpostRequest().copy(tema = "NOE_ANNET", skalFerdigstilles = true)
        val result =
            shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        result.message shouldBe "Journalpost som skal ferdigstilles må ha tema BID/FAR"
    }

    @Test
    fun `Validering skal feile hvis tittel ikke er satt på dokument`() {
        val request = createOpprettJournalpostRequest().copy(
            dokumenter = listOf(
                OpprettDokumentDto(
                    tittel = "",
                    fysiskDokument = "Innhold på dokumentet vedlegg".toByteArray()
                )
            )
        )
        val result =
            shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        result.message shouldBe "Dokument 1 mangler tittel. Alle dokumenter må ha satt tittel"
    }

    @Test
    fun `Validering skal feile hvis journalførendeenhet mangler på journalpost som skal journalføres`() {
        val request = createOpprettJournalpostRequest().copy(
            skalFerdigstilles = true,
            tilknyttSaker = listOf(""),
            tema = "BID"
        )
        val result =
            shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        result.message shouldBe "Journalpost som skal ferdigstilles må ha satt journalførendeEnhet"
    }

    @Test
    fun `Validering skal feile hvis sak mangler på journalpost som skal journalføres`() {
        val request = createOpprettJournalpostRequest().copy(
            skalFerdigstilles = true,
            journalførendeEnhet = "4214",
            tema = "BID"
        )
        val result =
            shouldThrow<IllegalArgumentException> { validerKanOppretteJournalpost(request) }

        result.message shouldBe "Journalpost som skal ferdigstilles må ha minst en sak"
    }
}
