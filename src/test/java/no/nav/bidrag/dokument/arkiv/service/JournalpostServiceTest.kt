package no.nav.bidrag.dokument.arkiv.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivTest
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer
import no.nav.bidrag.dokument.arkiv.dto.DokumentoversiktFagsakQueryResponse
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.JournalpostType
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.domain.ident.AktørId
import no.nav.bidrag.domain.ident.PersonIdent
import no.nav.bidrag.domain.string.FulltNavn
import no.nav.bidrag.transport.person.PersonDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.Objects
import java.util.Optional

@ActiveProfiles(BidragDokumentArkivConfig.PROFILE_TEST)
@SpringBootTest(classes = [BidragDokumentArkivTest::class])
@DisplayName("JournalpostService")
@EnableMockOAuth2Server
@ExtendWith(SpringExtension::class)
@Disabled
internal class JournalpostServiceTest {
    @Autowired
    private val objectMapper: ObjectMapper? = null

    @RelaxedMockK
    private lateinit var safConsumerMock: SafConsumer

    @RelaxedMockK
    private lateinit var personConsumerMock: PersonConsumer

    @Autowired
    private val journalpostService: ResourceByDiscriminator<JournalpostService>? = null

    @Value("classpath:__files/json/dokumentoversiktFagsakQueryResponse.json")
    private val responseJsonResource: Resource? = null

    @Test
    @DisplayName("skal oversette Map fra consumer til JournalpostDto")
    @Throws(IOException::class)
    fun skalOversetteMapFraConsumerTilJournalpostDto() {
        val jsonResponse = Files.readAllBytes(Paths.get(Objects.requireNonNull(responseJsonResource!!.file.toURI())))

        val dokumentoversiktFagsakQueryResponse = objectMapper!!.readValue(jsonResponse, DokumentoversiktFagsakQueryResponse::class.java)
        val journalpostIdFraJson = 201028011L
        val journalpostDokOversikt = dokumentoversiktFagsakQueryResponse.hentJournalpost(journalpostIdFraJson)
        every { safConsumerMock.hentJournalpost(journalpostIdFraJson) } returns journalpostDokOversikt
        every { personConsumerMock.hentPerson(journalpostDokOversikt.bruker!!.id) } returns Optional.of(
            PersonDto(
                ident = PersonIdent("123123"),
                navn = FulltNavn(""),
                aktørId = AktørId("555555"),
            ),
        )
        val muligJournalpost =
            journalpostService!!.get(Discriminator.REGULAR_USER).hentJournalpostMedFnrOgTilknyttedeSaker(journalpostIdFraJson, null)

        muligJournalpost.isPresent shouldBe true
        val journalpost = muligJournalpost.get()
        val avsenderMottaker = journalpost.avsenderMottaker
        val bruker = journalpost.bruker
        val dokumenter = journalpost.dokumenter
        assertSoftly {
            bruker!!.id shouldBe "123123"
            bruker.type shouldBe "FNR"
            avsenderMottaker!!.navn shouldBe "Draugen, Do"
            journalpost.journalposttype shouldBe JournalpostType.N
            journalpost.tema shouldBe "BID"
            journalpost.tittel shouldBe "Filosofens bidrag"
            journalpost.journalforendeEnhet shouldBe "4817"
            journalpost.journalfortAvNavn shouldBe "Bånn, Jaims"
            journalpost.journalstatus shouldBe JournalStatus.JOURNALFOERT
            journalpost.journalpostId shouldBe journalpostIdFraJson.toString()
            journalpost.hentDatoJournalfort() shouldBe LocalDate.of(2010, 3, 8)
            journalpost.hentDatoRegistrert() shouldBe LocalDate.of(2010, 3, 12)
            journalpost.dokumenter shouldHaveSize 1
            journalpost.dokumenter[0].tittel shouldBe "Å være eller ikke være..."
        }
    }
}
