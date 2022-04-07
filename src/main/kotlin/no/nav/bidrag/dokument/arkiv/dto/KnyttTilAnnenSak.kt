package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.arkiv.model.KnyttTilSakManglerTemaException

data class KnyttTilAnnenSakRequest(
    var fagsakId: String,
    var journalfoerendeEnhet: String,
    var bruker: KnyttTilBruker,
    var tema: String
){
    constructor(saksnummer: String, journalpost: Journalpost) : this(saksnummer, journalpost, null)
    constructor(saksnummer: String, journalpost: Journalpost, tema: String?) : this(
        fagsakId = saksnummer,
        journalfoerendeEnhet = journalpost.journalforendeEnhet ?: "",
        bruker = KnyttTilBruker(journalpost.bruker?.id, journalpost.bruker?.type),
        tema = tema?: journalpost.tema ?: throw KnyttTilSakManglerTemaException("Kunne ikke knytte journalpost til annen sak. Journalpost mangler tema. ")
    )
    var sakstype: String = "FAGSAK"
    var fagsaksystem: String = "BISYS"
}

data class KnyttTilAnnenSakResponse(
    var nyJournalpostId: String
)

data class KnyttTilBruker(
    val id: String?,
    val idType: String?
)