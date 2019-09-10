package no.nav.bidrag.dokument.arkiv;

import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.GraphQueryConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class BidragDokumentArkivConfig {

  public static final String ISSUER = "isso";

  @Bean
  GraphQueryConsumer graphQueryConsumer(GraphQueryConfiguration graphQueryConfiguration) {
    return new GraphQueryConsumer(graphQueryConfiguration.getHttpHeaderRestTemplate());
  }

  @Bean
  DokarkivConsumer dokarkivConsumer(HttpHeaderRestTemplate httpHeaderRestTemplate, @Value("${DOKARKIV_URL}") String baseUrl) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    return new DokarkivConsumer(httpHeaderRestTemplate);
  }

  @Bean
  GraphQueryConfiguration graphQueryConfiguration(HttpHeaderRestTemplate httpHeaderRestTemplate, @Value("${SAF_GRAPHQL_URL}") String baseUrl) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> "application/json");

    return new GraphQueryConfiguration(httpHeaderRestTemplate, baseUrl);
  }

  @Bean
  CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Bean
  ExceptionLogger exceptionLogger() {
    return new ExceptionLogger(BidragDokumentArkiv.class.getSimpleName());
  }

  public static class GraphQueryConfiguration {

    private final HttpHeaderRestTemplate httpHeaderRestTemplate;

    public GraphQueryConfiguration(HttpHeaderRestTemplate httpHeaderRestTemplate, String baseUrl) {
      this.httpHeaderRestTemplate = httpHeaderRestTemplate;
      httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    }

    public HttpHeaderRestTemplate getHttpHeaderRestTemplate() {
      return httpHeaderRestTemplate;
    }
  }
}
