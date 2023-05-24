package no.nav.bidrag.dokument.arkiv.consumer

import no.nav.bidrag.dokument.arkiv.dto.OppgaveData
import no.nav.bidrag.dokument.arkiv.dto.OppgaveResponse
import no.nav.bidrag.dokument.arkiv.dto.OppgaveSokResponse
import no.nav.bidrag.dokument.arkiv.dto.OpprettOppgaveRequest
import no.nav.bidrag.dokument.arkiv.model.OppgaveSokParametre
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

class OppgaveConsumer(restTemplate: RestTemplate?) : AbstractConsumer(restTemplate) {
    fun finnOppgaver(parametre: OppgaveSokParametre): OppgaveSokResponse? {
        val pathMedParametre = parametre.hentParametreForApneOppgaverSortertSynkendeEtterFrist()
        LOGGER.info("søk opp åpne oppgaver med {}", pathMedParametre)
        return restTemplate.exchange(
            pathMedParametre,
            HttpMethod.GET,
            null,
            OppgaveSokResponse::class.java
        ).body
    }

    fun opprett(opprettOppgaveRequest: OpprettOppgaveRequest): Long? {
        val oppgaveResponse =
            restTemplate.postForEntity("/", opprettOppgaveRequest, OppgaveResponse::class.java)
        LOGGER.info("Opprettet oppgave ${opprettOppgaveRequest.javaClass.simpleName} med id=${oppgaveResponse.body?.id} med type ${opprettOppgaveRequest.oppgavetype} og journalpostid ${opprettOppgaveRequest.journalpostId}")
        return oppgaveResponse.body?.id
    }

    fun patchOppgave(oppgavePatch: OppgaveData): OppgaveData? {
        LOGGER.info("${oppgavePatch.javaClass.simpleName} for oppgave med id: ${oppgavePatch.id}")
        return restTemplate.patchForObject(
            "/${oppgavePatch.id}",
            oppgavePatch,
            OppgaveData::class.java
        )
    }

    fun patchOppgaveWithVersionRetry(oppgavePatch: OppgaveData): OppgaveData? {
        try {
            return patchOppgave(oppgavePatch)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                val oppgaveData = hentOppgave(oppgavePatch.id!!)!!
                oppgavePatch.versjon = oppgaveData.versjon
                return patchOppgave(oppgavePatch)
            }
            throw e
        }
    }

    fun hentOppgave(oppgaveId: Long): OppgaveData? {
        LOGGER.info("Henter oppgave $oppgaveId")
        return restTemplate.getForObject("/${oppgaveId}", OppgaveData::class.java)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OppgaveConsumer::class.java)
    }
}
