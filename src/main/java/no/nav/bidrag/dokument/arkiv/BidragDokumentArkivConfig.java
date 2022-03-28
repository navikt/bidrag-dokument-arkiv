package no.nav.bidrag.dokument.arkiv;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration;
import no.nav.bidrag.commons.security.service.SecurityTokenService;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.aop.AspectExceptionLogger;
import no.nav.bidrag.dokument.arkiv.aop.HttpStatusRestControllerAdvice;
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivProxyConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokdistFordelingConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Configuration
@EnableSecurityConfiguration
public class BidragDokumentArkivConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(BidragDokumentArkivConfig.class);

  public static final String ISSUER_ISSO = "isso";
  public static final String ISSUER_STS = "sts";
  public static final String PROFILE_LIVE = "live";
  public static final String PROFILE_TEST = "test";

  @Bean
  @Scope("prototype")
  public SafConsumer baseSafConsumer(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      EnvironmentProperties environmentProperties
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.safQraphiQlUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);
    return new SafConsumer(httpHeaderRestTemplate);
  }

  @Bean
  @Scope("prototype")
  public DokarkivConsumer baseDokarkivConsumer(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      EnvironmentProperties environmentProperties
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.dokarkivUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);
    return new DokarkivConsumer(httpHeaderRestTemplate);
  }

  @Bean
  @Scope("prototype")
  public DokdistFordelingConsumer dokdistFordelingConsumer(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      EnvironmentProperties environmentProperties,
      ObjectMapper objectMapper,
      SecurityTokenService securityTokenService
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.dokdistFordelingUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);
    DokdistFordelingConsumer dokdistFordelingConsumer = new DokdistFordelingConsumer(httpHeaderRestTemplate, objectMapper);
    dokdistFordelingConsumer.leggTilInterceptor(securityTokenService.serviceUserAuthTokenInterceptor());
    return dokdistFordelingConsumer;
  }

  @Bean
  @Scope("prototype")
  PersonConsumer basePersonConsumer(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      EnvironmentProperties environmentProperties
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.bidragPersonUrl + "/bidrag-person"));
    return new PersonConsumer(httpHeaderRestTemplate);
  }

  @Bean
  public ResourceByDiscriminator<JournalpostService> journalpostServices(
      ResourceByDiscriminator<SafConsumer> safConsumers,
      ResourceByDiscriminator<PersonConsumer> personConsumers,
      ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers,
      DokarkivProxyConsumer dokarkivProxyConsumer,
      @Lazy HendelserProducer hendelserProducer
  ) {
    var journalpostServiceRegularUser = new JournalpostService(
        safConsumers.get(Discriminator.REGULAR_USER),
        personConsumers.get(Discriminator.REGULAR_USER),
        dokarkivConsumers.get(Discriminator.REGULAR_USER),
        dokarkivProxyConsumer,
        hendelserProducer);
    var journalpostServiceServiceUser = new JournalpostService(
        safConsumers.get(Discriminator.SERVICE_USER),
        personConsumers.get(Discriminator.SERVICE_USER),
        dokarkivConsumers.get(Discriminator.SERVICE_USER),
        dokarkivProxyConsumer,
        hendelserProducer);
    var journalpostServices = new HashMap<Discriminator, JournalpostService>();
    journalpostServices.put(Discriminator.REGULAR_USER, journalpostServiceRegularUser);
    journalpostServices.put(Discriminator.SERVICE_USER, journalpostServiceServiceUser);
    return new ResourceByDiscriminator<>(journalpostServices);
  }

  @Bean
  public ResourceByDiscriminator<SafConsumer> safConsumers(
      SafConsumer safConsumerRegularUser,
      SafConsumer safConsumerServiceUser,
      SecurityTokenService securityTokenService
  ) {
    safConsumerRegularUser.leggTilInterceptor(securityTokenService.authTokenInterceptor());
    safConsumerRegularUser.leggTilInterceptor(securityTokenService.serviceUserAuthTokenInterceptor());
    var safConsumers = new HashMap<Discriminator, SafConsumer>();
    safConsumers.put(Discriminator.REGULAR_USER, safConsumerRegularUser);
    safConsumers.put(Discriminator.SERVICE_USER, safConsumerServiceUser);
    return new ResourceByDiscriminator<>(safConsumers);
  }

  @Bean
  public ResourceByDiscriminator<PersonConsumer> personConsumers(
      PersonConsumer personConsumerRegularUser,
      PersonConsumer personConsumerServiceUser,
      SecurityTokenService securityTokenService
  ) {
    personConsumerRegularUser.leggTilInterceptor(securityTokenService.authTokenInterceptor());
    personConsumerRegularUser.leggTilInterceptor(securityTokenService.serviceUserAuthTokenInterceptor());
    var personConsumers = new HashMap<Discriminator, PersonConsumer>();
    personConsumers.put(Discriminator.REGULAR_USER, personConsumerRegularUser);
    personConsumers.put(Discriminator.SERVICE_USER, personConsumerServiceUser);
    return new ResourceByDiscriminator<>(personConsumers);
  }

  @Bean
  public DokarkivProxyConsumer dokarkivProxyConsumer(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      EnvironmentProperties environmentProperties,
      SecurityTokenService securityTokenService
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.dokarkivProxyUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);

    DokarkivProxyConsumer dokarkivProxyConsumer = new DokarkivProxyConsumer(httpHeaderRestTemplate);
    dokarkivProxyConsumer.leggTilInterceptor(securityTokenService.authTokenInterceptor());
    dokarkivProxyConsumer.leggTilInterceptor(securityTokenService.navConsumerTokenInterceptor());
    return dokarkivProxyConsumer;
  }

  @Bean
  public ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers(
      DokarkivConsumer dokarkivConsumerRegularUser,
      DokarkivConsumer dokarkivConsumerServiceUser,
      SecurityTokenService securityTokenService
  ) {
    dokarkivConsumerRegularUser.leggTilInterceptor(securityTokenService.authTokenInterceptor());
    dokarkivConsumerRegularUser.leggTilInterceptor(securityTokenService.navConsumerTokenInterceptor());
    dokarkivConsumerServiceUser.leggTilInterceptor(securityTokenService.serviceUserAuthTokenInterceptor());
    var dokarkivConsumers = new HashMap<Discriminator, DokarkivConsumer>();
    dokarkivConsumers.put(Discriminator.REGULAR_USER, dokarkivConsumerRegularUser);
    dokarkivConsumers.put(Discriminator.SERVICE_USER, dokarkivConsumerServiceUser);
    return new ResourceByDiscriminator<>(dokarkivConsumers);
  }

  @Bean
  public BidragOrganisasjonConsumer bidragOrganisasjonConsumer(
      HttpHeaderRestTemplate httpHeaderRestTemplate,
      SecurityTokenService securityTokenService,
      EnvironmentProperties environmentProperties
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.bidragOrganisasjonUrl + "/bidrag-organisasjon"));
    httpHeaderRestTemplate.getInterceptors().add(securityTokenService.serviceUserAuthTokenInterceptor());
    return new BidragOrganisasjonConsumer(httpHeaderRestTemplate);
  }

  @Bean
  @Order(1)
  public CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Bean
  @Order(2)
  public EnhetFilter enhetFilter() {
    return new EnhetFilter();
  }

  @Bean
  public ExceptionLogger exceptionLogger() {
    return new ExceptionLogger(
        BidragDokumentArkiv.class.getSimpleName(), AspectExceptionLogger.class, HttpStatusRestControllerAdvice.class
    );
  }

  @Bean
  public EnvironmentProperties environmentProperties(
      @Value("${DOKARKIV_URL}") String dokarkivUrl,
      @Value("${DOKDISTFORDELING_URL}") String dokdistFordelingUrl,
      @Value("${DOKARKIV_PROXY_URL}") String dokarkivProxyUrl,
      @Value("${BIDRAG_PERSON_URL}") String bidragPersonUrl,
      @Value("${SAF_GRAPHQL_URL}") String safQraphiQlUrl,
      @Value("${SRV_BD_ARKIV_AUTH}") String secretForServiceUser,
      @Value("${ACCESS_TOKEN_URL}") String securityTokenUrl,
      @Value("${BIDRAG_ORGANISASJON_URL}") String bidragOrganisasjonUrl,
      @Value("${NAIS_APP_NAME}") String naisAppName
  ) {
    var environmentProperties = new EnvironmentProperties(dokdistFordelingUrl, dokarkivUrl, dokarkivProxyUrl, safQraphiQlUrl, secretForServiceUser, securityTokenUrl,
        naisAppName, bidragPersonUrl, bidragOrganisasjonUrl);
    LOGGER.info(String.format("> Environment: %s", environmentProperties));

    return environmentProperties;
  }

  public static class EnvironmentProperties {

    public final String dokarkivUrl;
    public final String dokdistFordelingUrl;
    public final String dokarkivProxyUrl;
    public final String bidragPersonUrl;
    public final String safQraphiQlUrl;
    public final String secretForServiceUser;
    public final String securityTokenUrl;
    public final String bidragOrganisasjonUrl;
    public final String naisAppName;

    public EnvironmentProperties(
        String dokdistFordelingUrl,
        String dokarkivUrl, String dokarkivProxyUrl, String safQraphiQlUrl, String secretForServiceUser,
        String securityTokenUrl, String naisAppName, String bidragPersonUrl, String bidragOrganisasjonUrl
    ) {
      this.dokdistFordelingUrl = dokdistFordelingUrl;
      this.dokarkivProxyUrl = dokarkivProxyUrl;
      this.bidragPersonUrl = bidragPersonUrl;
      this.dokarkivUrl = dokarkivUrl;
      this.safQraphiQlUrl = safQraphiQlUrl;
      this.secretForServiceUser = secretForServiceUser;
      this.securityTokenUrl = securityTokenUrl;
      this.naisAppName = naisAppName;
      this.bidragOrganisasjonUrl = bidragOrganisasjonUrl;
    }

    @Override
    public String toString() {
      return "dokarkivUrl='" + dokarkivUrl + '\'' +
          ", safQraphiQlUrl='" + safQraphiQlUrl + '\'' +
          ", bidragPersonUrl='" + bidragPersonUrl + '\'' +
          ", securityTokenUrl='" + securityTokenUrl + '\'' +
          ", dokarkivProxyUrl='" + dokarkivProxyUrl + '\'' +
          ", bidragOrganisasjonUrl='" + bidragOrganisasjonUrl + '\'' +
          ", naisAppName='" + naisAppName + '\'' +
          ", secretForServiceUser '" + notActualValue() + "'.";
    }

    private String notActualValue() {
      return "No authentication available".equals(secretForServiceUser) ? "is not initialized" : "seems to be initialized by init_srvbdarkiv.sh";
    }
  }
}
