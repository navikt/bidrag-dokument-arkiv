package no.nav.bidrag.dokument.arkiv.consumer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.GraphQueryConfiguration;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.dto.JournalpostQuery;
import no.nav.modig.testcertificates.TestCertificates;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ActiveProfiles("dev")
@TestPropertySource(locations = "/secret.properties")
@PropertySource("classpath:url.properties")
@DisplayName("Bruk av graph query language")
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
class GraphQueryConsumerIntegrationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(GraphQueryConsumerIntegrationTest.class);

  @Autowired
  private GraphQueryConfiguration graphQueryConfiguration;
  @Autowired
  private GraphQueryConsumer graphQueryConsumer;
  @Value("${ISSO_TEST_USER}")
  private String testUser;
  @Value("${ISSO_TEST_USER_PWD}")
  private String testUserPwd;
  @Value("${TOKEN_ACCESS_URL}")
  private String tokenAccessUrl;
  @Value("${TOKEN_AUTH_URL}")
  private String tokenAuthenticateUrl;
  @Value("${TOKEN_OAUTH2_URL}")
  private String tokenOauth2url;

  @BeforeAll
  static void setUpKeyAndTrustStore() {
    TestCertificates.setupKeyAndTrustStore();
  }

  @BeforeAll
  static void createCorrelationIdForIntegrationTest() {
    CorrelationId.generateTimestamped("graphql-test-bidrag");
  }

  @BeforeEach
  void setUpSecurityAndAssumeSafIsRunning() {
    String headerValue = fetchValueForAuthorization();
    assumeSafRunning(headerValue);
  }


  private String fetchValueForAuthorization() {
    String headerValue;

    try {
      String authorizationValue = fetchIssoAuthenticationValue();
      String codeValue = fetchQueryCodeParameterFromIssoUrl(authorizationValue);
      headerValue = fetchTokenValueOfAuthorizationBearer(codeValue);
    } catch (Exception e) {
      LOGGER.error("Unnable to set up security: " + e);
      throw new TestAbortedException("unable to set up security", e);
    }

    return headerValue;
  }

  private String fetchIssoAuthenticationValue() {
    var restTemplate = new RestTemplate();
    var httpHeaders = new HttpHeaders();

    httpHeaders.add(HttpHeaders.CACHE_CONTROL, "no-cache");
    httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
    httpHeaders.add("X-OpenAM-Password", testUserPwd);
    httpHeaders.add("X-OpenAM-Username", testUser);

    var responseEntity = restTemplate.exchange(
        tokenAuthenticateUrl,
        HttpMethod.POST,
        new HttpEntity<>(null, httpHeaders),
        Map.class
    );

    var jsonMap = responseEntity.getBody();

    if (jsonMap == null) {
      throw new TestAbortedException("Unable to get tokenId from " + tokenAuthenticateUrl);
    }

    return (String) jsonMap.get("tokenId");
  }

  private String fetchQueryCodeParameterFromIssoUrl(String authorizationValue) {
    var httpHeaders = new HttpHeaders();

    httpHeaders.add(HttpHeaders.CACHE_CONTROL, "no-cache");
    httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
    httpHeaders.add(HttpHeaders.COOKIE, "nav-isso=" + authorizationValue);

    var httpEntity = new HttpEntity<>(
        "client_id=bidrag-dokument-ui-q0"
            + "&response_type=code"
            + "&redirect_uri=https%3A%2F%2Fbidrag-dokument-ui.nais.preprod.local%2Fisso"
            + "&decision=allow"
            + "&csrf=" + authorizationValue
            + "&scope=openid",
        httpHeaders
    );

    var httpComponentsClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(
        HttpClientBuilder
            .create()
            .disableRedirectHandling()
            .build()
    );

    var restTemplate = new RestTemplate(httpComponentsClientHttpRequestFactory);

    var responseEntity = restTemplate.exchange(
        tokenOauth2url,
        HttpMethod.POST,
        httpEntity,
        Map.class
    );

    var headers = responseEntity.getHeaders();
    var headerValues = headers.get(HttpHeaders.LOCATION);
    var urlString = (headerValues != null && !headerValues.isEmpty()) ? headerValues.get(headerValues.size() - 1) : "?no.location.provided";
    var queryStrings = urlString.substring(urlString.indexOf('?') + 1).split("&");

    return Arrays.stream(queryStrings)
        .filter(string -> string.startsWith("code="))
        .map(string -> string.substring(string.indexOf('=') + 1))
        .findFirst()
        .orElseThrow(() -> new TestAbortedException("Unable to find code from url: " + urlString));
  }

  private String fetchTokenValueOfAuthorizationBearer(String codeValue) {
    var httpHeaders = new HttpHeaders();

    httpHeaders.put(HttpHeaders.CACHE_CONTROL, List.of("no-cache"));
    httpHeaders.put(HttpHeaders.CONTENT_TYPE, List.of("application/x-www-form-urlencoded"));
    httpHeaders.setBasicAuth("bidrag-dokument-ui-q0", "IuiDmMKsVwBY4EMf1QEOh69WyExeFdwF");

    var httpEntity = new HttpEntity<>(
        "grant_type=authorization_code"
            + "&redirect_uri=https%3A%2F%2Fbidrag-dokument-ui.nais.preprod.local%2Fisso"
            + "&code=" + codeValue,
        httpHeaders
    );

    var restTemplate = new RestTemplate();

    var responseEntity = restTemplate.exchange(
        tokenAccessUrl,
        HttpMethod.POST,
        httpEntity,
        Map.class
    );

    if (responseEntity.getBody() == null) {
      throw new TestAbortedException("Unnable to get id_token from " + tokenAccessUrl);
    }

    return (String) responseEntity.getBody().get("id_token");
  }

  private void assumeSafRunning(String headerValue) {
    var headers = new HttpHeaders();

    headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
    graphQueryConfiguration.getHttpHeaderRestTemplate().addHeaderGenerator(HttpHeaders.AUTHORIZATION, () -> "Bearer " + headerValue);

    try {
      var resttemplate = graphQueryConfiguration.getHttpHeaderRestTemplate();
      var httpEntity = new HttpEntity<>(new JournalpostQuery(101), headers);

      resttemplate.exchange("/", HttpMethod.POST, httpEntity, Map.class).getStatusCode();
    } catch (HttpClientErrorException.Unauthorized e) {
      LOGGER.error("Security is not correct: " + e);
      throw new TestAbortedException("Security is not correct", e);
    } catch (HttpClientErrorException.NotFound e) {
      LOGGER.error("Saf is not running: " + e);
      throw new TestAbortedException("Saf is not running", e);
    } catch (Exception e) {
      LOGGER.error("Exception with cause: " + e);
      throw new TestAbortedException("Exception when assuming saf is running: ", e);
    }
  }

  @Test
  @DisplayName("skal hente journalpost med SakArkivFacade (saf) uten feil")
  void skalHenteJournalpostMedSaf() {
    graphQueryConsumer.hentJournalpost(1001);
  }

  @Test
  @DisplayName("skal kunne søke etter journalposter med SakArkivFacade (saf) uten feil")
  void skalSokeEtterJournalposterMedSaf() {
    graphQueryConsumer.finnJournalposter("0000003", "BID");
  }
}
