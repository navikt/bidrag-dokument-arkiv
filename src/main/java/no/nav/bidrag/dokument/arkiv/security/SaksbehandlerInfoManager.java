package no.nav.bidrag.dokument.arkiv.security;

import java.util.Optional;
import no.nav.bidrag.commons.security.service.OidcTokenManager;
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler;
import no.nav.bidrag.dokument.arkiv.utils.TokenUtils;
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


  public String hentSaksbehandlerBrukerId(){
    try {
      return TokenUtils.henteSubject(oidcTokenManager.fetchTokenAsString());
    } catch (Exception e){
      return null;
    }
  }
  public Optional<Saksbehandler> hentSaksbehandler(){
    try {
      var saksbehandlerIdent = hentSaksbehandlerBrukerId();
      if (saksbehandlerIdent == null){
        return Optional.empty();
      }
      var saksbehandlerNavn = bidragOrganisasjonConsumer.hentSaksbehandlerInfo(saksbehandlerIdent).getNavn();
      return Optional.of(new Saksbehandler(saksbehandlerIdent, saksbehandlerNavn));
    } catch (Exception e){
      return Optional.empty();
    }
  }

}
