package no.nav.bidrag.dokument.arkiv.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.arkiv.StaticContextAccessor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class AbstractConsumer {
  protected final RestTemplate restTemplate;

  public AbstractConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public void leggTilInterceptor(ClientHttpRequestInterceptor requestInterceptor) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      restTemplate.getInterceptors().add(requestInterceptor);
    }
  }

  protected String parseErrorMessage(HttpStatusCodeException e){
    try {
      var jsonNode = StaticContextAccessor.getBean(ObjectMapper.class).readValue(e.getResponseBodyAsString(), JsonNode.class);
      if (jsonNode.has("message")){
        return jsonNode.get("message").asText();
      }
      return e.getMessage();
    } catch (Exception ex) {
      return e.getMessage();
    }
  }
}
