package no.nav.bidrag.dokument.arkiv;

import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import no.nav.security.oidc.test.support.spring.TokenGeneratorConfiguration;
import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication @PropertySource("classpath:url.properties")
@EnableOIDCTokenValidation(ignore = {"springfox.documentation.swagger.web.ApiResourceController", "org.springframework"})
@Import(TokenGeneratorConfiguration.class)
@ComponentScan(excludeFilters = { @Filter(type = ASSIGNABLE_TYPE, value = BidragDokumentArkiv.class) })
public class BidragDokumentArkivLocal  {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BidragDokumentArkivLocal.class);
        app.setAdditionalProfiles("dev");
        app.run(args);
    }
}
