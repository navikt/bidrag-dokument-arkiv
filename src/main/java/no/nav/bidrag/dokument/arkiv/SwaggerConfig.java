package no.nav.bidrag.dokument.arkiv;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

  @Bean
  public Docket api() {
    return new Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.basePackage(BidragDokumentArkiv.class.getPackage().getName()))
        .build()
        .securitySchemes(newArrayList(apiKey()))
        .securityContexts(newArrayList(securityContext()));
  }

  private ApiKey apiKey() {
    return new ApiKey("mykey", HttpHeaders.AUTHORIZATION, "header");
  }

  private SecurityContext securityContext() {
    return SecurityContext.builder()
        .securityReferences(defaultAuth())
        .forPaths(PathSelectors.regex("/*.*"))
        .build();
  }

  private List<SecurityReference> defaultAuth() {
    var authorizationScope = new AuthorizationScope("global", "accessEverything");
    var authorizationScopes = new AuthorizationScope[1];
    authorizationScopes[0] = authorizationScope;

    return List.of(new SecurityReference("mykey", authorizationScopes));
  }

}
