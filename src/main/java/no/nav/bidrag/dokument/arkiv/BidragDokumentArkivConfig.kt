package no.nav.bidrag.dokument.arkiv

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.security.service.StsTokenService
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.DefaultCorsFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.MdcFilter
import no.nav.bidrag.commons.web.UserMdcFilter
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivKnyttTilSakConsumer
import no.nav.bidrag.dokument.arkiv.consumer.DokdistFordelingConsumer
import no.nav.bidrag.dokument.arkiv.consumer.OppgaveConsumer
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager
import no.nav.bidrag.dokument.arkiv.service.EndreJournalpostService
import no.nav.bidrag.dokument.arkiv.service.JournalpostService
import no.nav.bidrag.dokument.arkiv.service.OppgaveService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.retry.annotation.EnableRetry
import org.springframework.web.util.DefaultUriBuilderFactory

@Configuration
@EnableSecurityConfiguration
@EnableRetry
@OpenAPIDefinition(
    info = Info(title = "bidrag-dokument-arkiv", version = "v1"),
    security = [SecurityRequirement(name = "bearer-key")],
)
@SecurityScheme(
    bearerFormat = "JWT",
    name = "bearer-key",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
)
@Import(
    CorrelationIdFilter::class,
    DefaultCorsFilter::class,
    UserMdcFilter::class,
    StsTokenService::class,
    MdcFilter::class,
)
class BidragDokumentArkivConfig {
    @Bean
    fun clientRequestObservationConvention() = DefaultClientRequestObservationConvention()

    @Bean
    @Scope("prototype")
    fun baseSafConsumer(
        @Qualifier("base") httpHeaderRestTemplate: HttpHeaderRestTemplate,
        environmentProperties: EnvironmentProperties,
    ): SafConsumer {
        httpHeaderRestTemplate.uriTemplateHandler =
            DefaultUriBuilderFactory(environmentProperties.safUrl)
        httpHeaderRestTemplate.addHeaderGenerator(
            HttpHeaders.CONTENT_TYPE,
        ) { MediaType.APPLICATION_JSON_VALUE }
        return SafConsumer(httpHeaderRestTemplate)
    }

    @Bean
    @Scope("prototype")
    fun baseOppgaveConsumer(
        @Qualifier("base") httpHeaderRestTemplate: HttpHeaderRestTemplate,
        environmentProperties: EnvironmentProperties,
    ): OppgaveConsumer {
        httpHeaderRestTemplate.uriTemplateHandler =
            DefaultUriBuilderFactory(environmentProperties.oppgaveUrl)
        httpHeaderRestTemplate.addHeaderGenerator(
            HttpHeaders.CONTENT_TYPE,
        ) { MediaType.APPLICATION_JSON_VALUE }
        return OppgaveConsumer(httpHeaderRestTemplate)
    }

    @Bean
    @Scope("prototype")
    fun baseDokarkivConsumer(
        @Qualifier("base") httpHeaderRestTemplate: HttpHeaderRestTemplate,
        environmentProperties: EnvironmentProperties,
        objectMapper: ObjectMapper?,
    ): DokarkivConsumer {
        httpHeaderRestTemplate.uriTemplateHandler =
            DefaultUriBuilderFactory(environmentProperties.dokarkivUrl)
        httpHeaderRestTemplate.addHeaderGenerator(
            HttpHeaders.CONTENT_TYPE,
        ) { MediaType.APPLICATION_JSON_VALUE }
        return DokarkivConsumer(httpHeaderRestTemplate, objectMapper)
    }

    @Bean
    @Scope("prototype")
    fun dokdistFordelingConsumer(
        @Qualifier("base") httpHeaderRestTemplate: HttpHeaderRestTemplate,
        environmentProperties: EnvironmentProperties,
        objectMapper: ObjectMapper?,
        securityTokenService: SecurityTokenService,
    ): DokdistFordelingConsumer {
        httpHeaderRestTemplate.uriTemplateHandler =
            DefaultUriBuilderFactory(environmentProperties.dokdistFordelingUrl)
        httpHeaderRestTemplate.addHeaderGenerator(
            HttpHeaders.CONTENT_TYPE,
        ) { MediaType.APPLICATION_JSON_VALUE }
        val dokdistFordelingConsumer =
            DokdistFordelingConsumer(httpHeaderRestTemplate, objectMapper)
        dokdistFordelingConsumer.leggTilInterceptor(
            securityTokenService.clientCredentialsTokenInterceptor(null),
        )
        return dokdistFordelingConsumer
    }

    @Bean
    @Scope("prototype")
    fun basePersonConsumer(
        @Qualifier("base") httpHeaderRestTemplate: HttpHeaderRestTemplate,
        environmentProperties: EnvironmentProperties,
    ): PersonConsumer {
        httpHeaderRestTemplate.uriTemplateHandler =
            DefaultUriBuilderFactory(environmentProperties.bidragPersonUrl + "/bidrag-person")
        return PersonConsumer(httpHeaderRestTemplate)
    }

    @Bean
    fun oppgaveService(
        personConsumers: ResourceByDiscriminator<PersonConsumer>,
        oppgaveConsumers: ResourceByDiscriminator<OppgaveConsumer>,
        saksbehandlerInfoManager: SaksbehandlerInfoManager,
    ): OppgaveService = OppgaveService(personConsumers, oppgaveConsumers, saksbehandlerInfoManager)

    @Bean
    fun endreJournalpostService(
        journalpostServices: ResourceByDiscriminator<JournalpostService>,
        dokarkivConsumers: ResourceByDiscriminator<DokarkivConsumer>,
        dokarkivKnyttTilSakConsumer: DokarkivKnyttTilSakConsumer,
        hendelserProducer: HendelserProducer,
        saksbehandlerInfoManager: SaksbehandlerInfoManager,
    ): EndreJournalpostService = EndreJournalpostService(
        journalpostServices.get(Discriminator.REGULAR_USER),
        dokarkivConsumers.get(Discriminator.REGULAR_USER),
        dokarkivKnyttTilSakConsumer,
        hendelserProducer,
        saksbehandlerInfoManager,
    )

    @Bean
    fun journalpostServices(
        safConsumers: ResourceByDiscriminator<SafConsumer>,
        personConsumers: ResourceByDiscriminator<PersonConsumer>,
    ): ResourceByDiscriminator<JournalpostService> {
        val journalpostServiceRegularUser = JournalpostService(
            safConsumers.get(Discriminator.REGULAR_USER),
            personConsumers.get(Discriminator.SERVICE_USER),
        )
        val journalpostServiceServiceUser = JournalpostService(
            safConsumers.get(Discriminator.SERVICE_USER),
            personConsumers.get(Discriminator.SERVICE_USER),
        )
        val journalpostServices = HashMap<Discriminator, JournalpostService>()
        journalpostServices[Discriminator.REGULAR_USER] =
            journalpostServiceRegularUser
        journalpostServices[Discriminator.SERVICE_USER] = journalpostServiceServiceUser
        return ResourceByDiscriminator(journalpostServices)
    }

    @Bean
    fun safConsumers(
        safConsumerRegularUser: SafConsumer,
        safConsumerServiceUser: SafConsumer,
        securityTokenService: SecurityTokenService,
    ): ResourceByDiscriminator<SafConsumer> {
        safConsumerRegularUser.leggTilInterceptor(securityTokenService.authTokenInterceptor("saf"))
        safConsumerServiceUser.leggTilInterceptor(
            securityTokenService.clientCredentialsTokenInterceptor("saf"),
        )
        val safConsumers = HashMap<Discriminator, SafConsumer>()
        safConsumers[Discriminator.REGULAR_USER] =
            safConsumerRegularUser
        safConsumers[Discriminator.SERVICE_USER] = safConsumerServiceUser
        return ResourceByDiscriminator(safConsumers)
    }

    @Bean
    fun oppgaveConsumers(
        oppgaveConsumerRegularUser: OppgaveConsumer,
        oppgaveConsumerServiceUser: OppgaveConsumer,
        securityTokenService: SecurityTokenService,
    ): ResourceByDiscriminator<OppgaveConsumer> {
        oppgaveConsumerRegularUser.leggTilInterceptor(
            securityTokenService.authTokenInterceptor("oppgave"),
        )
        oppgaveConsumerServiceUser.leggTilInterceptor(
            securityTokenService.clientCredentialsTokenInterceptor("oppgave"),
        )
        val safConsumers = HashMap<Discriminator, OppgaveConsumer>()
        safConsumers[Discriminator.REGULAR_USER] = oppgaveConsumerRegularUser
        safConsumers[Discriminator.SERVICE_USER] = oppgaveConsumerServiceUser
        return ResourceByDiscriminator(safConsumers)
    }

    @Bean
    fun personConsumers(
        personConsumerRegularUser: PersonConsumer,
        personConsumerServiceUser: PersonConsumer,
        securityTokenService: SecurityTokenService,
    ): ResourceByDiscriminator<PersonConsumer> {
        personConsumerRegularUser.leggTilInterceptor(
            securityTokenService.authTokenInterceptor("bidrag-person"),
        )
        personConsumerServiceUser.leggTilInterceptor(
            securityTokenService.clientCredentialsTokenInterceptor("bidrag-person"),
        )
        val personConsumers = HashMap<Discriminator, PersonConsumer>()
        personConsumers[Discriminator.REGULAR_USER] = personConsumerRegularUser
        personConsumers[Discriminator.SERVICE_USER] =
            personConsumerServiceUser
        return ResourceByDiscriminator(personConsumers)
    }

    @Bean
    fun dokarkivKnyttTilSakConsumer(
        @Qualifier("base") httpHeaderRestTemplate: HttpHeaderRestTemplate,
        environmentProperties: EnvironmentProperties,
        securityTokenService: SecurityTokenService,
    ): DokarkivKnyttTilSakConsumer {
        httpHeaderRestTemplate.uriTemplateHandler =
            DefaultUriBuilderFactory(environmentProperties.dokarkivKnyttTilSakUrl)
        httpHeaderRestTemplate.addHeaderGenerator(
            HttpHeaders.CONTENT_TYPE,
        ) { MediaType.APPLICATION_JSON_VALUE }
        val dokarkivKnyttTilSakConsumer = DokarkivKnyttTilSakConsumer(httpHeaderRestTemplate)
        dokarkivKnyttTilSakConsumer.leggTilInterceptor(
            securityTokenService.authTokenInterceptor("dokarkiv"),
        )
        return dokarkivKnyttTilSakConsumer
    }

    @Bean
    fun dokarkivConsumers(
        dokarkivConsumerRegularUser: DokarkivConsumer,
        dokarkivConsumerServiceUser: DokarkivConsumer,
        securityTokenService: SecurityTokenService,
    ): ResourceByDiscriminator<DokarkivConsumer> {
        dokarkivConsumerRegularUser.leggTilInterceptor(
            securityTokenService.authTokenInterceptor("dokarkiv"),
        )
        dokarkivConsumerServiceUser.leggTilInterceptor(
            securityTokenService.clientCredentialsTokenInterceptor("dokarkiv"),
        )
        val dokarkivConsumers = HashMap<Discriminator, DokarkivConsumer>()
        dokarkivConsumers[Discriminator.REGULAR_USER] = dokarkivConsumerRegularUser
        dokarkivConsumers[Discriminator.SERVICE_USER] = dokarkivConsumerServiceUser
        return ResourceByDiscriminator(dokarkivConsumers)
    }

    @Bean
    fun bidragOrganisasjonConsumer(
        httpHeaderRestTemplate: HttpHeaderRestTemplate,
        securityTokenService: SecurityTokenService,
        environmentProperties: EnvironmentProperties,
    ): BidragOrganisasjonConsumer {
        httpHeaderRestTemplate.uriTemplateHandler = DefaultUriBuilderFactory(
            environmentProperties.bidragOrganisasjonUrl + "/bidrag-organisasjon",
        )
        httpHeaderRestTemplate
            .interceptors
            .add(securityTokenService.clientCredentialsTokenInterceptor("bidrag-organisasjon"))
        return BidragOrganisasjonConsumer(httpHeaderRestTemplate)
    }

    @Bean
    fun environmentProperties(
        @Value("\${DOKARKIV_URL}") dokarkivUrl: String,
        @Value("\${DOKARKIV_KNYTT_TIL_SAK_URL}") dokarkivKnyttTilSakUrl: String,
        @Value("\${DOKDISTFORDELING_URL}") dokdistFordelingUrl: String?,
        @Value("\${BIDRAG_PERSON_URL}") bidragPersonUrl: String,
        @Value("\${SAF_URL}") safUrl: String,
        @Value("\${OPPGAVE_URL}") oppgaveUrl: String?,
        @Value("\${ACCESS_TOKEN_URL}") securityTokenUrl: String,
        @Value("\${BIDRAG_ORGANISASJON_URL}") bidragOrganisasjonUrl: String,
        @Value("\${NAIS_APP_NAME}") naisAppName: String,
    ): EnvironmentProperties {
        val environmentProperties = EnvironmentProperties(
            dokdistFordelingUrl,
            dokarkivUrl,
            dokarkivKnyttTilSakUrl,
            safUrl,
            oppgaveUrl,
            securityTokenUrl,
            naisAppName,
            bidragPersonUrl,
            bidragOrganisasjonUrl,
        )
        LOGGER.info(String.format("> Environment: %s", environmentProperties))
        return environmentProperties
    }

    class EnvironmentProperties(
        val dokdistFordelingUrl: String?,
        val dokarkivUrl: String,
        val dokarkivKnyttTilSakUrl: String,
        val safUrl: String,
        val oppgaveUrl: String?,
        val securityTokenUrl: String,
        val naisAppName: String,
        val bidragPersonUrl: String,
        val bidragOrganisasjonUrl: String,
    ) {
        override fun toString(): String = (
            "dokarkivUrl='" +
                dokarkivUrl +
                '\'' +
                ", safUrl='" +
                safUrl +
                '\'' +
                ", bidragPersonUrl='" +
                bidragPersonUrl +
                '\'' +
                ", dokarkivKnyttTilSakUrl='" +
                dokarkivKnyttTilSakUrl +
                '\'' +
                ", securityTokenUrl='" +
                securityTokenUrl +
                '\'' +
                ", bidragOrganisasjonUrl='" +
                bidragOrganisasjonUrl +
                '\'' +
                ", naisAppName='" +
                naisAppName
            )
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(
            BidragDokumentArkivConfig::class.java,
        )
        const val PROFILE_LIVE = "live"
        const val PROFILE_KAFKA_TEST = "kafka_test"
        const val PROFILE_TEST = "test"
    }
}
