package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottakerIdType
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.validerAdresse
import no.nav.bidrag.dokument.arkiv.dto.validerKanDistribueres
import no.nav.bidrag.dokument.arkiv.stubs.createDistribuerTilAdresse
import no.nav.bidrag.transport.dokument.DistribuerJournalpostRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DistribuerJournalpostServiceTest {
    @Test
    @DisplayName("skal validere distribuer journalpost")
    fun skalValidere() {
        Assertions.assertDoesNotThrow { validerKanDistribueres(createValidJournalpost()) }
    }

    @Test
    @DisplayName("skal validere av hvis journalpost allerede er distribuert men er tillatt for redistribuering")
    fun skalValidereHvisJournalpostAlleredeErDistribuertMenTillatForRedistribusjon() {
        val jp = createValidJournalpost()
        jp.journalpostId = "123"
        jp.tilleggsopplysninger.setDistribusjonBestillt()
        Assertions.assertDoesNotThrow { validerKanDistribueres(createValidJournalpost()) }
    }

    @Test
    @DisplayName("skal feile validering av distribuer journalpost hvis journalpost allerede er distribuert")
    fun skalIkkeValidereHvisJournalpostAlleredeErDistribuert() {
        val jp = createValidJournalpost()
        jp.tilleggsopplysninger.setDistribusjonBestillt()
        val exceptionResult = Assertions.assertThrows(
            IllegalArgumentException::class.java,
            { validerKanDistribueres(jp) },
            "skal feile hvis journalpost allerede er distribuert"
        )
        org.assertj.core.api.Assertions.assertThat(exceptionResult.message).contains("Journalpost er allerede distribuert")
    }

    @Test
    @DisplayName("skal feile validering av distribuer journalpost hvis status ikke er FERDIGSTILT")
    fun skalIkkeValidereHvisStatusIkkeErFerdigstilt() {
        val jp = createValidJournalpost()
        jp.journalstatus = JournalStatus.JOURNALFOERT
        val exceptionResult = Assertions.assertThrows(
            IllegalArgumentException::class.java,
            { validerKanDistribueres(jp) },
            "skal feile hvis status ikke er FERDIGSTILT"
        )
        org.assertj.core.api.Assertions.assertThat(exceptionResult.message).contains("FERDIGSTILT")
    }

    @Test
    fun `skal ikke feile validering av distribuer journalpost hvis tema er FAR`() {
        val jp = createValidJournalpost()
        jp.tema = "FAR"
        Assertions.assertDoesNotThrow { validerKanDistribueres(createValidJournalpost()) }
    }

    @Test
    @DisplayName("skal feile validering av distribuer journalpost ikke har mottakerid satt")
    fun skalIkkeValidereHvisJournalpostIkkeHarMottakerIdSatt() {
        val jp = createValidJournalpost()
        jp.avsenderMottaker = AvsenderMottaker()
        val exceptionResult = Assertions.assertThrows(
            IllegalArgumentException::class.java,
            { validerKanDistribueres(jp) },
            "Skal feile hvis mottakerid ikke er satt"
        )
        org.assertj.core.api.Assertions.assertThat(exceptionResult.message).contains("mottakerId")
    }

    @Test
    @DisplayName("skal feile validering av distribuer journalpost hvis mottakerid er samhandlerid")
    @Disabled
    fun skalIkkeValidereHvisMottakerIdErSamhandlerId() {
        val jp = createValidJournalpost()
        jp.avsenderMottaker = AvsenderMottaker("", "8123213213", null)
        val exceptionResult = Assertions.assertThrows(
            IllegalArgumentException::class.java,
            { validerKanDistribueres(jp) },
            "Skal feile hvis mottakerid er samhandlerId"
        )
        org.assertj.core.api.Assertions.assertThat(exceptionResult.message).contains("samhandlerId")
    }

    @Nested
    @DisplayName("ValiderAdresseTest")
    internal inner class ValiderAdresseTest {
        @Test
        @DisplayName("skal feile validering av distribuer journalpost hvis adresse ikke er satt")
        fun skalIkkeValidereHvisJournalpostHvisAdresseIkkeErSatt() {
            Assertions.assertThrows(
                IllegalArgumentException::class.java,
                { validerAdresse(null) },
                "Skal validere adresse"
            )
        }

        @Test
        @DisplayName("skal validere adresse")
        fun skalValidereAdresse() {
            val adresse = createDistribuerTilAdresse()
            Assertions.assertDoesNotThrow({ validerAdresse(adresse) }, "Skal validere adresse")
        }

        @Test
        @DisplayName("skal ikke validere norsk adresse uten postnummer")
        fun skalIkkeValidereNorskAdresseSomManglerPostnummer() {
            val adresse = createDistribuerTilAdresse()
            adresse.postnummer = null
            Assertions.assertThrows(
                IllegalArgumentException::class.java,
                { validerAdresse(adresse) },
                "Skal ikke validere norsk adresse uten postnummer"
            )
        }

        @Test
        @DisplayName("skal ikke validere norsk adresse uten poststed")
        fun skalIkkeValidereNorskAdresseSomManglerPoststed() {
            val adresse = createDistribuerTilAdresse()
            adresse.poststed = null
            Assertions.assertThrows(
                IllegalArgumentException::class.java,
                { validerAdresse(adresse) },
                "Skal ikke validere norsk adresse uten poststed"
            )
        }

        @Test
        @DisplayName("skal ikke validere utenlandsk adresse uten adresselinje1")
        fun skalIkkeValidereUtenlandskAdresseSomManglerAdresselinje1() {
            val adresse = createDistribuerTilAdresse()
            adresse.adresselinje1 = null
            adresse.land = "SE"
            Assertions.assertThrows(
                IllegalArgumentException::class.java,
                { validerAdresse(adresse) },
                "Skal ikke validere utenlandsk adresse uten adresselinje1"
            )
        }

        @Test
        @DisplayName("skal ikke validere hvis landkode ikke er formatert som alpha-2 kode")
        fun skalIkkeValidereLandSomErFormatertFeil() {
            val adresse = createDistribuerTilAdresse()
            adresse.adresselinje1 = null
            adresse.land = "SER"
            Assertions.assertThrows(
                IllegalArgumentException::class.java,
                { validerAdresse(adresse) },
                "Skal ikke validere hvis landkode ikke er formatert som alpha-2 kode"
            )
        }
    }

    private fun createValidDistribuerJournalpostRequest(): DistribuerJournalpostRequest {
        return DistribuerJournalpostRequest(null, false, createDistribuerTilAdresse())
    }

    private fun createValidJournalpost(): Journalpost {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.FERDIGSTILT
        journalpost.tema = "BID"
        journalpost.avsenderMottaker = AvsenderMottaker("test", "123213213", AvsenderMottakerIdType.FNR)
        return journalpost
    }
}
