package no.nav.bidrag.dokument.arkiv.service;

import no.nav.bidrag.dokument.arkiv.consumer.JournalforingConsumer;
import no.nav.bidrag.dokument.arkiv.dto.JournalforingDto;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JournalpostService {

    private final JournalforingConsumer journalforingConsumer;
    private final JournalpostMapper journalpostMapper;

    public JournalpostService(JournalforingConsumer journalforingConsumer, JournalpostMapper journalpostMapper) {
        this.journalforingConsumer = journalforingConsumer;
        this.journalpostMapper = journalpostMapper;
    }

    public Optional<JournalpostDto> hentJournalpost(Integer journalpostId) {
        Optional<JournalforingDto> muligJournalforing = journalforingConsumer.hentJournalforing(journalpostId);
        return muligJournalforing.map(journalpostMapper::fraJournalfoering);
    }
}
