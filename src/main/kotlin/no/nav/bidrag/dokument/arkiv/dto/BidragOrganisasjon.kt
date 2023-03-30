package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeografiskTilknytningResponse(var enhetIdent: String, var enhetNavn: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SaksbehandlerInfoResponse(var ident: String, var navn: String)
