package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.arkiv.model.ViolationException
import no.nav.bidrag.dokument.arkiv.stubs.createEndreJournalpostCommand
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.transport.dokument.EndreReturDetaljer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DisplayName("Journalpost")
internal class EndreJournalpostValidationTest {

    @Test
    fun `Skal ikke feile hvis returdetaljer er tom`() {
        val journalpost = opprettUtgaendeSafResponse(relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf()
        Assertions.assertDoesNotThrow { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
    }

    @Test
    fun `Skal ikke feile hvis returdetaljer er null`() {
        val journalpost = opprettUtgaendeSafResponse(relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = null
        Assertions.assertDoesNotThrow { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
    }

    @Test
    fun `Skal feile validering hvis det allerede finnes returdetalj etter dokumentdato og originalDato er null`() {
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02")
            )
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2023-10-02")
            )
        )
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(null, LocalDate.parse("2022-01-15"), "Ny beskrivelse 1"))

        val throwable = Assertions.assertThrows(ViolationException::class.java) { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
        assertThat(throwable.message).isEqualTo("Ugyldige data: Kan ikke opprette ny returdetalj (originalDato=null)")
    }

    @Test
    fun `Skal feile validering hvis laast returdetalj endres`() {
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
                LocalDate.parse("2023-10-02")
            )
        )
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2019-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(LocalDate.parse("2020-01-02"), LocalDate.parse("2022-01-15"), "Ny beskrivelse 1"))

        val throwable = Assertions.assertThrows(ViolationException::class.java) { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
        assertThat(throwable.message).isEqualTo("Ugyldige data: Kan ikke endre låste returdetaljer")
    }

    @Test
    fun `Skal feile validering hvis returdetaljer endres paa journalpost som ikke har kommet i retur`() {
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02")
            )
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2021-09-02")
            )
        )
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 0
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(LocalDate.parse("2021-09-02"), LocalDate.parse("2022-01-15"), "Ny beskrivelse 1"))

        val throwable = Assertions.assertThrows(ViolationException::class.java) { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
        assertThat(throwable.message).isEqualTo("Ugyldige data: Kan ikke endre returdetaljer på journalpost som ikke har kommet i retur")
    }

    @Test
    fun `Skal feile validering hvis ny returdato ikke er etter dokumentdato for originalDato null`() {
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02")
            )
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-10-02")
            )
        )
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(null, LocalDate.parse("2020-11-15"), "Ny beskrivelse 1"))

        val throwable = Assertions.assertThrows(ViolationException::class.java) { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
        assertThat(throwable.message).contains("Kan ikke opprette ny returdetalj med returdato før dokumentdato")
    }

    @Test
    fun `Skal feile validering hvis oppdatert returdato er etter dagens dato`() {
        val dokumentDato = LocalDate.parse("2022-02-02")
        val endreDato = dokumentDato.plusDays(1)
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02")
            )
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                endreDato
            )
        )
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType(LocalDateTime.of(dokumentDato, LocalTime.of(0, 0)).toString(), "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(endreDato, LocalDate.now().plusDays(1), "Ny beskrivelse 1"))

        val throwable = Assertions.assertThrows(ViolationException::class.java) { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
        assertThat(throwable.message).isEqualTo("Ugyldige data: Kan ikke oppdatere returdato til etter dagens dato")
    }

    @Test
    fun `Skal feile validering hvis oppdatert returdato for returdetaljer opprettet foer dokumentdato`() {
        val dokumentDato = LocalDate.parse("2022-02-02")
        val endreDato = dokumentDato.minusDays(2)
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02")
            )
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                endreDato
            )
        )
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType(LocalDateTime.of(dokumentDato, LocalTime.of(0, 0)).toString(), "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(endreDato, LocalDate.parse("2021-02-02"), "Ny beskrivelse 1"))

        val throwable = Assertions.assertThrows(ViolationException::class.java) { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
        assertThat(throwable.message).isEqualTo("Ugyldige data: Kan ikke endre returdetaljer opprettet før dokumentdato")
    }

    @Test
    fun `Skal feile validering hvis ny returdetalj har samme dato som laast dato`() {
        val dokumentDato = LocalDate.parse("2022-02-02")
        val endreDato = dokumentDato.plusDays(2)
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
                endreDato,
                true
            )
        )
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType(LocalDateTime.of(dokumentDato, LocalTime.of(0, 0)).toString(), "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(null, endreDato, "Ny beskrivelse 1"))

        val throwable = Assertions.assertThrows(ViolationException::class.java) { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
        assertThat(throwable.message).isEqualTo("Ugyldige data: Kan ikke endre låste returdetaljer")
    }

    @Test
    fun `Skal ikke feile validering hvis oppdatert returdato er lik dokumentdato`() {
        val dokumentDato = LocalDate.parse("2020-10-02")
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02")
            )
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-02-02")
            )
        )
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType(LocalDateTime.of(dokumentDato, LocalTime.of(0, 0)).toString(), "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(null, dokumentDato, "Ny beskrivelse 1"))

        Assertions.assertDoesNotThrow { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
    }

    @Test
    fun `Skal ikke feile validering hvis antallretur er 1, det ikke finnes returdetalj etter dokumentdato og originalDato er null`() {
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02")
            )
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-10-02")
            )
        )
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(null, LocalDate.parse("2022-01-15"), "Ny beskrivelse 1"))

        Assertions.assertDoesNotThrow { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
    }

    @Test
    fun `Skal ikke feile validering ved gyldig endring av returdato`() {
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2020-01-02")
            )
        )
        tilleggsOpplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "En annen god begrunnelse for hvorfor dokument kom i retur",
                LocalDate.parse("2021-10-15")
            )
        )
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(LocalDate.parse("2021-10-15"), LocalDate.parse("2021-11-15"), "Ny beskrivelse 1"))

        Assertions.assertDoesNotThrow { EndreJournalpostCommandIntern(endreJournalpostCommand, "0000").sjekkGyldigEndring(journalpost) }
    }
}
