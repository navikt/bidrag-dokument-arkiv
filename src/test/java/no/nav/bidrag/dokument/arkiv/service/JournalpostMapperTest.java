package no.nav.bidrag.dokument.arkiv.service;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JournalpostMapper")
class JournalpostMapperTest {

  private JournalpostMapper journalpostMapper = new JournalpostMapper();

  @Test
  @DisplayName("skal mappe journalført dato fra en streng")
  void skalMappeJournalfortDatoFraStreng() {
    var journalpostMap = new HashMap<String, Object>();
    journalpostMap.put("datoOpprettet", "2008-12-06T00:00");

    JournalpostDto journalpostDto = journalpostMapper.tilJournalpostDto(journalpostMap);

    assertThat(journalpostDto.getJournalfortDato()).isEqualTo(LocalDate.of(2008, 12, 6));
  }

}
