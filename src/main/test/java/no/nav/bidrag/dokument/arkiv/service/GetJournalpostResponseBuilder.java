package no.nav.bidrag.dokument.arkiv.service;

import no.nav.dok.tjenester.journalfoerinngaaende.ArkivSakNoArkivsakSystemEnum;
import no.nav.dok.tjenester.journalfoerinngaaende.Avsender;
import no.nav.dok.tjenester.journalfoerinngaaende.Bruker;
import no.nav.dok.tjenester.journalfoerinngaaende.Dokument;
import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

import static java.util.Collections.singletonList;

@SuppressWarnings("SameParameterValue") class GetJournalpostResponseBuilder {
    private GetJournalpostResponse getJournalpostResponse = new GetJournalpostResponse();

    GetJournalpostResponseBuilder withArkivSakId(String arkivSakId) {
        ArkivSakNoArkivsakSystemEnum arkivSak = new ArkivSakNoArkivsakSystemEnum();
        arkivSak.setArkivSakId(arkivSakId);

        getJournalpostResponse.setArkivSak(arkivSak);

        return this;
    }

    GetJournalpostResponseBuilder withAvsender(String avsenderNavn) {
        Avsender avsender = new Avsender();
        avsender.setNavn(avsenderNavn);

        getJournalpostResponse.setAvsender(avsender);

        return this;
    }

    GetJournalpostResponseBuilder withBruker(String identifikator) {
        getJournalpostResponse.setBrukerListe(singletonList(initBruker(identifikator)));
        return this;
    }

    private Bruker initBruker(String identifikator) {
        Bruker bruker = new Bruker();
        bruker.setIdentifikator(identifikator);
        return bruker;
    }

    GetJournalpostResponseBuilder withArkivSakSystem(String arkivSakSystem) {
        ArkivSakNoArkivsakSystemEnum arkivSak = new ArkivSakNoArkivsakSystemEnum();
        arkivSak.setArkivSakSystem(arkivSakSystem);

        getJournalpostResponse.setArkivSak(arkivSak);

        return this;
    }

    GetJournalpostResponseBuilder withForsendelseMottatt(LocalDate forsendelseMottatt) {
        getJournalpostResponse.setForsendelseMottatt(Date.from(forsendelseMottatt.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        return this;
    }

    GetJournalpostResponseBuilder withDokumentId(String dokumentId) {
        initDokument().setDokumentId(dokumentId);
        return this;
    }

    private Dokument initDokument() {
        if (getJournalpostResponse.getDokumentListe() == null) {
            getJournalpostResponse.setDokumentListe(new ArrayList<>());
        }

        if (getJournalpostResponse.getDokumentListe().isEmpty()) {
            getJournalpostResponse.setDokumentListe(singletonList(new Dokument()));
        }

        return getJournalpostResponse.getDokumentListe().get(0);
    }

    GetJournalpostResponseBuilder withTittel(String tittel) {
        getJournalpostResponse.setTittel(tittel);
        return this;
    }

    GetJournalpostResponseBuilder withJournalfEnhet(String journalfEnhet) {
        getJournalpostResponse.setJournalfEnhet(journalfEnhet);
        return this;
    }

    GetJournalpostResponseBuilder withDokumentTypeId(String dokumentTypeId) {
        initDokument().setDokumentTypeId(dokumentTypeId);
        return this;
    }

    GetJournalpostResponse get() {
        return getJournalpostResponse;
    }
}
