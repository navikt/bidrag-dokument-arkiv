package no.nav.bidrag.dokument.arkiv;

import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.consumer.AccessTokenConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.GraphQueryConsumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class BidragDokumentArkivConfig {

  public static final String ISSUER = "isso";
  public static final String PROFILE_LIVE = "live";
  public static final String PROFILE_TEST = "test";

  @Bean
  GraphQueryConsumer graphQueryConsumer(
      @Qualifier("saf") HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Value("${SAF_GRAPHQL_URL}") String baseUrl
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> "application/json");

    return new GraphQueryConsumer(httpHeaderRestTemplate);
  }

  @Bean
  DokarkivConsumer dokarkivConsumer(
      @Qualifier("dokarkiv") HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Value("${DOKARKIV_URL}") String baseUrl
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));

    return new DokarkivConsumer(httpHeaderRestTemplate);
  }

  @Bean
  AccessTokenConsumer accessTokenConsumer(
      @Qualifier("token") HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Value("${ACCESS_TOKEN_URL}") String baseUrl
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));

    return new AccessTokenConsumer(httpHeaderRestTemplate);
  }

  @Bean
  CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Bean
  ExceptionLogger exceptionLogger() {
    return new ExceptionLogger(BidragDokumentArkiv.class.getSimpleName());
  }
}
