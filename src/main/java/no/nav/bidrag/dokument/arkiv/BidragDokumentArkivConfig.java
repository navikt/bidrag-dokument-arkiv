package no.nav.bidrag.dokument.arkiv;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import no.nav.bidrag.dokument.arkiv.consumer.JournalforingConsumer;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class BidragDokumentArkivConfig {
	
	public static final String ISSUER = "isso";
	
    private @Value("${JOARK_URL}") String baseUrl;

    @Bean JournalforingConsumer journalforingConsumer() {
        return new JournalforingConsumer(baseUrl);
    }

    @Bean public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage(BidragDokumentArkiv.class.getPackage().getName()))
                .build();
    }
}
