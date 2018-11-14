package no.nav.bidrag.dokument.arkiv.service;

import no.nav.bidrag.dokument.dto.DokumentDto;
import no.nav.bidrag.dokument.dto.JournalpostDto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

@SuppressWarnings("SameParameterValue")
public class JournalpostDtoBygger {
    private JournalpostDto journalpostDto = new JournalpostDto();
    private boolean medBrukerId = true;
    private boolean medDokument = true;

    public JournalpostDto build() {
        if (journalpostDto.getDokumenter().isEmpty() && medDokument) {
            journalpostDto.setDokumenter(Collections.singletonList(new DokumentDto()));
        }

        if (journalpostDto.getGjelderBrukerId().isEmpty() && medBrukerId) {
            journalpostDto.setGjelderBrukerId(Collections.singletonList("brukerId"));
        }

        return journalpostDto;
    }

    public JournalpostDtoBygger medAvsenderNavn(String avsenderNavn) {
        journalpostDto.setAvsenderNavn(avsenderNavn);
        return this;
    }

    public JournalpostDtoBygger medDokumentdato(LocalDate date) {
        journalpostDto.setDokumentDato(date);
        return this;
    }

    public JournalpostDtoBygger medInnhold(String innhold) {
        journalpostDto.setInnhold(innhold);
        return this;
    }

    public JournalpostDtoBygger medDokumentreferanse(String dokumentreferanse) {
        DokumentDto dokumentDto = hentForsteDokument();
        dokumentDto.setDokumentreferanse(dokumentreferanse);
        return this;
    }

    public JournalpostDtoBygger medDokumentType(String dokumentType) {
        DokumentDto dokumentDto = hentForsteDokument();
        dokumentDto.setDokumentType(dokumentType);
        return this;
    }

    private DokumentDto hentForsteDokument() {
        if (journalpostDto.getDokumenter().isEmpty()) {
            DokumentDto dokumentDto = new DokumentDto();
            journalpostDto.setDokumenter(Collections.singletonList(dokumentDto));
        }

        return journalpostDto.getDokumenter().get(0);
    }

    public JournalpostDtoBygger medFagomrade(String fagomrade) {
        journalpostDto.setFagomrade(fagomrade);
        return this;
    }

    public JournalpostDtoBygger medGjelderBrukerId(String... brukerId) {
        journalpostDto.setGjelderBrukerId(asList(brukerId));
        return this;
    }

    public JournalpostDtoBygger medJournalforendeEnhet(String journalforendeEnhet) {
        journalpostDto.setJournalforendeEnhet(journalforendeEnhet);
        return this;
    }

    public JournalpostDtoBygger medJournalfortAv(String journalfortAv) {
        journalpostDto.setJournalfortAv(journalfortAv);
        return this;
    }

    public JournalpostDtoBygger medJournalfortDato(LocalDate date) {
        journalpostDto.setJournalfortDato(date);
        return this;
    }

    public JournalpostDtoBygger medJournalpostId(String journalpostId) {
        journalpostDto.setJournalpostId(journalpostId);
        return this;
    }

    public JournalpostDtoBygger medMottattDato(LocalDate date) {
        journalpostDto.setMottattDato(date);
        return this;
    }

    public JournalpostDtoBygger medSaksnummerBidrag(String saksnummerBidrag) {
        journalpostDto.setSaksnummerBidrag(saksnummerBidrag);
        return this;
    }

    public JournalpostDtoBygger medDokumenter(List<DokumentDto> dokumenter) {
        journalpostDto.setDokumenter(dokumenter);
        return this;
    }

    public JournalpostDtoBygger utenBrukerId() {
        medBrukerId = false;
        return this;
    }

    public JournalpostDtoBygger utenDokument() {
        medDokument = false;
        return this;
    }
}
