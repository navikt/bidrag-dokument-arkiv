package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.arkiv.model.ViolationException
import no.nav.bidrag.dokument.arkiv.stubs.createEndreJournalpostCommand
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.dokument.dto.EndreReturDetaljer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.time.LocalDate

@DisplayName("Journalpost")
internal class EndreJournalpostValidationTest {

    @Test
    fun `Skal feile validering hvis det allerede finnes returdetalj etter dokumentdato og originalDato er null`(){
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En god begrunnelse for hvorfor dokument kom i retur",
            LocalDate.parse("2020-01-02")
        ))
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En annen god begrunnelse for hvorfor dokument kom i retur",
            LocalDate.parse("2023-10-02")
        ))
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(null, LocalDate.parse("2022-11-15"), "Ny beskrivelse 1"))

        val throwable = Assertions.assertThrows(ViolationException::class.java) { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
        assertThat(throwable.message).contains("Returdetaljer inneholder ugyldig endring av returdato")
    }

    @Test
    fun `Skal feile validering hvis antallretur er 0 originalDato er null`(){
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En god begrunnelse for hvorfor dokument kom i retur",
            LocalDate.parse("2020-01-02")
        ))
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En annen god begrunnelse for hvorfor dokument kom i retur",
            LocalDate.parse("2020-10-02")
        ))
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 0
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(null, LocalDate.parse("2022-11-15"), "Ny beskrivelse 1"))

        val throwable = Assertions.assertThrows(ViolationException::class.java) { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
        assertThat(throwable.message).contains("Returdetaljer inneholder ugyldig endring av returdato")
    }

    @Test
    fun `Skal feile validering hvis ny returdato ikke er etter dokumentdato for originalDato null`(){
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En god begrunnelse for hvorfor dokument kom i retur",
            LocalDate.parse("2020-01-02")
        ))
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En annen god begrunnelse for hvorfor dokument kom i retur",
            LocalDate.parse("2020-10-02")
        ))
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(null, LocalDate.parse("2020-11-15"), "Ny beskrivelse 1"))

        val throwable = Assertions.assertThrows(ViolationException::class.java) { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
        assertThat(throwable.message).contains("Returdetaljer inneholder ugyldig endring av returdato")
    }

    @Test
    fun `Skal ikke feile validering hvis antallretur er 1, det ikke finnes returdetalj etter dokumentdato og originalDato er null`(){
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En god begrunnelse for hvorfor dokument kom i retur",
            LocalDate.parse("2020-01-02")
        ))
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En annen god begrunnelse for hvorfor dokument kom i retur",
            LocalDate.parse("2020-10-02")
        ))
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(null, LocalDate.parse("2022-11-15"), "Ny beskrivelse 1"))

       Assertions.assertDoesNotThrow { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
    }

    @Test
    fun `Skal ikke feile validering hvis original dato ikke er null`(){
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En god begrunnelse for hvorfor dokument kom i retur",
            LocalDate.parse("2020-01-02")
        ))
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En annen god begrunnelse for hvorfor dokument kom i retur",
            LocalDate.parse("2022-10-15")
        ))
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(LocalDate.parse("2022-10-15"), LocalDate.parse("2022-11-15"), "Ny beskrivelse 1"))

        Assertions.assertDoesNotThrow { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
    }
}