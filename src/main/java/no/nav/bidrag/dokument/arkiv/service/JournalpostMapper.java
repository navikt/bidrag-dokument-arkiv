package no.nav.bidrag.dokument.arkiv.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import no.nav.bidrag.dokument.dto.AktorDto;
import no.nav.bidrag.dokument.dto.DokumentDto;
import no.nav.bidrag.dokument.dto.JournalpostDto;
import org.springframework.stereotype.Component;

@Component
public class JournalpostMapper {

  private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  JournalpostDto tilJournalpostDto(Map<String, Object> journalpostMap) {
    var avsenderMottakerMap = asMap(asMap(journalpostMap).get("avsenderMottaker"));
    var dokumenttype = asString(journalpostMap.get("journalposttype"));
    var sakMap = asMap(journalpostMap.get("sak"));
    var journalpostDto = new JournalpostDto();

    journalpostDto.setAvsenderNavn(asString(avsenderMottakerMap.get("navn")));
    journalpostDto.setDokumenter(tilDokumenter(journalpostMap.get("dokumenter"), dokumenttype));
    journalpostDto.setDokumentType(dokumenttype);
    journalpostDto.setFagomrade(asString(journalpostMap.get("tema")));
    journalpostDto.setInnhold(asString(journalpostMap.get("tittel")));
    journalpostDto.setGjelderAktor(tilAktorDto(journalpostMap.get("bruker")));
    journalpostDto.setJournalforendeEnhet(asString(journalpostMap.get("journalforendeEnhet")));
    journalpostDto.setJournalfortAv(asString(journalpostMap.get("journalfortAvNavn")));
    journalpostDto.setJournalfortDato(LocalDate.parse(asString(journalpostMap.get("datoOpprettet")), DATE_PATTERN));
    journalpostDto.setJournalpostId(asString(journalpostMap.get("journalpostId")));
    journalpostDto.setJournalstatus(asString(journalpostMap.get("journalstatus")));
    journalpostDto.setMottattDato(hentDatoRegistrert(journalpostMap.get("relevanteDatoer")));
    journalpostDto.setSaksnummer(asString(sakMap.get("arkivsaksnummer")));

    return journalpostDto;
  }

  private AktorDto tilAktorDto(Object bruker) {
    if (bruker == null) {
      return null;
    }

    return new AktorDto(asString(asMap(bruker).get("id")), asString(asMap(bruker).get("type")));
  }

  @SuppressWarnings("unchecked")
  private List<DokumentDto> tilDokumenter(Object dokumenter, String dokumenttype) {
    if (dokumenter == null) {
      return Collections.emptyList();
    }

    return ((List<Map<String, Object>>) dokumenter).stream()
        .map(dokumentMap -> tilDokumentDto(dokumentMap, dokumenttype))
        .collect(Collectors.toList());
  }

  private DokumentDto tilDokumentDto(Map<String, Object> dokumentMap, String dokumenttype) {
    return new DokumentDto("", dokumenttype, asString(dokumentMap.get("tittel")));
  }

  @SuppressWarnings("unchecked")
  private LocalDate hentDatoRegistrert(Object relevanteDatoer) {
    if (relevanteDatoer == null) {
      return null;
    }

    return ((List<Map<String, Object>>) relevanteDatoer).stream()
        .map(this::hentDatoRegistrert)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private LocalDate hentDatoRegistrert(Map<String, Object> relevantDatoMap) {
    if ("DATO_REGISTRERT".equals(relevantDatoMap.get("datotype"))) {
      return LocalDate.parse(asString(relevantDatoMap.get("dato")), DATE_PATTERN);
    }

    return null;
  }

  private String asString(Object object) {
    return String.valueOf(object);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(Object object) {
    if (object == null) {
      return Collections.emptyMap();
    }

    if (!(object instanceof Map)) {
      throw new IllegalStateException("Object is not a map: " + object);
    }

    return (Map<String, Object>) object;
  }
}
