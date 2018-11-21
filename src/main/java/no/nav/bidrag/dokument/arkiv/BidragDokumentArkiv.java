package no.nav.bidrag.dokument.arkiv;

import no.nav.bidrag.dokument.arkiv.consumer.JournalforingConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication @PropertySource("classpath:url.properties")
@EnableSwagger2
public class BidragDokumentArkiv extends WebMvcConfigurationSupport {

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

    @Override protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    public static void main(String[] args) {
        SpringApplication.run(BidragDokumentArkiv.class, args);
    }
}
