package no.nav.bidrag.dokument.arkiv.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    journalpostDto.setJournalfortDato(hentDato(journalpostMap.get("datoOpprettet")));
    journalpostDto.setJournalpostId(asString(journalpostMap.get("journalpostId")));
    journalpostDto.setJournalstatus(asString(journalpostMap.get("journalstatus")));

    setMottattOgJournalfortDato(journalpostDto, asList(journalpostMap.get("relevanteDatoer")));

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

  private void setMottattOgJournalfortDato(JournalpostDto journalpostDto, List<Map<String, Object>> relevanteDatoer) {
    relevanteDatoer.forEach(map -> setRelevanteDatoer(journalpostDto, map));
  }

  private void setRelevanteDatoer(JournalpostDto journalpostDto, Map<String, Object> datoMap) {
    if ("DATO_REGISTRERT".equals(datoMap.get("datotype"))) {
      journalpostDto.setMottattDato(hentDato(datoMap.get("dato")));
    }

    if ("DATO_JOURNALFOERT".equals(datoMap.get("datotype"))) {
      journalpostDto.setJournalfortDato(hentDato(datoMap.get("dato")));
    }
  }

  private LocalDate hentDato(Object datoObjekt) {
    if (datoObjekt != null) {
      return LocalDate.parse(asString(datoObjekt), DATE_PATTERN);
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
      throw new IllegalStateException(String.format("Object is not a map: %s/%s ", object.getClass(), object));
    }

    return (Map<String, Object>) object;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> asList(Object object) {
    if (object == null) {
      return Collections.emptyList();
    }

    if (!(object instanceof List)) {
      throw new IllegalStateException(String.format("Object is not a list: %s/%s ", object.getClass(), object));
    }

    return (List<Map<String, Object>>) object;
  }
}
