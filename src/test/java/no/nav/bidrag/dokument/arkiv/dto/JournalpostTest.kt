package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.arkiv.stubs.RETUR_DETALJER_DATO_1
import no.nav.bidrag.dokument.arkiv.stubs.RETUR_DETALJER_DATO_2
import no.nav.bidrag.dokument.arkiv.stubs.opprettSafResponse
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponseWithReturDetaljer
import no.nav.bidrag.dokument.dto.AvvikType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Objects

@DisplayName("Journalpost")
internal class JournalpostTest {
    private val objectMapper = ObjectMapper()
    private var journalpostJsonText: String? = null
    @BeforeEach
    @Throws(IOException::class)
    fun lesJsonResourceTilStreng() {
        val responseJsonResource: Resource = ClassPathResource("__files/json/journalpost.json")
        journalpostJsonText = String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.file.toURI()))))
    }

    @Test
    @DisplayName("skal mappe en journalpost fra json")
    @Throws(IOException::class)
    fun skalMappeJournalpostFraJson() {
        val (avsenderMottaker, bruker, dokumenter, journalforendeEnhet, journalfortAvNavn, journalpostId, journalposttype, _, journalstatus, relevanteDatoer, _, tema, _, tittel) = objectMapper.readValue(
            journalpostJsonText,
            Journalpost::class.java
        )
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(
                    avsenderMottaker
                ).`as`("avsenderMottaker").isEqualTo(AvsenderMottaker("Tuborg", null, null))
            },
            Executable { Assertions.assertThat(bruker).`as`("bruker").isEqualTo(Bruker("1000024690889", "AKTOERID")) },
            Executable {
                Assertions.assertThat(dokumenter).`as`("dokumenter").isEqualTo(
                    java.util.List.of(
                        Dokument("ROD SNO", "12345", "BI01S02"),
                        Dokument("SNOMANNEN", "56789", "BI01S02")
                    )
                )
            },
            Executable { Assertions.assertThat(journalforendeEnhet).`as`("journalforendeEnhet").isEqualTo("0104") },
            Executable { Assertions.assertThat(journalfortAvNavn).`as`("journalfortAvNavn").isEqualTo("Terkelsen, Karin") },
            Executable { Assertions.assertThat(journalpostId).`as`("journalpostId").isEqualTo("203915975") },
            Executable { Assertions.assertThat(journalposttype).`as`("journalposttype").isEqualTo(JournalpostType.I) },
            Executable { Assertions.assertThat(journalstatus).`as`("journalstatus").isEqualTo(JournalStatus.JOURNALFOERT) },
            Executable {
                Assertions.assertThat(relevanteDatoer).`as`("relevanteDater").isEqualTo(
                    java.util.List.of(
                        DatoType("2010-12-16T00:00", "DATO_JOURNALFOERT"),
                        DatoType("2010-12-15T00:00", "DATO_REGISTRERT"),
                        DatoType("2020-12-15T01:00", "DATO_AVS_RETUR")
                    )
                )
            },
            Executable { Assertions.assertThat(tema).`as`("tema").isEqualTo("AAP") },
            Executable { Assertions.assertThat(tittel).`as`("tittel").isEqualTo("...and so on...") }
        )
    }

    @Test
    @DisplayName("skal hente journalført dato")
    @Throws(IOException::class)
    fun skalHenteJournalfortDato() {
        val journalpost = objectMapper.readValue(journalpostJsonText, Journalpost::class.java)
        Assertions.assertThat(journalpost.hentDatoJournalfort()).isEqualTo(LocalDate.of(2010, 12, 16))
    }

    @Test
    @DisplayName("skal hente registrert dato")
    @Throws(IOException::class)
    fun skalHenteRegistrertDato() {
        val journalpost = objectMapper.readValue(journalpostJsonText, Journalpost::class.java)
        Assertions.assertThat(journalpost.hentDatoRegistrert()).isEqualTo(LocalDate.of(2010, 12, 15))
    }

    @Test
    @DisplayName("skal hente retur detaljer")
    @Throws(IOException::class)
    fun skalHenteReturDetaljer() {
        val journalpost = objectMapper.readValue(journalpostJsonText, Journalpost::class.java)
        val returDetaljer = journalpost.tilleggsopplysninger.hentReturDetaljerLogDO()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(journalpost.hentDatoRetur()).`as`("datoRegistrert").isEqualTo(LocalDate.parse("2020-12-15")) },
            Executable { Assertions.assertThat(returDetaljer.size).isEqualTo(3) },
            Executable { Assertions.assertThat(getReturDetaljerDOByDate(returDetaljer, "2020-12-14")).isNotNull() },
            Executable {
                Assertions.assertThat(getReturDetaljerDOByDate(returDetaljer, "2020-12-14").beskrivelse).isEqualTo(
                    "Beskrivelse av retur mer tekst for å teste lengre verdier"
                )
            },
            Executable {
                Assertions.assertThat(getReturDetaljerDOByDate(returDetaljer, "2020-12-15").beskrivelse).isEqualTo(
                    "Beskrivelse av retur 2 mer tekst for å teste lengre verdier"
                )
            },
            Executable { Assertions.assertThat(getReturDetaljerDOByDate(returDetaljer, "2020-11-15").beskrivelse).isEqualTo("Beskrivelse av retur") }
        )
    }

    @Test
    @DisplayName("skal hente distribuert adresse")
    @Throws(IOException::class)
    fun skalHenteDistribuertAdresse() {
        val journalpost = objectMapper.readValue(journalpostJsonText, Journalpost::class.java)
        journalpost.journalstatus = JournalStatus.FERDIGSTILT
        journalpost.journalposttype = JournalpostType.U
        val adresse = journalpost.tilleggsopplysninger.hentAdresseDo()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(adresse!!.adresselinje1).isEqualTo("Testveien 20A") },
            Executable { Assertions.assertThat(adresse!!.adresselinje2).isEqualTo("TestLinje2") },
            Executable { Assertions.assertThat(adresse!!.adresselinje3).isEqualTo("TestLinje4") },
            Executable { Assertions.assertThat(adresse!!.postnummer).isEqualTo("7950") },
            Executable { Assertions.assertThat(adresse!!.poststed).isEqualTo("ABELVÆR") },
            Executable { Assertions.assertThat(adresse!!.land).isEqualTo("NO") },
            Executable { Assertions.assertThat(journalpost.hentJournalStatus()).isEqualTo(JournalstatusDto.KLAR_TIL_PRINT) }
        )
    }

    @Test
    @DisplayName("skal hente status JOURNALFØRT når notat and ferdigstilt")
    @Throws(IOException::class)
    fun skalHenteJournalpostStatusJournalfortForNotat() {
        val journalpost = objectMapper.readValue(journalpostJsonText, Journalpost::class.java)
        journalpost.journalstatus = JournalStatus.FERDIGSTILT
        journalpost.journalposttype = JournalpostType.N
        journalpost.tilleggsopplysninger.setDistribusjonBestillt()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(journalpost.hentJournalStatus()).isEqualTo(JournalstatusDto.JOURNALFORT) },
            Executable { Assertions.assertThat(journalpost.hentJournalpostType()).isEqualTo("X") }
        )
    }

    @Test
    @DisplayName("skal hente status EKSPEDERT når distribuert")
    @Throws(IOException::class)
    fun skalHenteJournalpostStatusEkspedertNarDistribuert() {
        val journalpost = objectMapper.readValue(journalpostJsonText, Journalpost::class.java)
        journalpost.journalstatus = JournalStatus.FERDIGSTILT
        journalpost.journalposttype = JournalpostType.U
        journalpost.tilleggsopplysninger.setDistribusjonBestillt()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(journalpost.hentJournalStatus()).isEqualTo(JournalstatusDto.EKSPEDERT) }
        )
    }

    @Test
    @DisplayName("skal hente avvik hvis Journalpost med status mottatt og inngående")
    fun skalHenteAvvikForMottattOgInngaaende() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.MOTTATT
        journalpost.journalposttype = JournalpostType.I
        val avvikListe = journalpost.tilAvvik()
        Assertions.assertThat(avvikListe).hasSize(3)
        Assertions.assertThat(avvikListe).contains(AvvikType.OVERFOR_TIL_ANNEN_ENHET)
        Assertions.assertThat(avvikListe).contains(AvvikType.TREKK_JOURNALPOST)
        Assertions.assertThat(avvikListe).contains(AvvikType.ENDRE_FAGOMRADE)
    }

    @Test
    @DisplayName("skal hente avvik hvis Journalpost er status er FERDIGSTILT og utgående")
    fun skalHenteAvvikForFERDIGSTILT_Utgaaende() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.FERDIGSTILT
        journalpost.journalposttype = JournalpostType.U
        journalpost.sak = Sak("")
        val avvikListe = journalpost.tilAvvik()
        Assertions.assertThat(avvikListe).hasSize(2)
        Assertions.assertThat(avvikListe).contains(AvvikType.MANGLER_ADRESSE)
        Assertions.assertThat(avvikListe).contains(AvvikType.FEILFORE_SAK)
    }

    @Test
    @DisplayName("skal hente avvik hvis Journalpost er status er EKSPEDERT og utgående")
    fun skalHenteAvvikForEKSPEDERT_Utgaaende() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.journalposttype = JournalpostType.U
        journalpost.antallRetur = 1
        journalpost.sak = Sak("")
        val avvikListe = journalpost.tilAvvik()
        Assertions.assertThat(avvikListe).hasSize(2)
        Assertions.assertThat(avvikListe).contains(AvvikType.BESTILL_NY_DISTRIBUSJON)
        Assertions.assertThat(avvikListe).contains(AvvikType.FEILFORE_SAK)
    }

    @Test
    @DisplayName("skal returnere avvik FEILFORE_SAK hvis status ikke er feilfort")
    fun skalHenteAvvikFEILFORE_SAKHvisIkkeFeilfort() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.JOURNALFOERT
        journalpost.journalposttype = JournalpostType.I
        journalpost.sak = Sak("")
        val avvikListe = journalpost.tilAvvik()
        Assertions.assertThat(avvikListe).hasSize(3)
        Assertions.assertThat(avvikListe).contains(AvvikType.FEILFORE_SAK)
    }

    @Test
    @DisplayName("skal ikke tillate avvik FEILFORE_SAK hvis status feilregistrert")
    fun skalHenteAvvikHvisStatusFeilregistrert() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.FEILREGISTRERT
        journalpost.journalposttype = JournalpostType.I
        journalpost.sak = Sak("")
        val avvikListe = journalpost.tilAvvik()
        Assertions.assertThat(avvikListe).hasSize(0)
        Assertions.assertThat(avvikListe).doesNotContain(AvvikType.FEILFORE_SAK)
    }

    @Test
    fun `should map returdetaljer when distribusjon bestilt`() {
        val journalpost = opprettUtgaendeSafResponse()
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        journalpost.tilleggsopplysninger = tilleggsOpplysninger
        journalpost.antallRetur = 1
        val journalpostDto = journalpost.tilJournalpostDto()
        Assertions.assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(1)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(1)
        Assertions.assertThat(journalpostDto.returDetaljer?.dato).isNull()
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isNull()
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("Returpost")
    }

    @Test
    fun `should map returdetaljer when ekspedert`() {
        val journalpost = opprettUtgaendeSafResponse()
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        val journalpostDto = journalpost.tilJournalpostDto()
        Assertions.assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(1)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(1)
        Assertions.assertThat(journalpostDto.returDetaljer?.dato).isNull()
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isNull()
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("Returpost")
    }

    @Test
    fun `skal legge til ny returdetalj uten dato hvis returdetaljer mangler for siste retur og returdato mangler`() {
        val journalpost = opprettUtgaendeSafResponseWithReturDetaljer()
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        journalpost.relevanteDatoer = listOf(DatoType("2023-08-18T13:20:33", "DATO_DOKUMENT"))
        val journalpostDto = journalpost.tilJournalpostDto()
        Assertions.assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(3)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(3)
        Assertions.assertThat(journalpostDto.returDetaljer?.dato).isNull()

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isNull()
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("Returpost")

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(RETUR_DETALJER_DATO_1)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.dato).isEqualTo(RETUR_DETALJER_DATO_2)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

    }

    @Test
    fun `skal legge til ny returdetalj med dato hvis returdetaljer mangler for siste retur`() {
        val journalpost = opprettUtgaendeSafResponseWithReturDetaljer()
        val returDato = LocalDateTime.parse("2022-05-10T13:20:33")
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        journalpost.relevanteDatoer = listOf(DatoType("2023-08-18T13:20:33", "DATO_DOKUMENT"), DatoType(returDato.toString(), "DATO_AVS_RETUR"))
        val journalpostDto = journalpost.tilJournalpostDto()
        Assertions.assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(3)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(3)
        Assertions.assertThat(journalpostDto.returDetaljer?.dato).isEqualTo(returDato.toLocalDate())

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isEqualTo(returDato.toLocalDate())
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("Returpost")

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(RETUR_DETALJER_DATO_1)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.dato).isEqualTo(RETUR_DETALJER_DATO_2)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")
    }

    @Test
    fun `skal legge til ny returdetalj hvis journalpost har returdetalj lik dokumentdato men er låst`() {
        val sistRetur = LocalDate.parse("2023-08-18")
        val journalpost = opprettUtgaendeSafResponse()
        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier", RETUR_DETALJER_DATO_2, true)
        )
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier", sistRetur, true)
        )
        tilleggsopplysninger.setDistribusjonBestillt()
        journalpost.tilleggsopplysninger = tilleggsopplysninger
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        journalpost.relevanteDatoer = listOf(DatoType(LocalDateTime.of(sistRetur, LocalTime.of(1, 1)).toString(), "DATO_DOKUMENT"))
        val journalpostDto = journalpost.tilJournalpostDto()
        Assertions.assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(3)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(3)
        Assertions.assertThat(journalpostDto.returDetaljer?.dato).isNull()

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isNull()
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("Returpost")

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(RETUR_DETALJER_DATO_2)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.dato).isEqualTo(sistRetur)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")
    }

    @Test
    fun `skal ikke legge til ny returdetalj hvis journalpost har returdetalj lik dokumentdato`() {
        val sistRetur = LocalDate.parse("2023-08-18")
        val journalpost = opprettUtgaendeSafResponse()
        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier", LocalDate.parse("2022-10-22"))
        )
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier", sistRetur)
        )
        tilleggsopplysninger.setDistribusjonBestillt()
        journalpost.tilleggsopplysninger = tilleggsopplysninger
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        journalpost.relevanteDatoer = listOf(DatoType(LocalDateTime.of(sistRetur, LocalTime.of(1, 1)).toString(), "DATO_DOKUMENT"))
        val journalpostDto = journalpost.tilJournalpostDto()
        Assertions.assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(2)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(2)
        Assertions.assertThat(journalpostDto.returDetaljer?.dato).isEqualTo(sistRetur)

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isEqualTo(LocalDate.parse("2022-10-22"))
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(sistRetur)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")
    }

    @Test
    fun `skal ikke legge til ny returdetalj hvis journalpost har returdetalj etter dokumentdato`() {
        val sistRetur = LocalDate.parse("2023-09-18")

        val journalpost = opprettUtgaendeSafResponse()
        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier", LocalDate.parse("2022-10-22"))
        )
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier", sistRetur)
        )
        tilleggsopplysninger.setDistribusjonBestillt()
        journalpost.tilleggsopplysninger = tilleggsopplysninger
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        journalpost.relevanteDatoer = listOf(DatoType("2023-08-18T13:20:33", "DATO_DOKUMENT"))
        val journalpostDto = journalpost.tilJournalpostDto()
        Assertions.assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(2)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(2)
        Assertions.assertThat(journalpostDto.returDetaljer?.dato).isEqualTo(sistRetur)

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isEqualTo(LocalDate.parse("2022-10-22"))
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(sistRetur)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")
    }

    @Test
    fun `skal ikke legge til ny returdetalj hvis journalpost ikke har kommet i retur`() {
        val journalpost = opprettUtgaendeSafResponseWithReturDetaljer()
        val sistRetur = RETUR_DETALJER_DATO_2
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 0
        journalpost.relevanteDatoer = listOf(DatoType("2023-08-18T13:20:33", "DATO_DOKUMENT"))
        val journalpostDto = journalpost.tilJournalpostDto()
        Assertions.assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(2)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(2)
        Assertions.assertThat(journalpostDto.returDetaljer?.dato).isNull()

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isEqualTo(RETUR_DETALJER_DATO_1)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(sistRetur)
        Assertions.assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")
    }
    private fun getReturDetaljerDOByDate(returDetaljerLogDOList: List<ReturDetaljerLogDO>, dato: String): ReturDetaljerLogDO {
        return returDetaljerLogDOList.stream().filter { (_, dato1): ReturDetaljerLogDO -> dato1 == LocalDate.parse(dato) }.findFirst().orElse(
            ReturDetaljerLogDO("junit", LocalDate.now())
        )
    }
}