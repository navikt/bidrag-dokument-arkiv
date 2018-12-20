package no.nav.bidrag.dokument.arkiv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation;

@SpringBootApplication @PropertySource("classpath:url.properties")
@EnableOIDCTokenValidation(ignore = {"springfox.documentation.swagger.web.ApiResourceController", "org.springframework"})
public class BidragDokumentArkiv {

    public static void main(String[] args) {
        SpringApplication.run(BidragDokumentArkiv.class, args);
    }
}
