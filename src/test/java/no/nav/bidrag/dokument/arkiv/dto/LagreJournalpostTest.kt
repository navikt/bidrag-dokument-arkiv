package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.arkiv.stubs.createEndreJournalpostCommand
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.dokument.dto.EndreReturDetaljer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("LagreJournalpostTest")
internal class LagreJournalpostTest {

    @Test
    fun `Skal lagre journalpost med ny returdetaljer`(){
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

        val lagreJournalpost = LagreJournalpostRequest(journalpost = journalpost, endreJournalpostCommand = EndreJournalpostCommandIntern(endreJournalpostCommand, ""), journalpostId = 213)
        assertThat(lagreJournalpost.tilleggsopplysninger?.size).isEqualTo(4)
        assertThat(lagreJournalpost.tilleggsopplysninger?.any { it.values.any { value -> value.contains("2022-11-15") } }).isTrue
        assertThat(lagreJournalpost.tilleggsopplysninger?.any { it.values.any { value -> value.contains("2020-01-02") } }).isTrue
        assertThat(lagreJournalpost.tilleggsopplysninger?.any { it.values.any { value -> value.contains("2020-10-02") } }).isTrue
    }

    @Test
    fun `Skal ikke lagre journalpost med ny returdetaljer hvis ikke kommet i retur`(){
        val tilleggsOpplysninger = TilleggsOpplysninger()
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

        val lagreJournalpost = LagreJournalpostRequest(journalpost = journalpost, endreJournalpostCommand = EndreJournalpostCommandIntern(endreJournalpostCommand, ""), journalpostId = 213)
        assertThat(lagreJournalpost.tilleggsopplysninger?.size).isEqualTo(2)
        assertThat(lagreJournalpost.tilleggsopplysninger?.any { it.values.any { value -> value.contains("2022-11-15") } }).isFalse
        assertThat(lagreJournalpost.tilleggsopplysninger?.any { it.values.any { value -> value.contains("2020-01-02") } }).isTrue
        assertThat(lagreJournalpost.tilleggsopplysninger?.any { it.values.any { value -> value.contains("2020-10-02") } }).isTrue
    }

    @Test
    fun `Skal lagre journalpost endret returdetaljer`(){
        val datoSomSkalEndres = LocalDate.parse("2020-10-02")
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En god begrunnelse for hvorfor dokument kom i retur",
            LocalDate.parse("2020-01-02")
        ))
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(
            "En annen god begrunnelse for hvorfor dokument kom i retur",
            datoSomSkalEndres
        ))
        val journalpost = opprettUtgaendeSafResponse(tilleggsopplysninger = tilleggsOpplysninger, relevanteDatoer = listOf(DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")))
        journalpost.antallRetur = 1
        val endreJournalpostCommand = createEndreJournalpostCommand()
        endreJournalpostCommand.skalJournalfores = false
        endreJournalpostCommand.endreReturDetaljer = listOf(EndreReturDetaljer(datoSomSkalEndres, LocalDate.parse("2022-11-15"), "Ny beskrivelse 1"))

        val lagreJournalpost = LagreJournalpostRequest(journalpost = journalpost, endreJournalpostCommand = EndreJournalpostCommandIntern(endreJournalpostCommand, ""), journalpostId = 213)
        assertThat(lagreJournalpost.tilleggsopplysninger?.size).isEqualTo(3)
        assertThat(lagreJournalpost.tilleggsopplysninger?.any { it.values.any { value -> value.contains("2022-11-15") } }).isTrue
        assertThat(lagreJournalpost.tilleggsopplysninger?.any { it.values.any { value -> value.contains(datoSomSkalEndres.toString()) } }).isFalse
        assertThat(lagreJournalpost.tilleggsopplysninger?.filter { it.values.any { value -> value.contains("2022-11-15")}}?.joinToString()).contains("Ny beskrivelse 1")
        assertThat(lagreJournalpost.tilleggsopplysninger?.any { it.values.any { value -> value.contains("2020-01-02") } }).isTrue
    }
}