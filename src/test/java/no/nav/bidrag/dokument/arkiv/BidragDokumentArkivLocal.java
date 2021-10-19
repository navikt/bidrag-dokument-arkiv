package no.nav.bidrag.dokument.arkiv;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootApplication
@PropertySource("classpath:resources.properties")
@EnableJwtTokenValidation(ignore = {"springfox.documentation.swagger.web.ApiResourceController"})
@Import(TokenGeneratorConfiguration.class)
@ComponentScan(excludeFilters = {@Filter(type = ASSIGNABLE_TYPE, value = BidragDokumentArkiv.class)})
@EmbeddedKafka
public class BidragDokumentArkivLocal {

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(BidragDokumentArkivLocal.class);
    app.setAdditionalProfiles(PROFILE_TEST);
    app.run(args);
  }
}
