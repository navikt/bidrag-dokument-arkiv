package no.nav.bidrag.dokument.arkiv.dto

data class DokumentoversiktFagsakQueryResponse(
    var data: DokumentoversiktFagsak? = null
) {
    fun hentJournalpost(journalpostId: Int): Journalpost {
        return data?.dokumentoversiktFagsak?.journalposter?.find { it.journalpostId == journalpostId.toString() }
            ?: throw JournalpostIkkeFunnetException("Ingen journalpst med id: $journalpostId")
    }
}

data class DokumentoversiktFagsak(
    var dokumentoversiktFagsak: DokumentoversiktFagsakListe? = null
)

data class DokumentoversiktFagsakListe(
    var journalposter: List<Journalpost> = emptyList()
)
