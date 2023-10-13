# bidrag-dokument-arkiv

![](https://github.com/navikt/bidrag-dokument-arkiv/workflows/continuous%20integration/badge.svg)
![](https://github.com/navikt/bidrag-dokument-arkiv/workflows/test%20build%20on%20pull%20request/badge.svg)
[![release bidrag-dokument-arkiv](https://github.com/navikt/bidrag-dokument-arkiv/actions/workflows/release.yaml/badge.svg)](https://github.com/navikt/bidrag-dokument-arkiv/actions/workflows/release.yaml)

## Beskrivelse

Bidrag-dokument-arkiv er en mikrotjeneste som fungerer som en facade mot fagarkivet (JOARK) for
Bidrag/Farskap journalposter.<br/>
Tjenestene i denne applikasjonen blir primært brukt av `bidrag-dokument` for å hente/endre data fra
fagarkivet.

Denne applikasjonen tilbyr følgende operasjoner mot fagarkivet:

* Hente journalpost metadata fra SAFd
* Hente dokument bytedata fra SAF
* Oppdatere journalpost metadata ved å kalle Dokarkiv
* Bestille distribusjon av utgående journalpost ved å kalle dokdistfordeling
* Lytt på oppgave-opprettet og Joark journalføring
  hendelser [HendelseListener](src/main/java/no/nav/bidrag/dokument/arkiv/kafka/HendelseListener.java)
    * Lytt på oppgave opprettet hendelse som sjekker om en returoppgave er opprettet
        * Hendelsen oppdaterer returdato på journalpost hvis returoppgave er opprettet for en Bidrag
          journalpost
    * Lytt på journalføringshendelse for mottatte og journalførte inngående journalposter
        * Hendelsen henter journalpost og publiserer ny hendelse til `bidrag.journalpost` topic med
          journalpost metadata
        * Hendelser lagt til `bidrag.journalpost` blir lest av `bidrag-arbeidsflyt`.
        * `bidrag-arbeidsflyt` oppretter journalføringsoppgave for journalpost hvis status er
          MOTTATT og lukker journalføringsoppgaver hvis status er
          journalført eller endret tema til ikke bidrag tema

## Lokal utvikling

Verdiene i `src/test/resources/application-local.yaml` må settes opp som miljøvariabler når en
starter
`BidragDokumentArkiv` for kjøring lokalt. Start applikasjon med `spring.profiles.active=local,live`
`SRV_BD_ARKIV_AUTH` kan hentes fra Vault

#### Kjør lokalt med kafka

Start kafka lokalt i en docker container med følgende kommando på root mappen

````bash
docker-compose up -d
````

Start opp applikasjon ved å kjøre BidragDokumentArkivLocal.java under test/java mappen.
Når du starter applikasjon må følgende miljøvariabler settes

```bash
-DAZURE_APP_CLIENT_SECRET=<secret>
-DAZURE_APP_CLIENT_ID=<secret>
-DSRV_BD_ARKIV_AUTH=<secret> - kan hentes fra Vault
```

Disse kan hentes ved å kjøre kan hentes ved å
kjøre `kubectl exec --tty deployment/bidrag-dokument-arkiv-feature -- printenv | grep -e AZURE_APP_CLIENT_ID -e AZURE_APP_CLIENT_SECRET`

Bruk `kcat` til å lese meldinger fra kafka topic. Feks

````bash
kcat -b localhost:9092 -t bidrag-journalpost -C:
````

Eller sende meldinger til kafka topic

````bash
kcat -b localhost:9092 -t oppgave-opprettet -P -K:
````

og lim inn eks:

```bash
12345678:{"id":135894,"tildeltEnhetsnr":"4806","opprettetAvEnhetsnr":"9999","journalpostId":"453836528","saksreferanse":"123213123","tema":"BID","oppgavetype":"RETUR","versjon":1,"beskrivelse":"Returpost","fristFerdigstillelse":"2022-06-09","aktivDato":"2022-03-29","opprettetTidspunkt":"2023-05-24T13:19:35.25+02:00","opprettetAv":"srvbisys","prioritet":"HOY","status":"OPPRETTET","statuskategori":"AAPEN","ident":{"identType":"AKTOERID","verdi":"2421516513291","folkeregisterident":"22447402207"}}
```

og deretter trykk Ctrl+D. Da vil meldingen bli sendt til topic oppgave-opprettet

Det er ikke mulig å sende hendelse til journalforinghendelse topic da lytteren forventer avro
melding og kcat støtter ikke å serialisere json til avro
på en enkel måte

#### Kjøre lokalt mot sky

For å kunne kjøre lokalt mot sky må du gjøre følgende

Åpne terminal på root mappen til `bidrag-dokument-arkiv`
Konfigurer kubectl til å gå mot kluster `dev-fss`

```bash
# Sett cluster til dev-fss
kubectx dev-fss
# Sett namespace til bidrag
kubens bidrag 

# -- Eller hvis du ikke har kubectx/kubens installert 
# (da må -n=bidrag legges til etter exec i neste kommando)
kubectl config use dev-fss
```

Deretter kjør følgende kommando for å importere secrets. Viktig at filen som opprettes ikke
committes til git

```bash
kubectl exec --tty deployment/bidrag-dokument-arkiv printenv | grep -E 'AZURE_|_URL|SCOPE|SRV|NAIS_APP_NAME|TOPIC' > src/test/resources/application-lokal-nais-secrets.properties
```

Kjør
filen [BidragDokumentArkivLokalNais](src/test/java/no/nav/bidrag/dokument/arkiv/BidragDokumentArkivLokalNais.java)

Deretter kan tokenet brukes til å logge inn på swagger-ui http://localhost:8082/swagger-ui.html