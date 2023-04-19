package no.nav.bidrag.dokument.arkiv.security

import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler
import no.nav.bidrag.dokument.arkiv.utils.TokenUtils.henteSubject
import org.springframework.stereotype.Service

@Service
class SaksbehandlerInfoManager(
    private val bidragOrganisasjonConsumer: BidragOrganisasjonConsumer,
    private val oidcTokenManager: OidcTokenManager
) {
    fun hentSaksbehandlerBrukerId(): String? {
        return try {
            henteSubject(oidcTokenManager.fetchTokenAsString())
        } catch (e: Exception) {
            null
        }
    }

    fun hentSaksbehandler(_saksbehandlerIdent: String? = null): Saksbehandler? {
        return try {
            val saksbehandlerIdent = _saksbehandlerIdent ?: hentSaksbehandlerBrukerId() ?: return null
            val saksbehandlerNavn = bidragOrganisasjonConsumer.hentSaksbehandlerInfo(saksbehandlerIdent).navn?.verdi
            Saksbehandler(saksbehandlerIdent, saksbehandlerNavn)
        } catch (e: Exception) {
            null
        }
    }
}
