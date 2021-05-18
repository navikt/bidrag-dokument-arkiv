package no.nav.bidrag.dokument.arkiv;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_LIVE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BidragDokumentArkiv {

  public static void main(String... args) {

    String profile = args.length < 1 ? PROFILE_LIVE : args[0];

    SpringApplication app = new SpringApplication(BidragDokumentArkiv.class);
    app.setAdditionalProfiles(profile);
    app.run(args);
  }
}
