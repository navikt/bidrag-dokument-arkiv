# bidrag-dokument-arkiv
![](https://github.com/navikt/bidrag-dokument-arkiv/workflows/continuous%20integration/badge.svg)
![](https://github.com/navikt/bidrag-dokument-arkiv/workflows/test%20build%20on%20pull%20request/badge.svg)
[![release bidrag-dokument-arkiv](https://github.com/navikt/bidrag-dokument-arkiv/actions/workflows/release.yaml/badge.svg)](https://github.com/navikt/bidrag-dokument-arkiv/actions/workflows/release.yaml)

Microtjeneste som kommuniserer med et rest-api (GraphiQL): bidrag-dokument leser fra arkiv (JOARK)

## kjøring lokalt
Verdiene i `src/test/resources/resources.properties` må settes opp som miljøvariabler når man starter
`BidragDokumentArkiv` for kjøring lokalt.

Se [Sikkerhet](#Sikkerhet) for kjøring med sikkerhet lokalt.

## beskrivelse

Dette er en mikrotjeneste som blir brukt av `bidrag-dokument` for å hente metadata om
journalposter fra sak og arkiv (sak og arkiv facade/saf).

### bygg og kjør applikasjon

Dette er en spring-boot applikasjon og kan kjøres som java applikasjon, ved å
bruke `maven` eller ved å bygge et docker-image og kjøre dette 

Se [Sikkerhet](#Sikkerhet) for kjøring med sikkerhet lokalt.

##### java og maven
* krever installasjon av java og maven:

  * `mvn clean install`<br>
    deretter
  * `cd bidrag-dokument-arkiv`
  * `mvn spring-boot:run`<br>
     eller
  * `cd bidrag-dokument-arkiv/target`<br>
  * `java -jar bidrag-dokument-arkiv-<versjon>.jar`

##### docker og maven
* krever installasjon av java, maven og docker
* docker image er det som blir kjørt som nais applikasjon

`mvn clean install`<br>
deretter<br>
`docker build -t bidrag-dokument-arkiv .`<br>
`docker run -p 8080:8080 bidrag-dokument-arkiv`

Etter applikasjon er startet kan den nåes med browser på
`http://localhost:8080/bidrag-dokument-arkiv/swagger-ui.html`

### Sikkerhet
Tjenestens endepunkter er sikret med navikt
[token-validation-spring](https://github.com/navikt/token-support/tree/master/token-validation-spring)
fra [token-support](https://github.com/navikt/token-support). Det betyr at gyldig
OIDC-id-token må være inkludert som Bearer-token i Authorization header for alle
spørringer mot disse endepunktene. 

For kjøring lokalt benyttes
[token-validation-test-support](https://github.com/navikt/token-support/tree/master/token-validation-test-support)
som blant annet sørger for at det genereres id-tokens til test formål. For å redusere
risikoen for at testgeneratoren ved en feil gjøres aktiv i produksjon er
token-validation-test-support-modulen kun tilgjengelig i test-scope. I tillegg er bruken av
testgeneratoren kun knyttet til en egen spring-boot app-definisjon,
BidragDokumentLocal (lokalisert under test) som benytter test-profil.

BidragDokumentLocal brukes i stedet for BidragDokument ved lokal kjøring.

AUD bidrag-q-localhost er lagt til for å støtte localhost redirect i preprod. Denne benyttes ved front-end-utvikling for å kunne kjøre tester med 
preprod-tjenester uten å måtte legge inn host-mappinger. bidrag-q-localhost-agenten er satt opp vha https://github.com/navikt/amag. Denne er ikke, 
og skal heller ikke være tilgjengelig i prod.

***NB!*** Husk å sette miljøvaribler ihht. `src/test/resources/resources.properties`

#### Oppskrift for kjøring med test-token i Swagger lokalt (ved integrasjonstesting mot AM eller bidrag-dokument-journalpost i NAIS, må token hentes fra bidrag-ui.<domene-navn>/session)
 - Start BidragDokumentArkivLocal som standard Java-applikasjon
 - Hent test-token [http://localhost:8080/bidrag-dokument-arkiv/local/jwt](http://localhost:8080/bidrag-dokument-arkiv/local/jwt)
 - Åpne Swagger (http://localhost:8080/bidrag-dokument-arkiv/swagger-ui.html)
 - Trykk Authorize, og oppdater value-feltet med: Bearer <testtoken-streng> fra steg 2.
  
#### Swagger Authorize 
Den grønne authorize-knappen øverst i Swagger-ui kan brukes til å autentisere requester om du har tilgang på et gyldig OIDC-token. For å benytte authorize må følgende legges i value-feltet:
   - "Bearer id-token" (hvor id-token erstattes med et gyldig id-token (jwt-streng))
 
For localhost kan et gyldig id-token hentes med følgende URL (gitt BidragDokumentArkivLocal er startet på port 8080):
   - [http://localhost:8080/bidrag-dokument-arkiv/local/jwt](http://localhost:8080/bidrag-dokument-arkiv/local/jwt)
   
For preprod kan følgende CURL-kommando benyttes (krever tilgang til isso-agent-passord i Fasit for aktuelt miljø): 
 
```
  curl -X POST \
	   -u "{isso-agent-brukernavn}:{isso-agent-passord}" \
	   -d "grant_type=client_credentials&scope=openid" \
	   {isso-issuer-url}/access_token
```
  
hvor `{isso-agent-brukernavn}` og `{isso-agent-passord}` hentes fra Fasit-ressurs OpenIdConnect bidrag-dokument-ui-oidc for aktuelt miljø (f.eks [https://fasit.adeo.no/resources/6419841](https://fasit.adeo.no/resources/6419841) for q0),
og `{isso-issuer-url}` hentes fra Fasit-ressurs BaseUrl isso-issuer (f.eks [https://fasit.adeo.no/resources/2291405](https://fasit.adeo.no/resources/2291405) for q0.
