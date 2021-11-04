package no.nav.bidrag.dokument.arkiv.dto

data class KnyttTilAnnenSakRequest(
    var fagsakId: String,
    var journalfoerendeEnhet: String,
    var bruker: KnyttTilBruker,
    var tema: String
){
    constructor(saksnummer: String, journalpost: Journalpost) : this(
        fagsakId = saksnummer,
        journalfoerendeEnhet = journalpost.journalforendeEnhet ?: "",
        bruker = KnyttTilBruker(journalpost.bruker?.id, journalpost.bruker?.type),
        tema = journalpost.tema ?: ""
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