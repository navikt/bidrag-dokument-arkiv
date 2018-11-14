package no.nav.bidrag.dokument.arkiv.service;

import no.nav.bidrag.dokument.arkiv.dto.BrukerDto;
import no.nav.bidrag.dokument.arkiv.dto.JournalforingDto;
import no.nav.bidrag.dokument.dto.DokumentDto;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JournalpostMapper")
class JournalpostMapperTest {

    private JournalpostMapper journalpostMapper = new JournalpostMapper();

    @DisplayName("skal mappe fra JournalforingDto") @SuppressWarnings("ConstantConditions")
    @Test void skalMappeFraJournalforingDto() {
        JournalforingDto journalforingDto = new JournalforingBuilder()
                .withArkivSakId("10101")
                .withAvsender("U. A. N. Svarlig")
                .withBruker("06127412345")
                .withFagomrade("BID")
                .withDatoDokument(LocalDate.now().minusDays(3))
                .withDatoJournal(LocalDate.now().minusDays(1))
                .withDatoMottatt(LocalDate.now().minusDays(2))
                .withDokumentId("101")
                .withInnhold("...and know, something completely different...")
                .withJournalforendeEnhet("JUnit")
                .withJournalfortAvNavn("Dr. A. Cula")
                .withJournalpostId(101)
                .withDokumentTypeId("N")
                .get();

        JournalpostDto journalpostDto = journalpostMapper.fraJournalfoering(journalforingDto);
        assertThat(journalpostDto).isNotNull();

        assertAll(
                () -> assertThat(journalpostDto.getAvsenderNavn()).as("avsenderNavn").isEqualTo(journalforingDto.getAvsenderDto().getAvsender()),
                () -> assertThat(journalpostDto.getFagomrade()).as("fagomrade").isEqualTo(journalforingDto.getFagomrade()),
                () -> assertThat(journalpostDto.getDokumentDato()).as("dokumentdato").isEqualTo(journalforingDto.getDatoDokument()),
                () -> assertThat(journalpostDto.getDokumenter()).extracting(DokumentDto::getDokumentreferanse).as("dokumentreferanse")
                        .isEqualTo(singletonList(journalforingDto.getDokumenter().get(0).getDokumentId())),
                () -> assertThat(journalpostDto.getDokumenter()).extracting(DokumentDto::getDokumentType).as("dokumentType")
                        .isEqualTo(singletonList(journalforingDto.getDokumenter().get(0).getDokumentTypeId())),
                () -> assertThat(journalpostDto.getGjelderBrukerId()).as("gjelderBrukerId")
                        .isEqualTo(journalforingDto.getBrukere().stream().map(BrukerDto::getBrukerId).collect(toList())),
                () -> assertThat(journalpostDto.getInnhold()).as("innhold").isEqualTo(journalforingDto.getInnhold()),
                () -> assertThat(journalpostDto.getJournalforendeEnhet()).as("journalforendeEnhet").isEqualTo(journalforingDto.getJournalforendeEnhet()),
                () -> assertThat(journalpostDto.getJournalfortAv()).as("journalfortAv").isEqualTo(journalforingDto.getJournalfortAvNavn()),
                () -> assertThat(journalpostDto.getJournalfortDato()).as("journalfortDato").isEqualTo(journalforingDto.getDatoJournal()),
                () -> assertThat(journalpostDto.getJournalpostId()).as("journalpostId").isEqualTo("JOARK-" + journalforingDto.getJournalpostId()),
                () -> assertThat(journalpostDto.getMottattDato()).as("mottattDato").isEqualTo(journalforingDto.getDatoMottatt()),
                () -> assertThat(journalpostDto.getSaksnummerGsak()).as("saksnummerGsak").isEqualTo(journalforingDto.getArkivSak().getId())
        );
    }
}