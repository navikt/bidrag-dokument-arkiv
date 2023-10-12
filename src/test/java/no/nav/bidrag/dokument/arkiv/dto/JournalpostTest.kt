package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.arkiv.stubs.RETUR_DETALJER_DATO_1
import no.nav.bidrag.dokument.arkiv.stubs.RETUR_DETALJER_DATO_2
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponse
import no.nav.bidrag.dokument.arkiv.stubs.opprettUtgaendeSafResponseWithReturDetaljer
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.FARSKAP_UTELUKKET_PREFIKS
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
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
        journalpostJsonText =
            String(Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource.file.toURI()))))
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
                assertThat(
                    avsenderMottaker
                ).`as`("avsenderMottaker").isEqualTo(AvsenderMottaker("Tuborg", null, null))
            },
            Executable {
                assertThat(bruker).`as`("bruker").isEqualTo(Bruker("1000024690889", "AKTOERID"))
            },
            Executable {
                assertThat(dokumenter).`as`("dokumenter").isEqualTo(
                    java.util.List.of(
                        Dokument("ROD SNO", "12345", "BI01S02"),
                        Dokument("SNOMANNEN", "56789", "BI01S02")
                    )
                )
            },
            Executable {
                assertThat(journalforendeEnhet).`as`("journalforendeEnhet").isEqualTo("0104")
            },
            Executable {
                assertThat(journalfortAvNavn).`as`("journalfortAvNavn")
                    .isEqualTo("Terkelsen, Karin")
            },
            Executable { assertThat(journalpostId).`as`("journalpostId").isEqualTo("203915975") },
            Executable {
                assertThat(journalposttype).`as`("journalposttype").isEqualTo(JournalpostType.I)
            },
            Executable {
                assertThat(journalstatus).`as`("journalstatus")
                    .isEqualTo(JournalStatus.JOURNALFOERT)
            },
            Executable {
                assertThat(relevanteDatoer).`as`("relevanteDater").isEqualTo(
                    java.util.List.of(
                        DatoType("2010-12-16T00:00", "DATO_JOURNALFOERT"),
                        DatoType("2010-12-15T00:00", "DATO_REGISTRERT"),
                        DatoType("2020-12-15T01:00", "DATO_AVS_RETUR")
                    )
                )
            },
            Executable { assertThat(tema).`as`("tema").isEqualTo("AAP") },
            Executable { assertThat(tittel).`as`("tittel").isEqualTo("...and so on...") }
        )
    }

    @Test
    @DisplayName("skal hente journalført dato")
    @Throws(IOException::class)
    fun skalHenteJournalfortDato() {
        val journalpost = objectMapper.readValue(journalpostJsonText, Journalpost::class.java)
        assertThat(journalpost.hentDatoJournalfort()).isEqualTo(LocalDate.of(2010, 12, 16))
    }

    @Test
    @DisplayName("skal hente registrert dato")
    @Throws(IOException::class)
    fun skalHenteRegistrertDato() {
        val journalpost = objectMapper.readValue(journalpostJsonText, Journalpost::class.java)
        assertThat(journalpost.hentDatoRegistrert()).isEqualTo(LocalDate.of(2010, 12, 15))
    }

    @Test
    @DisplayName("skal hente retur detaljer")
    @Throws(IOException::class)
    fun skalHenteReturDetaljer() {
        val journalpost = objectMapper.readValue(journalpostJsonText, Journalpost::class.java)
        val returDetaljer = journalpost.tilleggsopplysninger.hentReturDetaljerLogDO()
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                assertThat(journalpost.hentDatoRetur()).`as`("datoRegistrert")
                    .isEqualTo(LocalDate.parse("2020-12-15"))
            },
            Executable { assertThat(returDetaljer.size).isEqualTo(3) },
            Executable {
                assertThat(
                    getReturDetaljerDOByDate(
                        returDetaljer,
                        "2020-12-14"
                    )
                ).isNotNull()
            },
            Executable {
                assertThat(
                    getReturDetaljerDOByDate(
                        returDetaljer,
                        "2020-12-14"
                    ).beskrivelse
                ).isEqualTo(
                    "Beskrivelse av retur mer tekst for å teste lengre verdier"
                )
            },
            Executable {
                assertThat(
                    getReturDetaljerDOByDate(
                        returDetaljer,
                        "2020-12-15"
                    ).beskrivelse
                ).isEqualTo(
                    "Beskrivelse av retur 2 mer tekst for å teste lengre verdier"
                )
            },
            Executable {
                assertThat(
                    getReturDetaljerDOByDate(
                        returDetaljer,
                        "2020-11-15"
                    ).beskrivelse
                ).isEqualTo("Beskrivelse av retur")
            }
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
            Executable { assertThat(adresse!!.adresselinje1).isEqualTo("Testveien 20A") },
            Executable { assertThat(adresse!!.adresselinje2).isEqualTo("TestLinje2") },
            Executable { assertThat(adresse!!.adresselinje3).isEqualTo("TestLinje4") },
            Executable { assertThat(adresse!!.postnummer).isEqualTo("7950") },
            Executable { assertThat(adresse!!.poststed).isEqualTo("ABELVÆR") },
            Executable { assertThat(adresse!!.land).isEqualTo("NO") },
            Executable { assertThat(journalpost.hentJournalStatus()).isEqualTo(JournalstatusDto.KLAR_TIL_PRINT) }
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
            Executable { assertThat(journalpost.hentJournalStatus()).isEqualTo(JournalstatusDto.RESERVERT) },
            Executable { assertThat(journalpost.hentJournalpostType()).isEqualTo("X") }
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
            Executable { assertThat(journalpost.hentJournalStatus()).isEqualTo(JournalstatusDto.EKSPEDERT) }
        )
    }

    @Test
    @DisplayName("skal hente avvik hvis Journalpost med status mottatt og inngående")
    fun skalHenteAvvikForMottattOgInngaaende() {
        val journalpost = Journalpost()
        journalpost.tema = "BID"
        journalpost.journalstatus = JournalStatus.MOTTATT
        journalpost.journalposttype = JournalpostType.I
        val avvikListe = journalpost.tilAvvik()
        assertThat(avvikListe).hasSize(3)
        assertThat(avvikListe).contains(AvvikType.OVERFOR_TIL_ANNEN_ENHET)
        assertThat(avvikListe).contains(AvvikType.TREKK_JOURNALPOST)
        assertThat(avvikListe).contains(AvvikType.ENDRE_FAGOMRADE)
    }

    @Test
    @DisplayName("skal hente avvik hvis Journalpost er status er FERDIGSTILT og utgående")
    fun skalHenteAvvikForFERDIGSTILT_Utgaaende() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.FERDIGSTILT
        journalpost.journalposttype = JournalpostType.U
        journalpost.sak = Sak("")
        journalpost.tema = "BID"
        val avvikListe = journalpost.tilAvvik()
        assertThat(avvikListe).hasSize(2)
        assertThat(avvikListe).contains(AvvikType.MANGLER_ADRESSE)
        assertThat(avvikListe).contains(AvvikType.FEILFORE_SAK)
    }

    @Test
    @DisplayName("skal hente avvik hvis Journalpost er status er EKSPEDERT og utgående")
    fun skalHenteAvvikForEKSPEDERT_Utgaaende() {
        val journalpost = Journalpost()
        journalpost.tema = "BID"
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.journalposttype = JournalpostType.U
        journalpost.antallRetur = 1
        journalpost.sak = Sak("")
        val avvikListe = journalpost.tilAvvik()
        assertThat(avvikListe).hasSize(2)
        assertThat(avvikListe).contains(AvvikType.BESTILL_NY_DISTRIBUSJON)
        assertThat(avvikListe).contains(AvvikType.FEILFORE_SAK)
    }

    @Test
    @DisplayName("skal returnere avvik FEILFORE_SAK hvis status ikke er feilfort")
    fun skalHenteAvvikFEILFORE_SAKHvisIkkeFeilfort() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.JOURNALFOERT
        journalpost.journalposttype = JournalpostType.I
        journalpost.sak = Sak("")
        journalpost.tema = "BID"
        val avvikListe = journalpost.tilAvvik()
        assertThat(avvikListe).hasSize(3)
        assertThat(avvikListe).contains(AvvikType.FEILFORE_SAK)
    }

    @Test
    fun `Skal ikke hente avvik FARSKAP UTELUKKET hvis tema FAR og status mottatt`() {
        val journalpost = Journalpost(
            dokumenter = listOf(Dokument("Test tittel"))
        )
        journalpost.journalstatus = JournalStatus.MOTTATT
        journalpost.journalposttype = JournalpostType.I
        journalpost.tema = "FAR"
        val avvikListe = journalpost.tilAvvik()
        assertThat(avvikListe).hasSize(3)
        assertThat(avvikListe).doesNotContain(AvvikType.FARSKAP_UTELUKKET)
    }

    @Test
    fun `Skal ikke hente avvik FARSKAP UTELUKKET hvis tema FAR men har tittel med prefiks FARSKAP UTELUKKET`() {
        val journalpost = Journalpost(
            dokumenter = listOf(Dokument("$FARSKAP_UTELUKKET_PREFIKS: Test tittel"))
        )
        journalpost.journalstatus = JournalStatus.MOTTATT
        journalpost.journalposttype = JournalpostType.U
        journalpost.tema = "FAR"
        val avvikListe = journalpost.tilAvvik()
        assertThat(avvikListe).hasSize(2)
        assertThat(avvikListe).doesNotContain(AvvikType.FARSKAP_UTELUKKET)
    }

    @Test
    fun `Skal ikke hente avvik FARSKAP UTELUKKET hvis tema BID`() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.MOTTATT
        journalpost.journalposttype = JournalpostType.U
        journalpost.tema = "BID"
        val avvikListe = journalpost.tilAvvik()
        assertThat(avvikListe).hasSize(2)
        assertThat(avvikListe).doesNotContain(AvvikType.FARSKAP_UTELUKKET)
    }

    @Test
    fun `Skal hente avvik BESTILL__ hvis kanal skanning`() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.MOTTATT
        journalpost.journalposttype = JournalpostType.I
        journalpost.tema = "BID"
        journalpost.kanal = JournalpostKanal.SKAN_IM
        val avvikListe = journalpost.tilAvvik()
        assertThat(avvikListe).hasSize(6)
        assertThat(avvikListe).contains(AvvikType.BESTILL_ORIGINAL)
        assertThat(avvikListe).contains(AvvikType.BESTILL_SPLITTING)
        assertThat(avvikListe).contains(AvvikType.BESTILL_RESKANNING)
    }

    @Test
    fun `Skal ikke hente avvik BESTILL_ORIGINAL hvis bestilt hvis kanal skanning`() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.MOTTATT
        journalpost.journalposttype = JournalpostType.I
        journalpost.tema = "BID"
        journalpost.kanal = JournalpostKanal.SKAN_IM
        journalpost.tilleggsopplysninger.setOriginalBestiltFlagg()
        val avvikListe = journalpost.tilAvvik()
        assertThat(avvikListe).hasSize(5)
        assertThat(avvikListe).doesNotContain(AvvikType.BESTILL_ORIGINAL)
        assertThat(avvikListe).contains(AvvikType.BESTILL_SPLITTING)
        assertThat(avvikListe).contains(AvvikType.BESTILL_RESKANNING)
    }

    @Test
    fun `Skal ikke hente avvik BESTILL_SPLITTING og BESTILL_RESKANNING hvis feilført`() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.FEILREGISTRERT
        journalpost.journalposttype = JournalpostType.I
        journalpost.tema = "BID"
        journalpost.kanal = JournalpostKanal.SKAN_IM
        val avvikListe = journalpost.tilAvvik()
        assertThat(avvikListe).hasSize(0)
        assertThat(avvikListe).doesNotContain(AvvikType.BESTILL_ORIGINAL)
        assertThat(avvikListe).doesNotContain(AvvikType.BESTILL_SPLITTING)
        assertThat(avvikListe).doesNotContain(AvvikType.BESTILL_RESKANNING)
    }

    @Test
    @DisplayName("skal ikke tillate avvik FEILFORE_SAK hvis status feilregistrert")
    fun skalHenteAvvikHvisStatusFeilregistrert() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.FEILREGISTRERT
        journalpost.journalposttype = JournalpostType.I
        journalpost.sak = Sak("")
        val avvikListe = journalpost.tilAvvik()
        assertThat(avvikListe).hasSize(1)
        assertThat(avvikListe).doesNotContain(AvvikType.FEILFORE_SAK)
    }

    @Test
    fun `should map returdetaljer when distribusjon bestilt`() {
        val journalpost = opprettUtgaendeSafResponse()
        val tilleggsOpplysninger = TilleggsOpplysninger()
        tilleggsOpplysninger.setDistribusjonBestillt()
        journalpost.tilleggsopplysninger = tilleggsOpplysninger
        journalpost.antallRetur = 1
        val journalpostDto = journalpost.tilJournalpostDto()
        assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(1)
        assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(1)
        assertThat(journalpostDto.returDetaljer?.dato).isNull()
        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isNull()
        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("Returpost")
    }

    @Test
    fun `should map returdetaljer when ekspedert`() {
        val journalpost = opprettUtgaendeSafResponse()
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        val journalpostDto = journalpost.tilJournalpostDto()
        assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(1)
        assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(1)
        assertThat(journalpostDto.returDetaljer?.dato).isNull()
        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isNull()
        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("Returpost")
    }

    @Test
    fun `skal legge til ny returdetalj uten dato hvis returdetaljer mangler for siste retur og returdato mangler`() {
        val journalpost = opprettUtgaendeSafResponseWithReturDetaljer()
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        journalpost.relevanteDatoer = listOf(DatoType("2023-08-18T13:20:33", "DATO_DOKUMENT"))
        val journalpostDto = journalpost.tilJournalpostDto()
        assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(3)
        assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(3)
        assertThat(journalpostDto.returDetaljer?.dato).isNull()

        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isNull()
        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("Returpost")

        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(RETUR_DETALJER_DATO_1)
        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.dato).isEqualTo(RETUR_DETALJER_DATO_2)
        assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")
    }

    @Test
    fun `skal legge til ny returdetalj med dato hvis returdetaljer mangler for siste retur`() {
        val journalpost = opprettUtgaendeSafResponseWithReturDetaljer()
        val returDato = LocalDateTime.parse("2022-05-10T13:20:33")
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        journalpost.relevanteDatoer = listOf(
            DatoType("2023-08-18T13:20:33", "DATO_DOKUMENT"),
            DatoType(returDato.toString(), "DATO_AVS_RETUR")
        )
        val journalpostDto = journalpost.tilJournalpostDto()
        assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(3)
        assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(3)
        assertThat(journalpostDto.returDetaljer?.dato).isEqualTo(returDato.toLocalDate())

        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isEqualTo(returDato.toLocalDate())
        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("Returpost")

        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(RETUR_DETALJER_DATO_1)
        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.dato).isEqualTo(RETUR_DETALJER_DATO_2)
        assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")
    }

    @Test
    fun `skal legge til ny returdetalj hvis journalpost har returdetalj lik dokumentdato men er låst`() {
        val sistRetur = LocalDate.parse("2023-08-18")
        val journalpost = opprettUtgaendeSafResponse()
        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier",
                RETUR_DETALJER_DATO_2,
                true
            )
        )
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier",
                sistRetur,
                true
            )
        )
        tilleggsopplysninger.setDistribusjonBestillt()
        journalpost.tilleggsopplysninger = tilleggsopplysninger
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        journalpost.relevanteDatoer = listOf(
            DatoType(
                LocalDateTime.of(sistRetur, LocalTime.of(1, 1)).toString(),
                "DATO_DOKUMENT"
            )
        )
        val journalpostDto = journalpost.tilJournalpostDto()
        assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(3)
        assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(3)
        assertThat(journalpostDto.returDetaljer?.dato).isNull()

        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isNull()
        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("Returpost")

        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(RETUR_DETALJER_DATO_2)
        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.dato).isEqualTo(sistRetur)
        assertThat(journalpostDto.returDetaljer?.logg?.get(2)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")
    }

    @Test
    fun `skal ikke legge til ny returdetalj hvis journalpost har returdetalj lik dokumentdato`() {
        val sistRetur = LocalDate.parse("2023-08-18")
        val journalpost = opprettUtgaendeSafResponse()
        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier",
                LocalDate.parse("2022-10-22")
            )
        )
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier",
                sistRetur
            )
        )
        tilleggsopplysninger.setDistribusjonBestillt()
        journalpost.tilleggsopplysninger = tilleggsopplysninger
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        journalpost.relevanteDatoer = listOf(
            DatoType(
                LocalDateTime.of(sistRetur, LocalTime.of(1, 1)).toString(),
                "DATO_DOKUMENT"
            )
        )
        val journalpostDto = journalpost.tilJournalpostDto()
        assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(2)
        assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(2)
        assertThat(journalpostDto.returDetaljer?.dato).isEqualTo(sistRetur)

        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isEqualTo(LocalDate.parse("2022-10-22"))
        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(sistRetur)
        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")
    }

    @Test
    fun `skal ikke legge til ny returdetalj hvis journalpost har returdetalj etter dokumentdato`() {
        val sistRetur = LocalDate.parse("2023-09-18")

        val journalpost = opprettUtgaendeSafResponse()
        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier",
                LocalDate.parse("2022-10-22")
            )
        )
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier",
                sistRetur
            )
        )
        tilleggsopplysninger.setDistribusjonBestillt()
        journalpost.tilleggsopplysninger = tilleggsopplysninger
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 1
        journalpost.relevanteDatoer = listOf(DatoType("2023-08-18T13:20:33", "DATO_DOKUMENT"))
        val journalpostDto = journalpost.tilJournalpostDto()
        assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(2)
        assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(2)
        assertThat(journalpostDto.returDetaljer?.dato).isEqualTo(sistRetur)

        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isEqualTo(LocalDate.parse("2022-10-22"))
        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(sistRetur)
        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")
    }

    @Test
    fun `skal ikke legge til ny returdetalj hvis journalpost ikke har kommet i retur`() {
        val journalpost = opprettUtgaendeSafResponseWithReturDetaljer()
        val sistRetur = RETUR_DETALJER_DATO_2
        journalpost.journalstatus = JournalStatus.EKSPEDERT
        journalpost.antallRetur = 0
        journalpost.relevanteDatoer = listOf(DatoType("2023-08-18T13:20:33", "DATO_DOKUMENT"))
        val journalpostDto = journalpost.tilJournalpostDto()
        assertThat(journalpostDto.returDetaljer?.antall).isEqualTo(2)
        assertThat(journalpostDto.returDetaljer?.logg?.size).isEqualTo(2)
        assertThat(journalpostDto.returDetaljer?.dato).isNull()

        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.dato).isEqualTo(RETUR_DETALJER_DATO_1)
        assertThat(journalpostDto.returDetaljer?.logg?.get(0)?.beskrivelse).isEqualTo("1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")

        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.dato).isEqualTo(sistRetur)
        assertThat(journalpostDto.returDetaljer?.logg?.get(1)?.beskrivelse).isEqualTo("2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier")
    }

    @Test
    fun `skal laase returdetaljer`() {
        val tilleggsopplysninger = TilleggsOpplysninger()
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "1 - Beskrivelse av retur med litt lengre test for å teste lengre verdier",
                RETUR_DETALJER_DATO_1,
                false
            )
        )
        tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "2 - Beskrivelse av retur med litt lengre test for å teste lengre verdier",
                RETUR_DETALJER_DATO_2,
                false
            )
        )
        tilleggsopplysninger.setDistribusjonBestillt()
        tilleggsopplysninger.lockAllReturDetaljerLog()
        val returDetaljerLog = tilleggsopplysninger.hentReturDetaljerLogDO()
        assertThat(returDetaljerLog[0].locked).isTrue
        assertThat(returDetaljerLog[1].locked).isTrue
        assertThat(returDetaljerLog[0].dato).isEqualTo(RETUR_DETALJER_DATO_1)
        assertThat(returDetaljerLog[1].dato).isEqualTo(RETUR_DETALJER_DATO_2)
    }

    private fun getReturDetaljerDOByDate(
        returDetaljerLogDOList: List<ReturDetaljerLogDO>,
        dato: String
    ): ReturDetaljerLogDO {
        return returDetaljerLogDOList.stream()
            .filter { (_, dato1): ReturDetaljerLogDO -> dato1 == LocalDate.parse(dato) }.findFirst()
            .orElse(
                ReturDetaljerLogDO("junit", LocalDate.now())
            )
    }
}
