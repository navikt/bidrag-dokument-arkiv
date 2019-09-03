package no.nav.bidrag.dokument.arkiv.dto

data class DokumentoversiktFagsakQueryResponse(
        var data: DokumentoversiktFagsak? = null
)

data class DokumentoversiktFagsak (
        var dokumentoversiktFagsak: DokumentoversiktFagsakListe? = null
)

data class DokumentoversiktFagsakListe (
        var journalposter: List<Journalpost> = emptyList()
)
