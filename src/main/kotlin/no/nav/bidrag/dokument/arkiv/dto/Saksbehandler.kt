package no.nav.bidrag.dokument.arkiv.dto


data class Saksbehandler(
    var ident: String = "",
    var navn: String? = null
) {
    fun hentIdentMedNavn() = "$ident - $navn"
    fun hentSaksbehandlerInfo(journalforendeEnhet: String) = "$navn ($ident - $journalforendeEnhet)"
    fun tilEnhet(enhetsnummer: String?): SaksbehandlerMedEnhet {
        return SaksbehandlerMedEnhet(this, enhetsnummer)
    }
}

data class SaksbehandlerMedEnhet(val saksbehandler: Saksbehandler, val enhetsnummer: String?)
