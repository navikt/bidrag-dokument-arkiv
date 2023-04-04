package no.nav.bidrag.dokument.arkiv.dto

data class PersonRequest(
    val ident: String,
    val verdi: String = ident
)

data class HentPostadresseResponse(
    var adresselinje1: String?,
    var adresselinje2: String?,
    var adresselinje3: String?,
    var postnummer: String?,
    var poststed: String?,
    var land: String?
)
