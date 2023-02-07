package no.nav.bidrag.dokument.arkiv;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_KAFKA_TEST;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class })
@EnableJwtTokenValidation(ignore = {"org.springframework", "org.springdoc"})
public class BidragDokumentArkivLokalNais {
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(BidragDokumentArkivLokalNais.class);
    app.setAdditionalProfiles(PROFILE_KAFKA_TEST, "live", "lokal", "lokal-nais", "lokal-nais-secrets");
    app.run(args);
  }
}
