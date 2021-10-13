package no.nav.bidrag.dokument.arkiv.hendelser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.dto.PersonResponse;
import no.nav.bidrag.dokument.arkiv.kafka.HendelseListener;
import no.nav.bidrag.dokument.arkiv.kafka.HendelsesType;
import no.nav.bidrag.dokument.arkiv.kafka.MottaksKanal;
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

@ActiveProfiles("test")
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
class JournalpostKafkaEventProducerTest {

  @MockBean
  private KafkaTemplate<String, String> kafkaTemplateMock;

  @MockBean
  private SafConsumer safConsumer;
  @MockBean
  private PersonConsumer personConsumer;
  @Autowired
  private HendelseListener hendelseListener;

  @Value("${TOPIC_JOURNALPOST}")
  private String topicJournalpost;

  @Test
  @DisplayName("skal publisere journalpost hendelser")
  void skalPublisereJournalpostHendelser() {
    var journalpostId = 123213L;
    var expectedJoarkJournalpostId = "JOARK-"+journalpostId;
    var brukerId = "555555";
    var jfEnhet = "4833";
    mockSafResponse(journalpostId, brukerId,"AKTOERID", jfEnhet);

    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setJournalpostId(journalpostId);
    record.setHendelsesType(HendelsesType.JOURNALPOST_MOTTAT.getHendelsesType());
    record.setTemaNytt("BID");
    record.setMottaksKanal(MottaksKanal.NAV_NO.name());
    hendelseListener.listen(record);

    var jsonCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplateMock).send(eq(topicJournalpost), eq(expectedJoarkJournalpostId), jsonCaptor.capture());
    verify(safConsumer).hentJournalpost(eq(journalpostId));

    var hendelse = """
        "hendelse":"OPPRETT_OPPGAVE"
        """.trim();

    var expectedJournalpostId = String.format("""
            "journalpostId":"%s"
            """.trim(), expectedJoarkJournalpostId);

    var aktoerId = String.format("""
            "aktoerId":"%s"
            """.trim(), brukerId);

    assertThat(jsonCaptor.getValue()).containsSequence(hendelse).containsSequence(expectedJournalpostId).containsSequence(aktoerId);
  }

  @Test
  @DisplayName("skal publisere journalpost hendelser med aktørid når saf returnerer FNR")
  void skalPublisereJournalpostHendelserWhenSafReturnFNR() {
    var journalpostId = 123213L;
    var expectedJoarkJournalpostId = "JOARK-"+journalpostId;
    var brukerIdFnr = "555555";
    var brukerIdAktorId = "213213323";
    var jfEnhet = "4833";
    mockSafResponse(journalpostId, brukerIdFnr, "FNR", jfEnhet);
    when(personConsumer.hentPerson(any())).thenReturn(HttpResponse.from(HttpStatus.OK, new PersonResponse(brukerIdFnr, brukerIdAktorId)));

    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setJournalpostId(journalpostId);
    record.setHendelsesType(HendelsesType.JOURNALPOST_MOTTAT.getHendelsesType());
    record.setTemaNytt("BID");
    record.setMottaksKanal(MottaksKanal.NAV_NO.name());
    hendelseListener.listen(record);

    var jsonCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplateMock).send(eq(topicJournalpost), eq(expectedJoarkJournalpostId), jsonCaptor.capture());
    verify(safConsumer).hentJournalpost(eq(journalpostId));

    var hendelse = """
        "hendelse":"OPPRETT_OPPGAVE"
        """.trim();

    var expectedJournalpostId = String.format("""
            "journalpostId":"%s"
            """.trim(), expectedJoarkJournalpostId);

    var aktoerId = String.format("""
            "aktoerId":"%s"
            """.trim(), brukerIdAktorId);

    assertThat(jsonCaptor.getValue()).containsSequence(hendelse).containsSequence(expectedJournalpostId).containsSequence(aktoerId);
  }

  @Test
  @DisplayName("skal ignorere hendelse hvis tema ikke er BID")
  void shouldIgnoreWhenHendelseIsNotBID() {
    var journalpostId1 = 123213L;
    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setJournalpostId(journalpostId1);
    record.setHendelsesType(HendelsesType.JOURNALPOST_MOTTAT.getHendelsesType());
    record.setTemaNytt("NOT BID");
    record.setMottaksKanal(MottaksKanal.NAV_NO.name());
    hendelseListener.listen(record);

    verify(kafkaTemplateMock, never()).send(any(), any(), any());
  }

  @Test
  @DisplayName("skal ignorere hendelse hvis hendelse ikke er mottatt")
  void shouldIgnoreWhenHendelseIsNotMottatt() {
    var journalpostId1 = 123213L;
    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setJournalpostId(journalpostId1);
    record.setHendelsesType("MidlertidigJournalført");
    record.setTemaNytt("BID");
    record.setMottaksKanal(MottaksKanal.NAV_NO.name());
    hendelseListener.listen(record);

    verify(kafkaTemplateMock, never()).send(any(), any(), any());
  }

  @Test
  @DisplayName("skal ignorere hendelse hvis mottakskanal ikke er NAV_NO")
  void shouldIgnoreWhenMottakskanalIsNotNavNo() {
    var journalpostId1 = 123213L;
    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setJournalpostId(journalpostId1);
    record.setHendelsesType(HendelsesType.JOURNALPOST_MOTTAT.getHendelsesType());
    record.setTemaNytt("BID");
    record.setMottaksKanal("NAV_NO_CHAT");
    hendelseListener.listen(record);

    verify(kafkaTemplateMock, never()).send(any(), any(), any());
  }

  private void mockSafResponse(Long journalpostId, String brukerId, String brukerType, String jfEnhet){
    var safJournalpostResponse = new Journalpost();
    safJournalpostResponse.setBruker(new Bruker(brukerId, brukerType));
    safJournalpostResponse.setJournalforendeEnhet(jfEnhet);
    when(safConsumer.hentJournalpost(journalpostId)).thenReturn(safJournalpostResponse);
  }
}
