package no.nav.bidrag.dokument.arkiv

import com.nimbusds.jose.JOSEObjectType
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import java.util.Map

@Configuration
@Profile(BidragDokumentArkivTest.PROFILE_INTEGRATION)
class TestRestTemplateConfiguration {
    @Autowired
    private val mockOAuth2Server: MockOAuth2Server? = null

    @Bean
    fun httpHeaderTestRestTemplate(): TestRestTemplate = TestRestTemplate(
        RestTemplateBuilder()
            .additionalInterceptors({ request, body, execution ->
                request.headers.add(HttpHeaders.AUTHORIZATION, generateBearerToken())
                execution.execute(request, body)
            }),
    )

    private fun generateBearerToken(): String {
        val iss = mockOAuth2Server!!.issuerUrl("aad")
        val newIssuer = iss.newBuilder().host("localhost").build()
        val token = mockOAuth2Server.issueToken(
            "aad",
            "aud-localhost",
            DefaultOAuth2TokenCallback(
                "aad",
                "aud-localhost",
                JOSEObjectType.JWT.getType(),
                listOf<String>("aud-localhost"),
                mapOf("iss" to newIssuer.toString()),
                3600,
            ),
        )
        return "Bearer " + token.serialize()
    }
}
