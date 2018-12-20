# bidrag-dokument-arkiv
Microservice for integration with JOARK for bidrag-dokument

### kjøring lokalt
Fila `src/main/resources/url.properties` må inneholde url til rest-api for joark tjeneste.

Se [Sikkerhet](#Sikkerhet) for kjøring med sikkerhet lokalt.

### beskrivelse

Dette er en mikrotjeneste som blir brukt av andre mikrotjenester, `bidrag-dokument` og
`bidrag-dokument-hendelse`. Formålet med tjensten er å hente metadata om journalposter fra
JOARK.

### bygg og kjør applikasjon

Dette er en spring-boot applikasjon og kan kjøres som ren java applikasjon, ved å
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
Tjenestens endepunkter er sikret med navikt [oidc-spring-support](https://github.com/navikt/token-support/tree/master/oidc-spring-support)
fra [token-support](https://github.com/navikt/token-support). Det betyr at gyldig OIDC-id-token må være inkludert som Bearer-token i Authorization 
header for alle spørringer mot disse endepunktene. 

For kjøring lokalt benyttes [oidc-test-support](https://github.com/navikt/token-support/tree/master/oidc-test-support) som blan annet sørger for
at det genereres id-tokens til test formål. For å redusere risikoen for at testgeneratoren ved en feil gjøres aktiv i produksjon er 
oidc-test-support-modulen kun tilgjengelig i test-scope. I tillegg er bruken av testgeneratoren kun knyttet til en egen spring-boot app-definisjon
, BidragDokumentLocal (lokalisert under test) som benytter dev-profil.

BidragDokumentLocal brukes i stedet for BidragDokument ved lokal kjøring.

#### Oppskrift for kjøring med sikkerhet lokalt
 - Start BidragDokumentLocal som standard Java-applikasjon
 
 - Registrere app-instans for bruk av oidc-test-support, naviger til:<br> 
 	 - [http://localhost:8080/bidrag-dokument-journalpost/local/cookie?redirect=/bidrag-dokument-journalpost/api](http://localhost:8080/bidrag-dokument-journalpost/local/cookie?redirect=/bidrag-dokument-journalpost/api)
 	 
 - (Valgfri) Verifiser at test-tokengeneratoren fungerer ved å hente frem:<br>
 	 - [http://localhost:8080/bidrag-dokument-journalpost/local/jwt](http://localhost:8080/bidrag-dokument-journalpost/local/jwt)<br> 	  	
 	 - [http://localhost:8080/bidrag-dokument-journalpost/local/cookie](http://localhost:8080/bidrag-dokument-journalpost/local/cookie)<br> 	  	 
  	 - [http://localhost:8080/bidrag-dokument-journalpost/local/claims](http://localhost:8080/bidrag-dokument-journalpost/local/claims)<br>
  
