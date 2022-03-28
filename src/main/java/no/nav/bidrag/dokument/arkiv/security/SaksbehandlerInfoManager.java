package no.nav.bidrag.dokument.arkiv.security;

import no.nav.bidrag.commons.security.service.OidcTokenManager;
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler;
import org.springframework.stereotype.Service;

@Service
public class SaksbehandlerInfoManager {

  private final BidragOrganisasjonConsumer bidragOrganisasjonConsumer;
  private final OidcTokenManager oidcTokenManager;
  public SaksbehandlerInfoManager(BidragOrganisasjonConsumer bidragOrganisasjonConsumer,
      OidcTokenManager oidcTokenManager) {
    this.bidragOrganisasjonConsumer = bidragOrganisasjonConsumer;
    this.oidcTokenManager = oidcTokenManager;
  }

  public Saksbehandler hentSaksbehandler(){
    try {
      var saksbehandlerIdent = oidcTokenManager.fetchToken().getSubject();
      if (saksbehandlerIdent == null){
        return null;
      }
      var response = bidragOrganisasjonConsumer.hentSaksbehandlerInfo(saksbehandlerIdent);
      if (!response.is2xxSuccessful() && response.fetchBody().isEmpty()){
        return null;
      }
      var saksbehandlerNavn = response.fetchBody().isEmpty() ? saksbehandlerIdent : response.fetchBody().get().getNavn();
      return new Saksbehandler(saksbehandlerIdent, saksbehandlerNavn);
    } catch (Exception e){
      return null;
    }

  }

}
