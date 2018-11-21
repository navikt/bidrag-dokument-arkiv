package no.nav.bidrag.dokument.arkiv.service;

import no.nav.bidrag.dokument.arkiv.consumer.JournalforingConsumer;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
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
        Optional<GetJournalpostResponse> muligJournalforing = journalforingConsumer.hentJournalforing(journalpostId);
        return muligJournalforing.map(journalpostResponse -> journalpostMapper.fra(journalpostResponse, journalpostId));
    }
}
