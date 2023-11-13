package no.nav.bidrag.dokument.arkiv

import mu.KLogger
import mu.KotlinLogging
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

@JvmField
val SECURE_LOGGER: KLogger = KotlinLogging.logger("secureLogger")

@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class])
class BidragDokumentArkiv

fun main(args: Array<String>) {
    val profile = if (args.isEmpty()) BidragDokumentArkivConfig.PROFILE_LIVE else args[0]
    val app = SpringApplication(BidragDokumentArkiv::class.java)
    app.setAdditionalProfiles(profile)
    app.run(*args)
}
