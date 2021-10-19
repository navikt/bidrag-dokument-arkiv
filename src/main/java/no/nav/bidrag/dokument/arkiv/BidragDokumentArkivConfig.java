package no.nav.bidrag.dokument.arkiv;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.util.HashMap;
import java.util.Optional;
import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.aop.AspectExceptionLogger;
import no.nav.bidrag.dokument.arkiv.aop.HttpStatusRestControllerAdvice;
import no.nav.bidrag.dokument.arkiv.consumer.AccessTokenConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.arkiv.security.OidcTokenGenerator;
import no.nav.bidrag.dokument.arkiv.security.TokenForBasicAuthenticationGenerator;
import no.nav.bidrag.dokument.arkiv.service.JournalpostService;
import no.nav.bidrag.tilgangskontroll.SecurityUtils;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Configuration
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
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

    public static final String ISSUER = "isso";
    public static final String PROFILE_LIVE = "live";
    public static final String PROFILE_TEST = "test";

    @Bean
    @Scope("prototype")
    SafConsumer baseSafConsumer(
            @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
            EnvironmentProperties environmentProperties
    ) {
        httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.safQraphiQlUrl));
        httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);
        return new SafConsumer(httpHeaderRestTemplate);
    }

    @Bean
    ResourceByDiscriminator<JournalpostService> journalpostServices(
        ResourceByDiscriminator<SafConsumer> safConsumers,
        ResourceByDiscriminator<PersonConsumer> personConsumers,
        DokarkivConsumer dokarkivConsumer
    ) {
        var journalpostServiceRegularUser = new JournalpostService(safConsumers.get(Discriminator.REGULAR_USER), personConsumers.get(Discriminator.REGULAR_USER), dokarkivConsumer);
        var journalpostServiceServiceUser = new JournalpostService(safConsumers.get(Discriminator.SERVICE_USER), personConsumers.get(Discriminator.SERVICE_USER), dokarkivConsumer);
        var journalpostServices = new HashMap<Discriminator, JournalpostService>();
        journalpostServices.put(Discriminator.REGULAR_USER, journalpostServiceRegularUser);
        journalpostServices.put(Discriminator.SERVICE_USER, journalpostServiceServiceUser);
        return new ResourceByDiscriminator<>(journalpostServices);
    }

    @Bean
    ResourceByDiscriminator<SafConsumer> safConsumers(
            SafConsumer safConsumerRegularUser,
            SafConsumer safConsumerServiceUser,
            OidcTokenGenerator oidcTokenGenerator,
            TokenForBasicAuthenticationGenerator tokenForBasicAuthenticationGenerator
    ) {
        safConsumerRegularUser.leggTilSikkerhet(oidcTokenGenerator::fetchBearerToken);
        safConsumerServiceUser.leggTilSikkerhet(tokenForBasicAuthenticationGenerator::generateToken);
        var safConsumers = new HashMap<Discriminator, SafConsumer>();
        safConsumers.put(Discriminator.REGULAR_USER, safConsumerRegularUser);
        safConsumers.put(Discriminator.SERVICE_USER, safConsumerServiceUser);
        return new ResourceByDiscriminator<>(safConsumers);
    }

    @Bean
    @Scope("prototype")
    PersonConsumer basePersonConsumer(
        @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
        EnvironmentProperties environmentProperties
    ) {
        httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.bidragPersonUrl +"/bidrag-person"));
        return new PersonConsumer(httpHeaderRestTemplate);
    }

    @Bean
    ResourceByDiscriminator<PersonConsumer> personConsumers(
        PersonConsumer personConsumerRegularUser,
        PersonConsumer personConsumerServiceUser,
        OidcTokenGenerator oidcTokenGenerator,
        TokenForBasicAuthenticationGenerator tokenForBasicAuthenticationGenerator
    ) {
        personConsumerRegularUser.leggTilSikkerhet(oidcTokenGenerator::fetchBearerToken);
        personConsumerServiceUser.leggTilSikkerhet(tokenForBasicAuthenticationGenerator::generateToken);
        var personConsumers = new HashMap<Discriminator, PersonConsumer>();
        personConsumers.put(Discriminator.REGULAR_USER, personConsumerRegularUser);
        personConsumers.put(Discriminator.SERVICE_USER, personConsumerServiceUser);
        return new ResourceByDiscriminator<>(personConsumers);
    }

    @Bean
    DokarkivConsumer dokarkivConsumer(
            @Qualifier("dokarkiv") HttpHeaderRestTemplate httpHeaderRestTemplate,
            EnvironmentProperties environmentProperties
    ) {
        httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.dokarkivUrl));

        return new DokarkivConsumer(httpHeaderRestTemplate);
    }

    @Bean
    AccessTokenConsumer accessTokenConsumer(
            @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
            EnvironmentProperties environmentProperties
    ) {
        httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(environmentProperties.securityTokenUrl));
        httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        return new AccessTokenConsumer(httpHeaderRestTemplate);
    }

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    ExceptionLogger exceptionLogger() {
        return new ExceptionLogger(
                BidragDokumentArkiv.class.getSimpleName(), AspectExceptionLogger.class, HttpStatusRestControllerAdvice.class
        );
    }

    @Bean
    TokenForBasicAuthenticationGenerator basicAuthenticationTokenGenerator(
            AccessTokenConsumer accessTokenConsumer,
            EnvironmentProperties environmentProperties
    ) {
        return new TokenForBasicAuthenticationGenerator(accessTokenConsumer, environmentProperties.secretForServiceUser);
    }

    @Bean
    OidcTokenGenerator oidcTokenGenerator(TokenValidationContextHolder tokenValidationContextHolder) {
        return new OidcTokenGenerator(tokenValidationContextHolder);
    }

    @Bean
    EnvironmentProperties environmentProperties(
            @Value("${DOKARKIV_URL}") String dokarkivUrl,
            @Value("${BIDRAG_PERSON_URL}") String bidragPersonUrl,
            @Value("${SAF_GRAPHQL_URL}") String safQraphiQlUrl,
            @Value("${SRV_BD_ARKIV_AUTH}") String secretForServiceUser,
            @Value("${ACCESS_TOKEN_URL}") String securityTokenUrl,
            @Value("${NAIS_APP_NAME}") String naisAppName
    ) {
        var environmentProperties = new EnvironmentProperties(dokarkivUrl, safQraphiQlUrl, secretForServiceUser, securityTokenUrl, naisAppName, bidragPersonUrl);
        LOGGER.info(String.format("> Environment: %s", environmentProperties));

        return environmentProperties;
    }

    @Bean
    public OidcTokenManager oidcTokenManager(TokenValidationContextHolder tokenValidationContextHolder) {
        return () -> Optional.ofNullable(tokenValidationContextHolder)
            .map(TokenValidationContextHolder::getTokenValidationContext)
            .map(TokenValidationContext::getFirstValidToken).flatMap(token -> token.map(JwtToken::getTokenAsString));
    }

    @Bean
    SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManager(OidcTokenManager oidcTokenManager) {
        return () -> oidcTokenManager.hentIdToken().map(SecurityUtils::henteSubject).map((subject)-> new Saksbehandler(subject, "")).orElse(null);
    }

    static class EnvironmentProperties {

        final String dokarkivUrl;
        final String bidragPersonUrl;
        final String safQraphiQlUrl;
        final String secretForServiceUser;
        final String securityTokenUrl;
        final String naisAppName;

        public EnvironmentProperties(String dokarkivUrl, String safQraphiQlUrl, String secretForServiceUser, String securityTokenUrl, String naisAppName, String bidragPersonUrl) {
            this.bidragPersonUrl = bidragPersonUrl;
            this.dokarkivUrl = dokarkivUrl;
            this.safQraphiQlUrl = safQraphiQlUrl;
            this.secretForServiceUser = secretForServiceUser;
            this.securityTokenUrl = securityTokenUrl;
            this.naisAppName = naisAppName;
        }

        @Override
        public String toString() {
            return "dokarkivUrl='" + dokarkivUrl + '\'' +
                    ", safQraphiQlUrl='" + safQraphiQlUrl + '\'' +
                    ", bidragPersonUrl='" + bidragPersonUrl + '\'' +
                    ", securityTokenUrl='" + securityTokenUrl + '\'' +
                    ", naisAppName='" + naisAppName + '\'' +
                    ", secretForServiceUser '" + notActualValue() + "'.";
        }

        private String notActualValue() {
            return "No authentication available".equals(secretForServiceUser) ? "is not initialized" : "seems to be initialized by init_srvbdarkiv.sh";
        }
    }

    @FunctionalInterface
    public interface OidcTokenManager {

        Optional<String> hentIdToken();
    }

    @FunctionalInterface
    public interface SaksbehandlerOidcTokenManager {

        Saksbehandler hentSaksbehandler();
    }
}
