package no.nav.bidrag.dokument.arkiv;

import no.nav.bidrag.dokument.arkiv.consumer.JournalforingConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BidragDokumentArkivConfig {

  public static final String ISSUER = "isso";

  @Value("${JOARK_URL}")
  private String baseUrl;

  @Bean
  JournalforingConsumer journalforingConsumer() {
    return new JournalforingConsumer(baseUrl);
  }
}
