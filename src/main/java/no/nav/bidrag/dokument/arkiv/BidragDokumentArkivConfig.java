package no.nav.bidrag.dokument.arkiv;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
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
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivKnyttTilSakConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokdistFordelingConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.OppgaveConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager;
import no.nav.bidrag.dokument.arkiv.service.EndreJournalpostService;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.bidrag.dokument.arkiv.service.OppgaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableSecurityConfiguration
@EnableRetry
@OpenAPIDefinition(
    info = @Info(title = "bidrag-dokument-arkiv", version = "v1"),
    security = @SecurityRequirement(name = "bearer-key")
)
@SecurityScheme(
    bearerFormat = "JWT",
    name = "bearer-key",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP
)
public class BidragDokumentArkivConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(BidragDokumentArkivConfig.class);

  public static final String ISSUER_ISSO = "isso";
  public static final String ISSUER_STS = "sts";
  public static final String PROFILE_LIVE = "live";
  public static final String PROFILE_KAFKA_TEST = "kafka_test";
  public static final String PROFILE_TEST = "test";

  @Bean
  @Scope("prototype")
  public SafConsumer baseSafConsumer(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      EnvironmentProperties environmentProperties
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.safUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);
    return new SafConsumer(httpHeaderRestTemplate);
  }

  @Bean
  @Scope("prototype")
  public OppgaveConsumer baseOppgaveConsumer(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      EnvironmentProperties environmentProperties
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.oppgaveUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);
    return new OppgaveConsumer(httpHeaderRestTemplate);
  }

  @Bean
  @Scope("prototype")
  public DokarkivConsumer baseDokarkivConsumer(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      EnvironmentProperties environmentProperties,
      ObjectMapper objectMapper
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.dokarkivUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);
    return new DokarkivConsumer(httpHeaderRestTemplate, objectMapper);
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
  public OppgaveService oppgaveService(
      ResourceByDiscriminator<PersonConsumer> personConsumers,
      ResourceByDiscriminator<OppgaveConsumer> oppgaveConsumers,
      SaksbehandlerInfoManager saksbehandlerInfoManager
  ) {
    return new OppgaveService(
        personConsumers,
        oppgaveConsumers,
        saksbehandlerInfoManager);
  }

  @Bean
  public EndreJournalpostService endreJournalpostService(
      ResourceByDiscriminator<JournalpostService> journalpostServices,
      ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers,
      DokarkivKnyttTilSakConsumer dokarkivKnyttTilSakConsumer,
      HendelserProducer hendelserProducer,
      SaksbehandlerInfoManager saksbehandlerInfoManager
  ) {
    return new EndreJournalpostService(
        journalpostServices.get(Discriminator.REGULAR_USER),
        dokarkivConsumers.get(Discriminator.REGULAR_USER),
        dokarkivKnyttTilSakConsumer, hendelserProducer, saksbehandlerInfoManager);
  }

  @Bean
  public ResourceByDiscriminator<JournalpostService> journalpostServices(
      ResourceByDiscriminator<SafConsumer> safConsumers,
      ResourceByDiscriminator<PersonConsumer> personConsumers
  ) {
    var journalpostServiceRegularUser = new JournalpostService(
        safConsumers.get(Discriminator.REGULAR_USER),
        personConsumers.get(Discriminator.REGULAR_USER));
    var journalpostServiceServiceUser = new JournalpostService(
        safConsumers.get(Discriminator.SERVICE_USER),
        personConsumers.get(Discriminator.SERVICE_USER));
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
    safConsumerRegularUser.leggTilInterceptor(securityTokenService.authTokenInterceptor("saf"));
    safConsumerServiceUser.leggTilInterceptor(securityTokenService.serviceUserAuthTokenInterceptor("saf"));
    var safConsumers = new HashMap<Discriminator, SafConsumer>();
    safConsumers.put(Discriminator.REGULAR_USER, safConsumerRegularUser);
    safConsumers.put(Discriminator.SERVICE_USER, safConsumerServiceUser);
    return new ResourceByDiscriminator<>(safConsumers);
  }

  @Bean
  public ResourceByDiscriminator<OppgaveConsumer> oppgaveConsumers(
      OppgaveConsumer oppgaveConsumerRegularUser,
      OppgaveConsumer oppgaveConsumerServiceUser,
      SecurityTokenService securityTokenService
  ) {
    oppgaveConsumerRegularUser.leggTilInterceptor(securityTokenService.authTokenInterceptor("oppgave"));
    oppgaveConsumerServiceUser.leggTilInterceptor(securityTokenService.serviceUserAuthTokenInterceptor("oppgave"));
    var safConsumers = new HashMap<Discriminator, OppgaveConsumer>();
    safConsumers.put(Discriminator.REGULAR_USER, oppgaveConsumerRegularUser);
    safConsumers.put(Discriminator.SERVICE_USER, oppgaveConsumerServiceUser);
    return new ResourceByDiscriminator<>(safConsumers);
  }

  @Bean
  public ResourceByDiscriminator<PersonConsumer> personConsumers(
      PersonConsumer personConsumerRegularUser,
      PersonConsumer personConsumerServiceUser,
      SecurityTokenService securityTokenService
  ) {
    personConsumerRegularUser.leggTilInterceptor(securityTokenService.authTokenInterceptor("bidrag-person"));
    personConsumerServiceUser.leggTilInterceptor(securityTokenService.serviceUserAuthTokenInterceptor("bidrag-person"));
    var personConsumers = new HashMap<Discriminator, PersonConsumer>();
    personConsumers.put(Discriminator.REGULAR_USER, personConsumerRegularUser);
    personConsumers.put(Discriminator.SERVICE_USER, personConsumerServiceUser);
    return new ResourceByDiscriminator<>(personConsumers);
  }

  @Bean
  public DokarkivKnyttTilSakConsumer dokarkivKnyttTilSakConsumer(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      EnvironmentProperties environmentProperties,
      SecurityTokenService securityTokenService
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.dokarkivUrl));
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);

    DokarkivKnyttTilSakConsumer dokarkivKnyttTilSakConsumer = new DokarkivKnyttTilSakConsumer(httpHeaderRestTemplate);
    dokarkivKnyttTilSakConsumer.leggTilInterceptor(securityTokenService.authTokenInterceptor("dokarkiv"));
    dokarkivKnyttTilSakConsumer.leggTilInterceptor(securityTokenService.navConsumerTokenInterceptor(true));
    return dokarkivKnyttTilSakConsumer;
  }

  @Bean
  public ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers(
      DokarkivConsumer dokarkivConsumerRegularUser,
      DokarkivConsumer dokarkivConsumerServiceUser,
      SecurityTokenService securityTokenService
  ) {
    dokarkivConsumerRegularUser.leggTilInterceptor(securityTokenService.authTokenInterceptor("dokarkiv"));
    dokarkivConsumerRegularUser.leggTilInterceptor(securityTokenService.navConsumerTokenInterceptor(true));
    dokarkivConsumerServiceUser.leggTilInterceptor(securityTokenService.serviceUserAuthTokenInterceptor("dokarkiv"));
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
    httpHeaderRestTemplate.getInterceptors().add(securityTokenService.serviceUserAuthTokenInterceptor("bidrag-organisasjon"));
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
      @Value("${BIDRAG_PERSON_URL}") String bidragPersonUrl,
      @Value("${SAF_URL}") String safUrl,
      @Value("${OPPGAVE_URL}") String oppgaveUrl,
      @Value("${SRV_BD_ARKIV_AUTH}") String secretForServiceUser,
      @Value("${ACCESS_TOKEN_URL}") String securityTokenUrl,
      @Value("${BIDRAG_ORGANISASJON_URL}") String bidragOrganisasjonUrl,
      @Value("${NAIS_APP_NAME}") String naisAppName
  ) {
    var environmentProperties = new EnvironmentProperties(dokdistFordelingUrl, dokarkivUrl, safUrl, oppgaveUrl,
        secretForServiceUser, securityTokenUrl,
        naisAppName, bidragPersonUrl, bidragOrganisasjonUrl);
    LOGGER.info(String.format("> Environment: %s", environmentProperties));

    return environmentProperties;
  }

  public static class EnvironmentProperties {

    public final String dokarkivUrl;
    public final String dokdistFordelingUrl;
    public final String bidragPersonUrl;
    public final String safUrl;
    public final String oppgaveUrl;
    public final String secretForServiceUser;
    public final String securityTokenUrl;
    public final String bidragOrganisasjonUrl;
    public final String naisAppName;

    public EnvironmentProperties(
        String dokdistFordelingUrl,
        String dokarkivUrl, String safUrl, String oppgaveUrl, String secretForServiceUser,
        String securityTokenUrl, String naisAppName, String bidragPersonUrl, String bidragOrganisasjonUrl
    ) {
      this.dokdistFordelingUrl = dokdistFordelingUrl;
      this.oppgaveUrl = oppgaveUrl;
      this.bidragPersonUrl = bidragPersonUrl;
      this.dokarkivUrl = dokarkivUrl;
      this.safUrl = safUrl;
      this.secretForServiceUser = secretForServiceUser;
      this.securityTokenUrl = securityTokenUrl;
      this.naisAppName = naisAppName;
      this.bidragOrganisasjonUrl = bidragOrganisasjonUrl;
    }

    @Override
    public String toString() {
      return "dokarkivUrl='" + dokarkivUrl + '\'' +
          ", safUrl='" + safUrl + '\'' +
          ", bidragPersonUrl='" + bidragPersonUrl + '\'' +
          ", securityTokenUrl='" + securityTokenUrl + '\'' +
          ", bidragOrganisasjonUrl='" + bidragOrganisasjonUrl + '\'' +
          ", naisAppName='" + naisAppName + '\'' +
          ", secretForServiceUser '" + notActualValue() + "'.";
    }

    private String notActualValue() {
      return "No authentication available".equals(secretForServiceUser) ? "is not initialized" : "seems to be initialized by init_srvbdarkiv.sh";
    }
  }
}
