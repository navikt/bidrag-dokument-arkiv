package no.nav.bidrag.dokument.arkiv.config

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Configuration
class RestConfig {

    @Bean
    fun jackson2ObjectMapperBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer {
            it.failOnUnknownProperties(false)
            it.failOnEmptyBeans(false)
            it.modulesToInstall(
                JavaTimeModule()
                    .addDeserializer(
                        YearMonth::class.java,
                        YearMonthDeserializer(DateTimeFormatter.ofPattern("u-MM")) // Denne trengs for å parse år over 9999 riktig.
                    )
            )
        }


}