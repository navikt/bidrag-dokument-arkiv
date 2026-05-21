package no.nav.bidrag.dokument.arkiv.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.YearMonthDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Configuration
class RestConfig : WebMvcConfigurer {

    override fun configureMessageConverters(converters: HttpMessageConverters.ServerBuilder) {
        converters.addCustomConverter(
            CustomJacksonHttpMessageConverter(
                commonObjectmapper
                    .registerModules(
                        KotlinModule.Builder().build(),
                        JavaTimeModule()
                            .addDeserializer(
                                YearMonth::class.java,
                                // Denne trengs for å parse år over 9999 riktig.
                                YearMonthDeserializer(DateTimeFormatter.ofPattern("u-MM")),
                            ).addSerializer(
                                LocalDate::class.java,
                                // Denne trengs for å skrive ut år over 9999 riktig.
                                LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            ),
                    ),
            ),
        )
    }
}
