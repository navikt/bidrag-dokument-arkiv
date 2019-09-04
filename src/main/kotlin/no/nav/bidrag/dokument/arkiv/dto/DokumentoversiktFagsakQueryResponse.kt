package no.nav.bidrag.dokument.arkiv.dto

data class DokumentoversiktFagsakQueryResponse(
        var data: DokumentoversiktFagsak? = null
) {
    fun hentJournalposter(): List<Journalpost> {
        return data?.dokumentoversiktFagsak?.journalposter ?: throw IllegalStateException("Ingen journalposter blant data: $data")
    }

    fun harJournalpostMedId(journalpostId: Int): Boolean {
        return data?.dokumentoversiktFagsak?.journalposter?.find { it.journalpostId == journalpostId.toString() } != null
    }

    fun hentJournalpost(journalpostId: Int): Journalpost {
        return data?.dokumentoversiktFagsak?.journalposter?.find { it.journalpostId == journalpostId.toString() }
                ?: throw IllegalStateException("Ingen journalpst med id: $journalpostId")
    }
}

data class DokumentoversiktFagsak(
        var dokumentoversiktFagsak: DokumentoversiktFagsakListe? = null
)

data class DokumentoversiktFagsakListe(
        var journalposter: List<Journalpost> = emptyList()
)
