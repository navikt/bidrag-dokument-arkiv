package no.nav.bidrag.dokument.arkiv.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivTest
import no.nav.bidrag.dokument.arkiv.stubs.Stubs
import no.nav.bidrag.dokument.arkiv.stubs.X_ENHET_HEADER
import no.nav.bidrag.domene.ident.AktørId
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(BidragDokumentArkivConfig.PROFILE_TEST, BidragDokumentArkivTest.PROFILE_INTEGRATION)
@DisplayName("JournalpostController")
@SpringBootTest(
    classes = [BidragDokumentArkivTest::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
abstract class AbstractControllerTest {
    protected var PERSON_IDENT = Personident("12345678910")
    protected var AKTOR_IDENT = AktørId("92345678910")
    protected var responseJournalpostJson = "journalpostSafResponse.json"
    protected var responseJournalpostJsonUtgaaende = "journalpostSafUtgaaendeResponse.json"
    protected var responseJournalpostJsonWithReturDetaljer =
        "journalpostSafReturDetaljerResponse.json"
    protected var responseJournalpostJsonWithAdresse = "journalpostSafAdresseResponse.json"
    protected var journalpostSafNotFoundResponse = "journalpostSafNotFoundResponse.json"
    protected var journalpostJournalfortSafResponse = "journalpostJournalfortSafResponse.json"

    @LocalServerPort
    protected var port = 0

    @Value("\${server.servlet.context-path}")
    protected var contextPath: String? = null

    @Autowired
    lateinit var httpHeaderTestRestTemplate: HttpHeaderTestRestTemplate

    var stubs: Stubs = Stubs()

    @MockBean
    lateinit var kafkaTemplateMock: KafkaTemplate<String, String>

    @Value("\${TOPIC_JOURNALPOST}")
    lateinit var topicJournalpost: String

    @Autowired
    lateinit var objectMapper: ObjectMapper

    var headerMedEnhet = HttpHeaders()

    @BeforeEach
    fun initMocks() {
        headerMedEnhet = HttpHeaders()
        headerMedEnhet.add(EnhetFilter.X_ENHET_HEADER, X_ENHET_HEADER)
        stubs!!.mockOppdaterOppgave(HttpStatus.OK)
        stubs!!.mockOpprettOppgave(HttpStatus.OK)
        stubs!!.mockSokOppgave()
        stubs!!.mockSts()
        stubs.mockPersonResponse(PersonDto(PERSON_IDENT), HttpStatus.OK)
        stubs!!.mockBidragOrganisasjonSaksbehandler()
    }

    @AfterEach
    fun resetMocks() {
        WireMock.reset()
        WireMock.resetToDefault()
    }

    protected fun initUrl(): String {
        return "http://localhost:$port$contextPath"
    }
}
