package no.nav.bidrag.dokument.arkiv.service;

import no.nav.bidrag.dokument.arkiv.dto.BrukerDto;
import no.nav.bidrag.dokument.arkiv.dto.JoarkDokumentDto;
import no.nav.bidrag.dokument.arkiv.dto.JournalforingDto;
import no.nav.bidrag.dokument.dto.DokumentDto;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class JournalpostMapper {

    JournalpostDto fraJournalfoering(JournalforingDto journalforingDto) {
        JournalpostDto journalpostDto = new JournalpostDto();
        journalpostDto.setAvsenderNavn(journalforingDto.getAvsenderDto() != null ? journalforingDto.getAvsenderDto().getAvsender() : null);
        journalpostDto.setFagomrade(journalforingDto.getFagomrade());
        journalpostDto.setDokumentDato(journalforingDto.getDatoDokument());
        journalpostDto.setJournaltilstand(journalforingDto.getJournalTilstand());
        journalpostDto.setDokumenter(journalforingDto.getDokumenter().stream().map(this::tilDokumentDto).collect(Collectors.toList()));
        journalpostDto.setGjelderBrukerId(journalforingDto.getBrukere().stream().map(BrukerDto::getBrukerId).collect(Collectors.toList()));
        journalpostDto.setInnhold(journalforingDto.getInnhold());
        journalpostDto.setJournalforendeEnhet(journalforingDto.getJournalforendeEnhet());
        journalpostDto.setJournalfortAv(journalforingDto.getJournalfortAvNavn());
        journalpostDto.setJournalfortDato(journalforingDto.getDatoJournal());
        journalpostDto.setJournalpostId("JOARK-" + journalforingDto.getJournalpostId());
        journalpostDto.setMottattDato(journalforingDto.getDatoMottatt());
        journalpostDto.setSaksnummerGsak(journalforingDto.getArkivSak() != null ? journalforingDto.getArkivSak().getId() : null);

        return journalpostDto;
    }

    private DokumentDto tilDokumentDto(JoarkDokumentDto joarkDokumentDto) {
        DokumentDto dokumentDto = new DokumentDto();
        dokumentDto.setDokumentreferanse(joarkDokumentDto.getDokumentId());
        dokumentDto.setTittel(joarkDokumentDto.getTittel());
        dokumentDto.setDokumentType(joarkDokumentDto.getDokumentTypeId());

        return dokumentDto;
    }
}
