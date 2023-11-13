package no.nav.bidrag.dokument.arkiv.kafka

import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import no.nav.bidrag.dokument.arkiv.SECURE_LOGGER
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer
import no.nav.bidrag.dokument.arkiv.consumer.OppgaveConsumer
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.OppdaterSakRequest
import no.nav.bidrag.dokument.arkiv.dto.OppgaveData
import no.nav.bidrag.dokument.arkiv.dto.OpprettNyReturLoggRequest
import no.nav.bidrag.dokument.arkiv.kafka.dto.OppgaveKafkaHendelse
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.JournalpostHarIkkeKommetIRetur
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.arkiv.service.JournalpostService
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import java.time.LocalDate

private val LOGGER = KotlinLogging.logger {}

@Service
class BehandleOppgaveHendelseService(
    dokarkivConsumers: ResourceByDiscriminator<DokarkivConsumer?>,
    oppgaveConsumers: ResourceByDiscriminator<OppgaveConsumer?>,
    journalpostServices: ResourceByDiscriminator<JournalpostService?>,
    private val meterRegistry: MeterRegistry
) {
    private val dokarkivConsumer: DokarkivConsumer
    private val oppgaveConsumer: OppgaveConsumer
    private val journalpostService: JournalpostService

    init {
        dokarkivConsumer = dokarkivConsumers.get(Discriminator.SERVICE_USER)
        oppgaveConsumer = oppgaveConsumers.get(Discriminator.SERVICE_USER)
        journalpostService = journalpostServices.get(Discriminator.SERVICE_USER)
    }

    // Returoppgave opprettes før journalpost retur attributter oppdateres. Det kan derfor hende at
    // journalpost ikke er markert at det har kommet i retur og må derfor prøves flere ganger
    @Retryable(
        value = [JournalpostHarIkkeKommetIRetur::class],
        maxAttempts = 5,
        backoff = Backoff(delay = 1000, maxDelay = 12000, multiplier = 2.0)
    )
    fun behandleReturOppgaveOpprettetHendelse(oppgaveHendelse: OppgaveKafkaHendelse) {
        val oppgave = validerOgHentOppgave(oppgaveHendelse) ?: return
        LOGGER.info {
            "Sjekker om det skal legges til returlogg med dagens dato på journalpost ${oppgave.journalpostId}"
        }
        journalpostService
            .hentJournalpost(oppgave.journalpostId!!.toLong())?.let { journalpost: Journalpost ->
                if (journalpost.manglerReturDetaljForSisteRetur()) {
                    dokarkivConsumer.endre(
                        OpprettNyReturLoggRequest(
                            journalpost,
                            opprettKommentarSomLeggesTilReturlogg(journalpost)
                        )
                    )
                    LOGGER.info {
                        "Lagt til ny returlogg med returdato ${LocalDate.now()} på journalpost ${journalpost.journalpostId} med dokumentdato ${journalpost.hentDatoDokument()}."
                    }
                } else if (!journalpost.isDistribusjonKommetIRetur()) {
                    LOGGER.warn {
                        "Journalpost ${oppgave.journalpostId} har ikke kommet i retur. Det kan hende dette skyldes race-condition hvor retur oppgave er opprettet før journalpost er oppdatert. Forsøker på nytt."
                    }
                    throw JournalpostHarIkkeKommetIRetur(
                        "Journalpost ${oppgave.journalpostId} har ikke kommet i retur"
                    )
                } else {
                    LOGGER.warn {
                        "Legger ikke til ny returlogg på journalpost ${journalpost.journalpostId}. Journalpost har allerede registrert returlogg for siste retur"
                    }
                }
                oppdaterOppgaveSaksreferanse(journalpost, oppgave)
            }
            ?: run {
                LOGGER.error {
                    "Fant ingen journalpost med id ${oppgave.journalpostId}"
                }
            }
        meterRegistry.counter("ny_retur_oppgave", "tema", oppgaveHendelse.tema).increment()
    }

    private fun validerOgHentOppgave(oppgaveHendelse: OppgaveKafkaHendelse): OppgaveData? {
        if (!oppgaveHendelse.erReturOppgave()) {
            LOGGER.warn("Oppgave ${oppgaveHendelse.oppgaveId} er ikke returoppgave. Avslutter behandling")
            return null
        }
        val oppgave = oppgaveConsumer.hentOppgave(oppgaveHendelse.oppgaveId)
        if (oppgave == null) {
            LOGGER.warn(
                "Fant ingen oppgave med id ${oppgaveHendelse.oppgaveId}"
            )
            return null
        }

        if (oppgave.journalpostId.isNullOrEmpty()) {
            LOGGER.warn(
                "Returoppgave ${oppgave.id} har ingen journalpostid. Avslutter behandling"
            )
            return null
        }

        return oppgave
    }

    private fun oppdaterOppgaveSaksreferanse(
        journalpost: Journalpost,
        oppgave: OppgaveData
    ) {
        try {
            val kommentar = opprettKommentarSomLeggesTilOppgave(journalpost)
            if (oppgave.saksreferanse != journalpost.hentSaksnummer()) {
                oppgaveConsumer.patchOppgaveWithVersionRetry(
                    OppdaterSakRequest(
                        oppgave,
                        journalpost.hentSaksnummer(),
                        kommentar
                    )
                )
                LOGGER.info {
                    "Oppdatert returoppgave ${oppgave.id} saksreferanse til ${journalpost.hentSaksnummer()} og lagt til kommentar $kommentar. JournalpostId=${journalpost.journalpostId}"
                }
            } else {
                LOGGER.info {
                    "Returoppgave ${oppgave.id} har allerede saksreferanse ${journalpost.hentSaksnummer()}. Gjør ingen endringer. JournalpostId=${journalpost.journalpostId}"
                }
            }
        } catch (e: Exception) {
            LOGGER.error(e) {
                "Det skjedde en feil ved oppdatering av saksreferanse på oppgave ${oppgave.id}"
            }
        }
    }

    private fun Journalpost.harReturKommetFraNavNo() = distribuertTilAdresse() == null
    private fun opprettKommentarSomLeggesTilOppgave(journalpost: Journalpost): String? {
        SECURE_LOGGER.info(
            "Journalpost kommet retur med følgende detaljer ${journalpost.journalpostId} ${journalpost.journalstatus} ${journalpost.distribuertTilAdresse()} ${
            journalpost.relevanteDatoer.joinToString(",") { "${it.datotype}:${it.dato}" }
            }"
        )
        return if (journalpost.harReturKommetFraNavNo()) "Mottaker har ikke åpnet forsendelsen via www.nav.no innen 40 timer. Ingen postadresse er registrert. Vurder om mottaker har adresse forsendelsen kan sendes til." else null
    }

    private fun opprettKommentarSomLeggesTilReturlogg(journalpost: Journalpost): String {
        return if (journalpost.harReturKommetFraNavNo()) "Distribusjon feilet, mottaker mangler postadresse" else "Returpost"
    }
}
