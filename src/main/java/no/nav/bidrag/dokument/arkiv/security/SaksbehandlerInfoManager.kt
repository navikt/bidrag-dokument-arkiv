package no.nav.bidrag.dokument.arkiv.security

import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler
import no.nav.bidrag.dokument.arkiv.utils.TokenUtils.henteSubject
import org.springframework.stereotype.Service
import java.util.Optional

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

    fun hentSaksbehandler(_saksbehandlerIdent: String? = null): Optional<Saksbehandler> {
        return try {
            val saksbehandlerIdent = _saksbehandlerIdent ?: hentSaksbehandlerBrukerId() ?: return Optional.empty()
            val saksbehandlerNavn = bidragOrganisasjonConsumer.hentSaksbehandlerInfo(saksbehandlerIdent).navn
            Optional.of(Saksbehandler(saksbehandlerIdent, saksbehandlerNavn))
        } catch (e: Exception) {
            Optional.empty()
        }
    }
}
