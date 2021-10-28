package no.nav.bidrag.dokument.arkiv.hendelser;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkivConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.GeografiskTilknytningResponse;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostResponse;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerInfoResponse;
import no.nav.bidrag.dokument.arkiv.kafka.HendelseListener;
import no.nav.bidrag.dokument.arkiv.kafka.HendelsesType;
import no.nav.bidrag.dokument.arkiv.kafka.MottaksKanal;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
class JournalpostKafkaEventProducerTest {

  @MockBean
  private KafkaTemplate<String, String> kafkaTemplateMock;

  @MockBean
  private SafConsumer safConsumer;
  @MockBean
  private PersonConsumer personConsumer;
  @MockBean
  private DokarkivConsumer dokarkivConsumer;
  @MockBean
  private BidragOrganisasjonConsumer organisasjonConsumer;
  @Autowired
  private HendelseListener hendelseListener;

  @Value("${TOPIC_JOURNALPOST}")
  private String topicJournalpost;

  @Test
  @DisplayName("skal publisere journalpost hendelser")
  void skalPublisereJournalpostHendelser() {
    var journalpostId = 123213L;
    var expectedJoarkJournalpostId = "JOARK-" + journalpostId;
    var brukerId = "555555";
    var jfEnhet = "4833";
    mockSafResponse(journalpostId, brukerId, "AKTOERID", jfEnhet);
    when(dokarkivConsumer.endre(any())).thenReturn(HttpResponse.from(HttpStatus.OK, new OppdaterJournalpostResponse(journalpostId)));
    when(organisasjonConsumer.hentGeografiskEnhet(any())).thenReturn(HttpResponse.from(HttpStatus.OK, new GeografiskTilknytningResponse("4806", "navn")));
    when(organisasjonConsumer.hentSaksbehandlerInfo(any())).thenReturn(HttpResponse.from(HttpStatus.OK, new SaksbehandlerInfoResponse("123213", "navn")));

    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setHendelsesId("TEST_HENDELSE_ID");
    record.setJournalpostId(journalpostId);
    record.setHendelsesType(HendelsesType.JOURNALPOST_MOTTATT.getHendelsesType());
    record.setTemaNytt("BID");
    record.setMottaksKanal(MottaksKanal.NAV_NO.name());
    hendelseListener.listen(record);

    var jsonCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplateMock).send(eq(topicJournalpost), eq(expectedJoarkJournalpostId), jsonCaptor.capture());
    verify(safConsumer).hentJournalpost(eq(journalpostId));

    var expectedJournalpostId = """
        "journalpostId":"%s"
        """.formatted(expectedJoarkJournalpostId).trim();

    var aktoerId = """
        "aktorId":"%s"
        """.formatted(brukerId).trim();

    var correlationIdString = CorrelationId.fetchCorrelationIdForThread();
    var correlationId = """
        "correlationId":"%s"
        """.formatted(correlationIdString).trim();

    assertThat(jsonCaptor.getValue()).containsSequence(expectedJournalpostId).containsSequence(aktoerId).containsSequence(correlationId);
  }

  @Test
  @DisplayName("skal publisere journalpost hendelser med aktørid når saf returnerer FNR")
  void skalPublisereJournalpostHendelserWhenSafReturnFNR() {
    var journalpostId = 123213L;
    var expectedJoarkJournalpostId = "JOARK-" + journalpostId;
    var brukerIdFnr = "555555";
    var brukerIdAktorId = "213213323";
    var jfEnhet = "4833";
    mockSafResponse(journalpostId, brukerIdFnr, "FNR", jfEnhet);
    when(dokarkivConsumer.endre(any())).thenReturn(HttpResponse.from(HttpStatus.OK, new OppdaterJournalpostResponse(journalpostId)));
    when(personConsumer.hentPerson(any())).thenReturn(HttpResponse.from(HttpStatus.OK, new PersonResponse(brukerIdFnr, brukerIdAktorId)));
    when(organisasjonConsumer.hentGeografiskEnhet(any())).thenReturn(HttpResponse.from(HttpStatus.OK, new GeografiskTilknytningResponse("4806", "navn")));
    when(organisasjonConsumer.hentSaksbehandlerInfo(any())).thenReturn(HttpResponse.from(HttpStatus.OK, new SaksbehandlerInfoResponse("123213", "navn")));
    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setJournalpostId(journalpostId);
    record.setHendelsesType(HendelsesType.JOURNALPOST_MOTTATT.getHendelsesType());
    record.setTemaNytt("BID");
    record.setMottaksKanal(MottaksKanal.NAV_NO.name());
    hendelseListener.listen(record);

    var jsonCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplateMock).send(eq(topicJournalpost), eq(expectedJoarkJournalpostId), jsonCaptor.capture());
    verify(safConsumer).hentJournalpost(eq(journalpostId));

    var expectedJournalpostId = String.format("""
        "journalpostId":"%s"
        """.trim(), expectedJoarkJournalpostId);

    var aktoerId = String.format("""
        "aktorId":"%s"
        """.trim(), brukerIdAktorId);

    assertThat(jsonCaptor.getValue()).containsSequence(expectedJournalpostId).containsSequence(aktoerId);
  }

  @Test
  @DisplayName("skal ignorere hendelse hvis tema ikke er BID")
  void shouldIgnoreWhenHendelseIsNotBID() {
    var journalpostId1 = 123213L;
    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setJournalpostId(journalpostId1);
    record.setHendelsesType(HendelsesType.JOURNALPOST_MOTTATT.getHendelsesType());
    record.setTemaNytt("NOT BID");
    record.setMottaksKanal(MottaksKanal.NAV_NO.name());
    hendelseListener.listen(record);

    verify(kafkaTemplateMock, never()).send(any(), any(), any());
  }

  @Test
  @DisplayName("skal feile med JournalpostIkkeFunnetException hvis SAF feiler")
  void shouldThrowWhenSafFails() {
    var journalpostId1 = 123213L;
    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setJournalpostId(journalpostId1);
    record.setHendelsesType("JournalpostMottatt");
    record.setTemaNytt("BID");
    record.setMottaksKanal(MottaksKanal.NAV_NO.name());
    assertThatExceptionOfType(JournalpostIkkeFunnetException.class)
        .isThrownBy(() -> hendelseListener.listen(record))
        .withMessage("Fant ikke journalpost med id %s".formatted(journalpostId1));

  }

  private void mockSafResponse(Long journalpostId, String brukerId, String brukerType, String jfEnhet) {
    var safJournalpostResponse = new Journalpost();
    safJournalpostResponse.setJournalpostId(journalpostId.toString());
    safJournalpostResponse.setBruker(new Bruker(brukerId, brukerType));
    safJournalpostResponse.setJournalforendeEnhet(jfEnhet);
    when(safConsumer.hentJournalpost(journalpostId)).thenReturn(safJournalpostResponse);
  }
}
