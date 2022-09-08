package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonResponse(
    var ident: String,
    var navn: String? = null,
    var aktoerId: String? = null
)

data class HentPostadresseRequest(
    var ident: String
)

data class HentPostadresseResponse(
    var adresselinje1: String?,
    var adresselinje2: String?,
    var adresselinje3: String?,
    var postnummer: String?,
    var poststed: String?,
    var land: String?
)