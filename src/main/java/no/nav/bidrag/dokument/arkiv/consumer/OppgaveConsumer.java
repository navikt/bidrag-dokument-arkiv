package no.nav.bidrag.dokument.arkiv.consumer;

import java.util.Optional;
import no.nav.bidrag.dokument.arkiv.dto.OppgaveData;
import no.nav.bidrag.dokument.arkiv.dto.OppgaveResponse;
import no.nav.bidrag.dokument.arkiv.dto.OppgaveSokResponse;
import no.nav.bidrag.dokument.arkiv.dto.OpprettOppgaveRequest;
import no.nav.bidrag.dokument.arkiv.model.OppgaveSokParametre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class OppgaveConsumer extends AbstractConsumer {
  private static final Logger LOGGER = LoggerFactory.getLogger(OppgaveConsumer.class);

  public OppgaveConsumer(RestTemplate restTemplate) {
    super(restTemplate);
  }

  public OppgaveSokResponse finnOppgaver(OppgaveSokParametre parametre) {
    var pathMedParametre = parametre.hentParametreForApneOppgaverSortertSynkendeEtterFrist();
    LOGGER.info("søk opp åpne oppgaver med {}", pathMedParametre);

    var responseEntity = restTemplate.exchange(pathMedParametre, HttpMethod.GET, null, OppgaveSokResponse.class);

    LOGGER.debug("Response: {}, HttpStatus: {}", responseEntity.getBody(), responseEntity.getStatusCode());

    return responseEntity.getBody();

  }

  public long opprett(OpprettOppgaveRequest opprettOppgaveRequest) {

    LOGGER.debug("oppretter oppgave: " + opprettOppgaveRequest);
    LOGGER.info("oppretter oppgave med type {} og journalpostid {}", opprettOppgaveRequest.getOppgavetype(), opprettOppgaveRequest.getJournalpostId());

    var oppgaveResponse = restTemplate.postForEntity("/", opprettOppgaveRequest, OppgaveResponse.class);

    LOGGER.debug("oppgaveResponse: " + oppgaveResponse);

    return Optional.of(oppgaveResponse)
        .map(ResponseEntity::getBody)
        .map(OppgaveResponse::getId)
        .orElse(-1L);
  }

  public OppgaveData patchOppgave(OppgaveData oppgavePatch) {
    LOGGER.info("{} for oppgave med id: {}", oppgavePatch.getClass().getSimpleName(), oppgavePatch.getId());

    return restTemplate.patchForObject("/%s".formatted(oppgavePatch.getId()), oppgavePatch, OppgaveData.class);
  }

}
