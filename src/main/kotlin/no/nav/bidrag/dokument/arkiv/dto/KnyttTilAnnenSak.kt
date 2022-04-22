package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.arkiv.model.KnyttTilSakManglerTemaException


open class KnyttTilAnnenSakRequest(
    open var fagsakId: String? = null,
    open var fagsaksystem: String? = null,
    open var sakstype: String,
    open var journalfoerendeEnhet: String,
    open var bruker: KnyttTilBruker,
    open var tema: String
)
data class KnyttTilSakRequest(
    override var fagsakId: String?,
    override var journalfoerendeEnhet: String,
    override var bruker: KnyttTilBruker,
    override var tema: String
): KnyttTilAnnenSakRequest(
    sakstype = "FAGSAK",
    fagsaksystem = "BISYS",
    fagsakId = fagsakId,
    journalfoerendeEnhet = journalfoerendeEnhet,
    bruker = bruker,
    tema = tema
){
    constructor(saksnummer: String, journalpost: Journalpost) : this(saksnummer, journalpost, null)
    constructor(saksnummer: String, journalpost: Journalpost, tema: String?) : this(
        fagsakId = saksnummer,
        journalfoerendeEnhet = journalpost.journalforendeEnhet ?: "",
        bruker = KnyttTilBruker(journalpost.bruker?.id, journalpost.bruker?.type),
        tema = tema?: journalpost.tema ?: throw KnyttTilSakManglerTemaException("Kunne ikke knytte journalpost til annen sak. Journalpost mangler tema. ")
    )
}

data class KnyttTilGenerellSakRequest(
    override var journalfoerendeEnhet: String,
    override var bruker: KnyttTilBruker,
    override var tema: String
): KnyttTilAnnenSakRequest(
    sakstype = "GENERELL_SAK",
    journalfoerendeEnhet = journalfoerendeEnhet,
    bruker = bruker,
    tema = tema
){
    constructor(journalpost: Journalpost, tema: String?) : this(
        journalfoerendeEnhet = journalpost.journalforendeEnhet  ?: "9999",
        bruker = KnyttTilBruker(journalpost.bruker?.id, journalpost.bruker?.type),
        tema = tema?: journalpost.tema ?: throw KnyttTilSakManglerTemaException("Kunne ikke knytte journalpost til annen sak. Journalpost mangler tema. ")
    )
}

data class KnyttTilAnnenSakResponse(
    var nyJournalpostId: String
)

data class KnyttTilBruker(
    val id: String?,
    val idType: String?
)