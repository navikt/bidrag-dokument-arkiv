package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException

data class DokumentoversiktFagsakQueryResponse(
    var data: DokumentoversiktFagsak? = null,
) {
    fun hentJournalpost(journalpostId: Long): Journalpost = data?.dokumentoversiktFagsak?.journalposter?.find { it.journalpostId == journalpostId.toString() }
        ?: throw JournalpostIkkeFunnetException("Ingen journalpst med id: $journalpostId")
}

data class DokumentoversiktFagsak(
    var dokumentoversiktFagsak: DokumentoversiktFagsakListe? = null,
)

data class DokumentoversiktFagsakListe(
    var journalposter: List<Journalpost> = emptyList(),
)
