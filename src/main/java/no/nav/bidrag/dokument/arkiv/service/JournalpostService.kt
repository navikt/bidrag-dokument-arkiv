package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer
import no.nav.bidrag.dokument.arkiv.dto.Bruker
import no.nav.bidrag.dokument.arkiv.dto.BrukerType
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import no.nav.bidrag.dokument.arkiv.model.hentJournalMedUgyldigFagomrade
import no.nav.bidrag.dokument.dto.JournalpostDto
import org.slf4j.LoggerFactory
import java.util.Objects
import java.util.Optional
import java.util.stream.Collectors

class JournalpostService(private val safConsumer: SafConsumer, private val personConsumer: PersonConsumer) {
    fun hentJournalpost(journalpostId: Long): Optional<Journalpost> {
        return hentJournalpost(journalpostId, null)?.let { Optional.ofNullable(it) } ?: Optional.empty()
    }

    fun hentJournalpostMedTilknyttedeSaker(journalpostId: Long): Optional<Journalpost> {
        val jpOptional = hentJournalpost(journalpostId)
        return if (jpOptional.isEmpty) jpOptional
        else Optional.of(populerMedTilknyttedeSaker(jpOptional.get()))
    }

    fun hentJournalpostMedFnrOgTilknyttedeSaker(journalpostId: Long, saksnummer: String?): Optional<Journalpost> {
        return hentJournalpostMedFnr(journalpostId, saksnummer)?.let { Optional.ofNullable(populerMedTilknyttedeSaker(it)) } ?: Optional.empty()
    }

    fun List<String>.inneholderBidragFagomrader() = this.isEmpty() || this.hentIkkeBidragFagomrader().isEmpty()
    fun List<String>.hentIkkeBidragFagomrader() = this.filter { it != "BID" && it != "FAR" }
    fun finnJournalposter(saksnummer: String, fagomrade: List<String> = emptyList()): List<JournalpostDto> {
        if (!fagomrade.inneholderBidragFagomrader()) hentJournalMedUgyldigFagomrade(fagomrade.hentIkkeBidragFagomrader().joinToString(","))
        return finnJournalposterForSaksnummer(saksnummer, fagomrade)
            .map { journalpost: Journalpost -> konverterAktoerIdTilFnr(journalpost) }
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

    fun finnTilknyttedeJournalposter(dokumentreferanse: String): List<TilknyttetJournalpost> {
        return safConsumer.finnTilknyttedeJournalposter(dokumentreferanse)
    }

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

    private fun hentJournalpostMedFnr(journalpostId: Long, saksummer: String?): Journalpost? {
        return hentJournalpost(journalpostId, saksummer)?.let { konverterAktoerIdTilFnr(it) }
    }

    fun finnJournalposterForSaksnummer(saksnummer: String, fagomrade: List<String> = emptyList()): List<Journalpost> {
        return safConsumer.finnJournalposter(saksnummer, fagomrade)
    }

    private fun konverterAktoerIdTilFnr(journalpost: Journalpost): Journalpost {
        val bruker = journalpost.bruker
        if (bruker == null || !bruker.isAktoerId()) {
            return journalpost
        }
        personConsumer.hentPerson(bruker.id).ifPresent { (brukerId): PersonResponse ->
            journalpost.bruker = Bruker(brukerId, BrukerType.FNR.name)
        }
        return journalpost
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(JournalpostService::class.java)
    }
}