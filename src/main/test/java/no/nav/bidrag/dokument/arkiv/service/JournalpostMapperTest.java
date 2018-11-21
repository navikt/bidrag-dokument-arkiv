package no.nav.bidrag.dokument.arkiv.service;

import no.nav.bidrag.dokument.dto.DokumentDto;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import no.nav.dok.tjenester.journalfoerinngaaende.Bruker;
import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("JournalpostMapper")
class JournalpostMapperTest {

    private JournalpostMapper journalpostMapper = new JournalpostMapper();

    @DisplayName("skal mappe fra GetJournalpostResponse")
    @Test void skalMappeFraGetJournalpostResponse() {
        GetJournalpostResponse getJournalpostResponse = new GetJournalpostResponseBuilder()
                .withArkivSakId("10101")
                .withArkivSakSystem("BID")
                .withAvsender("U. A. N. Svarlig")
                .withBruker("06127412345")
                .withForsendelseMottatt(LocalDate.now())
                .withDokumentId("101")
                .withTittel("...and know, something completely different...")
                .withJournalfEnhet("JUnit")
                .withDokumentTypeId("N")
                .get();

        JournalpostDto journalpostDto = journalpostMapper.fra(getJournalpostResponse, 101);
        assertThat(journalpostDto).isNotNull();

        assertAll(
                () -> assertThat(journalpostDto.getAvsenderNavn()).as("avsenderNavn").isEqualTo(getJournalpostResponse.getAvsender().getNavn()),
                () -> assertThat(journalpostDto.getFagomrade()).as("fagomrade").isEqualTo(getJournalpostResponse.getArkivSak().getArkivSakSystem()),
                () -> assertThat(journalpostDto.getDokumentDato()).as("dokumentdato").isNull(),
                () -> assertThat(journalpostDto.getDokumenter()).extracting(DokumentDto::getDokumentreferanse).as("dokumentreferanse")
                        .isEqualTo(singletonList(getJournalpostResponse.getDokumentListe().get(0).getDokumentId())),
                () -> assertThat(journalpostDto.getDokumenter()).extracting(DokumentDto::getDokumentType).as("dokumentType")
                        .isEqualTo(singletonList(getJournalpostResponse.getDokumentListe().get(0).getDokumentTypeId())),
                () -> assertThat(journalpostDto.getGjelderBrukerId()).as("gjelderBrukerId")
                        .isEqualTo(getJournalpostResponse.getBrukerListe().stream().map(Bruker::getIdentifikator).collect(toList())),
                () -> assertThat(journalpostDto.getInnhold()).as("innhold").isEqualTo(getJournalpostResponse.getTittel()),
                () -> assertThat(journalpostDto.getJournalforendeEnhet()).as("journalforendeEnhet").isEqualTo(getJournalpostResponse.getJournalfEnhet()),
                () -> assertThat(journalpostDto.getJournalfortAv()).as("journalfortAv").isNull(),
                () -> assertThat(journalpostDto.getJournalfortDato()).as("journalfortDato").isEqualTo(LocalDate.now()),
                () -> assertThat(journalpostDto.getJournalpostId()).as("journalpostId").isEqualTo("JOARK-101"),
                () -> assertThat(journalpostDto.getMottattDato()).as("mottattDato").isEqualTo(LocalDate.now()),
                () -> assertThat(journalpostDto.getSaksnummer()).as("saksnummerGsak").isEqualTo("GSAK-" + getJournalpostResponse.getArkivSak().getArkivSakId())
        );
    }
}
