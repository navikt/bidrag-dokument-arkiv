# bidrag-dokument-arkiv
Microservice for integration with JOARK for bidrag-dokument

### kjøring lokalt
Fila `src/main/resources/url.properties` må inneholde url til rest-api for joark tjeneste.

### beskrivelse

Dette er en mikrotjeneste som blir brukt av andre mikrotjenester, `bidrag-dokument` og
`bidrag-dokument-hendelse`. Formålet med tjensten er å hente metadata om journalposter fra
JOARK.

### bygg og kjør applikasjon

Dette er en spring-boot applikasjon og kan kjøres som ren java applikasjon, ved å
bruke `maven` eller ved å bygge et docker-image og kjøre dette 

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
`http://localhost:8080/bidrag-dokument-arkiv/v2/api-docs`
