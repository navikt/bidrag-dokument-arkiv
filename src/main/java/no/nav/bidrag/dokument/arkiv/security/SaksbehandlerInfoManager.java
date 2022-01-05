package no.nav.bidrag.dokument.arkiv.security;

import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler;
import no.nav.bidrag.tilgangskontroll.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class SaksbehandlerInfoManager {

  private final BidragOrganisasjonConsumer bidragOrganisasjonConsumer;
  private final OidcTokenGenerator oidcTokenGenerator;
  public SaksbehandlerInfoManager(BidragOrganisasjonConsumer bidragOrganisasjonConsumer,
      OidcTokenGenerator oidcTokenGenerator) {
    this.bidragOrganisasjonConsumer = bidragOrganisasjonConsumer;
    this.oidcTokenGenerator = oidcTokenGenerator;
  }

  public Saksbehandler hentSaksbehandler(){
    var subject = oidcTokenGenerator.getToken().map(SecurityUtils::henteSubject);
    if (subject.isEmpty()){
      return null;
    }
    var saksbehandlerIdent = subject.get();
    var response = bidragOrganisasjonConsumer.hentSaksbehandlerInfo(saksbehandlerIdent);
    if (!response.is2xxSuccessful() && response.fetchBody().isEmpty()){
      return null;
    }
    var saksbehandlerNavn = response.fetchBody().isEmpty() ? saksbehandlerIdent : response.fetchBody().get().getNavn();
    return new Saksbehandler(saksbehandlerIdent, saksbehandlerNavn);
  }

}
