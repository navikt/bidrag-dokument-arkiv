package no.nav.bidrag.dokument.arkiv

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@EnableJwtTokenValidation(ignore = ["springfox.documentation.swagger.web.ApiResourceController"])
class BidragDokumentArkivTest {

    companion object {
        const val PROFILE_INTEGRATION: String = "integration"
    }
    fun main(args: Array<String>) {
        val app: SpringApplication = SpringApplication(BidragDokumentArkivTest::class.java)
        app.setAdditionalProfiles(BidragDokumentArkivConfig.PROFILE_KAFKA_TEST, "local")
        app.run(*args)
    }
}
