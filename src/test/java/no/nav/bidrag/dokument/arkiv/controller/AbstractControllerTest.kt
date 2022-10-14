package no.nav.bidrag.dokument.arkiv.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.context.ActiveProfiles
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.beans.factory.annotation.Autowired
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.kafka.core.KafkaTemplate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.dokument.arkiv.stubs.Stubs
import no.nav.bidrag.dokument.arkiv.stubs.X_ENHET_HEADER
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus

@ActiveProfiles(BidragDokumentArkivConfig.PROFILE_TEST, BidragDokumentArkivTest.PROFILE_INTEGRATION)
@DisplayName("JournalpostController")
@SpringBootTest(classes = [BidragDokumentArkivTest::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
abstract class AbstractControllerTest {
    @JvmField
    protected var PERSON_IDENT = "12345678910"
    @JvmField
    protected var AKTOR_IDENT = "92345678910"
    @JvmField
    protected var responseJournalpostJson = "journalpostSafResponse.json"
    protected var responseJournalpostJsonUtgaaende = "journalpostSafUtgaaendeResponse.json"
    @JvmField
    protected var responseJournalpostJsonWithReturDetaljer = "journalpostSafReturDetaljerResponse.json"
    @JvmField
    protected var responseJournalpostJsonWithAdresse = "journalpostSafAdresseResponse.json"
    @JvmField
    protected var journalpostSafNotFoundResponse = "journalpostSafNotFoundResponse.json"
    @JvmField
    protected var journalpostJournalfortSafResponse = "journalpostJournalfortSafResponse.json"

    @JvmField
    @LocalServerPort
    protected var port = 0

    @Value("\${server.servlet.context-path}")
    protected var contextPath: String? = null

    @Autowired
    lateinit var httpHeaderTestRestTemplate: HttpHeaderTestRestTemplate

    @Autowired
    lateinit var stubs: Stubs

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