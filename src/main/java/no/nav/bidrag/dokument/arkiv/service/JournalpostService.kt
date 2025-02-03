package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer
import no.nav.bidrag.dokument.arkiv.dto.Bruker
import no.nav.bidrag.dokument.arkiv.dto.BrukerType
import no.nav.bidrag.dokument.arkiv.dto.DistribusjonsInfo
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import no.nav.bidrag.dokument.arkiv.model.kanIkkeHenteJournalMedUgyldigFagomrade
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.person.PersonDto
import org.slf4j.LoggerFactory
import java.util.Optional

class JournalpostService(
    private val safConsumer: SafConsumer,
    private val personConsumer: PersonConsumer,
) {
    fun hentJournalpost(journalpostId: Long): Journalpost? = hentJournalpost(journalpostId, null)

    fun hentJournalpostMedTilknyttedeSaker(journalpostId: Long): Journalpost? = hentJournalpost(journalpostId)?.let { populerMedTilknyttedeSaker(it) }

    fun hentJournalpostMedFnrOgTilknyttedeSaker(journalpostId: Long, saksnummer: String?): Optional<Journalpost> = hentJournalpostMedFnr(
        journalpostId,
        saksnummer,
    )?.let {
        Optional.ofNullable(
            populerMedTilknyttedeSaker(it),
        )
    } ?: Optional.empty()

    fun List<String>.inneholderBidragFagomrader() = this.isEmpty() || this.hentIkkeBidragFagomrader().isEmpty()

    fun List<String>.hentIkkeBidragFagomrader() = this.filter { it != "BID" && it != "FAR" }
    fun finnJournalposter(saksnummer: String, fagomrade: List<String> = emptyList()): List<JournalpostDto> {
        if (!fagomrade.inneholderBidragFagomrader()) {
            kanIkkeHenteJournalMedUgyldigFagomrade(
                fagomrade.hentIkkeBidragFagomrader().joinToString(","),
            )
        }
        return finnJournalposterForSaksnummer(saksnummer, fagomrade)
            .map { konverterAktoerIdTilFnr(it) }
            .filter { !(it.tilleggsopplysninger.isEndretTema() || it.tilleggsopplysninger.isNyDistribusjonBestilt()) }
            .map { it.tilJournalpostDto() }
    }

    private fun hentJournalpost(journalpostId: Long, saksnummer: String?): Journalpost? {
        val journalpost = safConsumer.hentJournalpost(journalpostId)
        return if (journalpost.erIkkeTilknyttetSakNarOppgitt(saksnummer)) null else journalpost
    }

    fun hentTilknyttedeJournalposter(journalpost: Journalpost): List<TilknyttetJournalpost> {
        if (journalpost.dokumenter.isEmpty() || journalpost.sak == null) {
            return listOf()
        }
        val dokumentInfoId = journalpost.dokumenter[0].dokumentInfoId
        LOGGER.info("Henter tilknyttede journalposter for journalpost ${journalpost.journalpostId} med dokumentinfoId $dokumentInfoId")
        return dokumentInfoId?.let { finnTilknyttedeJournalposter(it) } ?: emptyList()
    }

    fun finnTilknyttedeJournalposter(dokumentreferanse: String): List<TilknyttetJournalpost> = safConsumer.finnTilknyttedeJournalposter(
        dokumentreferanse,
    )

    fun populerMedTilknyttedeSaker(journalpost: Journalpost): Journalpost {
        val journalpostFagsakId = if (journalpost.sak != null) journalpost.sak!!.fagsakId else ""
        val tilknytteteJournalposter = hentTilknyttedeJournalposter(journalpost)
        val saker = tilknytteteJournalposter
            .asSequence()
            .filter { it.isNotFeilregistrert() }
            .map(TilknyttetJournalpost::sak)
            .filterNotNull()
            .map(Sak::fagsakId)
            .filterNotNull()
            .filter { it != journalpostFagsakId }.toList()
        val sakerNoDuplicates = HashSet(saker).stream().toList()
        LOGGER.info("Fant ${saker.size + 1} saker for journalpost ${journalpost.journalpostId}")
        journalpost.tilknyttedeSaker = sakerNoDuplicates
        return journalpost
    }

    fun hentJournalpostMedFnr(journalpostId: Long, saksummer: String?): Journalpost? = hentJournalpost(journalpostId, saksummer)?.let {
        konverterAktoerIdTilFnr(it)
    }

    fun finnJournalposterForSaksnummer(saksnummer: String, fagomrade: List<String> = emptyList()): List<Journalpost> = safConsumer.finnJournalposter(
        saksnummer,
        fagomrade,
    )

    fun hentDistribusjonsInfo(journalpostId: Long): DistribusjonsInfo = safConsumer.hentDistribusjonInfo(journalpostId)

    private fun konverterAktoerIdTilFnr(journalpost: Journalpost): Journalpost {
        val bruker = journalpost.bruker
        if (bruker == null || !bruker.isAktoerId()) {
            return journalpost
        }
        personConsumer.hentPerson(bruker.id).ifPresent { (brukerId): PersonDto ->
            journalpost.bruker = Bruker(brukerId.verdi, BrukerType.FNR.name)
        }
        return journalpost
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(JournalpostService::class.java)
    }
}
