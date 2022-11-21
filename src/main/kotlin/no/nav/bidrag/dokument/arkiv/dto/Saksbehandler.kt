package no.nav.bidrag.dokument.arkiv.dto


data class Saksbehandler(
    var ident: String? = null,
    var navn: String? = null
) {
    fun hentIdentMedNavn() = "$ident - $navn"
    fun hentSaksbehandlerInfo(saksbehandlerEnhet: String) = if (ident == null && navn == null) "ukjent saksbehandler" else hentBrukeridentMedSaksbehandler(saksbehandlerEnhet)
    private fun hentBrukeridentMedSaksbehandler(enhetsnummer: String) = "${navn?:"Ukjent"} (${ident?:"Ukjent"} - $enhetsnummer)"
    fun tilEnhet(enhetsnummer: String?): SaksbehandlerMedEnhet {
        return SaksbehandlerMedEnhet(this, enhetsnummer?:"9999")
    }
}

data class SaksbehandlerMedEnhet(val saksbehandler: Saksbehandler, val enhetsnummer: String){
    fun hentSaksbehandlerInfo() = saksbehandler.hentSaksbehandlerInfo(enhetsnummer)

}
