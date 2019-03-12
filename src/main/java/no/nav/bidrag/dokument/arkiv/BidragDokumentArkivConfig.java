package no.nav.bidrag.dokument.arkiv;

import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.dokument.arkiv.consumer.JournalforingConsumer;
import no.nav.security.oidc.context.OIDCRequestContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BidragDokumentArkivConfig {

  public static final String ISSUER = "isso";

  @Value("${JOARK_URL}")
  private String baseUrl;

  @Bean
  JournalforingConsumer journalforingConsumer(RestTemplate restTemplate, OIDCRequestContextHolder oidcRequestContextHolder) {
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    return new JournalforingConsumer(oidcRequestContextHolder, restTemplate);
  }

  @Bean
  public CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }
}
