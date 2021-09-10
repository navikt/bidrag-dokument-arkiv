package no.nav.bidrag.dokument.arkiv.security;

import no.nav.bidrag.dokument.arkiv.consumer.AccessTokenConsumer;

public class TokenForBasicAuthenticationGenerator {

  public static final String HEADER_NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";

  private final AccessTokenConsumer accessTokenConsumer;
  private final String secretForServiceUserNotEncoded;

  public TokenForBasicAuthenticationGenerator(AccessTokenConsumer accessTokenConsumer, String secretForServiceUserNotEncoded) {
    this.accessTokenConsumer = accessTokenConsumer;
    this.secretForServiceUserNotEncoded = secretForServiceUserNotEncoded;
  }

  public String generateToken() {
    return accessTokenConsumer.finnTokenFor("srvbdarkiv", secretForServiceUserNotEncoded);
  }
}
