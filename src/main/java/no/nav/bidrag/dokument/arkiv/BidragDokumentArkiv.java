package no.nav.bidrag.dokument.arkiv;

import no.nav.bidrag.dokument.arkiv.consumer.JournalforingConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@SpringBootApplication @PropertySource("classpath:url.properties")
public class BidragDokumentArkiv extends WebMvcConfigurationSupport {

    private @Value("${JOARK_URL}") String baseUrl;

    @Bean JournalforingConsumer journalforingConsumer() {
        return new JournalforingConsumer(baseUrl);
    }

    public static void main(String[] args) {
        SpringApplication.run(BidragDokumentArkiv.class, args);
    }
}
