package no.nav.bidrag.dokument.arkiv.service;

import no.nav.bidrag.dokument.dto.DokumentDto;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.dok.tjenester.journalfoerinngaaende.ArkivSakNoArkivsakSystemEnum;
import no.nav.dok.tjenester.journalfoerinngaaende.Avsender;
import no.nav.dok.tjenester.journalfoerinngaaende.Bruker;
import no.nav.dok.tjenester.journalfoerinngaaende.Dokument;
import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JournalpostMapper {

    JournalpostDto fra(GetJournalpostResponse getJournalpostResponse, Integer journalpostId) {
        JournalpostDto journalpostDto = new JournalpostDto();
        journalpostDto.setAvsenderNavn(fra(getJournalpostResponse.getAvsender()));
        journalpostDto.setFagomrade(fra(getJournalpostResponse.getArkivSak()));
        journalpostDto.setDokumentDato(null); // ???
        journalpostDto.setDokumenter(fraDokumentListe(getJournalpostResponse.getDokumentListe()));
        journalpostDto.setGjelderBrukerId(fraBrukerListe(getJournalpostResponse.getBrukerListe()));
        journalpostDto.setInnhold(getJournalpostResponse.getTittel());
        journalpostDto.setJournalforendeEnhet(getJournalpostResponse.getJournalfEnhet());
        journalpostDto.setJournalfortAv(null); // ???
        journalpostDto.setJournalfortDato(fra(getJournalpostResponse.getForsendelseMottatt()));
        journalpostDto.setJournalpostId("JOARK-" + journalpostId);
        journalpostDto.setMottattDato(fra(getJournalpostResponse.getForsendelseMottatt()));
        journalpostDto.setSaksnummer(prefixMedGSAK(getJournalpostResponse.getArkivSak()));

        return journalpostDto;
    }

    private String fra(Avsender avsender) {
        if (avsender != null) {
            return avsender.getNavn();
        }

        return null;
    }

    private String fra(ArkivSakNoArkivsakSystemEnum arkivSak) {
        if (arkivSak != null) {
            return arkivSak.getArkivSakSystem();
        }

        return null;
    }

    private List<DokumentDto> fraDokumentListe(List<Dokument> dokumentListe) {
        if (dokumentListe != null) {
            return dokumentListe.stream().map(this::fraDokument).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private DokumentDto fraDokument(Dokument dokument) {
        DokumentDto dokumentDto = new DokumentDto();
        dokumentDto.setDokumentType(dokument.getDokumentTypeId());
        dokumentDto.setDokumentreferanse(dokument.getDokumentId());
        dokumentDto.setTittel(dokument.getTittel());

        return dokumentDto;
    }

    private List<String> fraBrukerListe(List<Bruker> brukerListe) {
        if (brukerListe != null) {
            return brukerListe.stream().map(Bruker::getIdentifikator).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private LocalDate fra(Date forsendelseMottatt) {
        if (forsendelseMottatt != null) {
            return forsendelseMottatt.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        return null;
    }

    private String prefixMedGSAK(ArkivSakNoArkivsakSystemEnum arkivSaK) {
        return arkivSaK != null ? "GSAK-" + arkivSaK.getArkivSakId() : null;
    }
}
