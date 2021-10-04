package no.nav.bidrag.dokument.arkiv.hendelser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.kafka.HendelseListener;
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static no.nav.bidrag.dokument.arkiv.kafka.HendelsesType.JOURNALPOST_MOTTAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(classes = BidragDokumentArkivLocal.class)
class JournalpostKafkaEventProducerTest {

  @MockBean
  private KafkaTemplate<String, String> kafkaTemplateMock;

  @MockBean
  private SafConsumer safConsumer;

  @Autowired
  private HendelseListener hendelseListener;

  @Value("${TOPIC_JOURNALPOST}")
  private String topicJournalpost;

  @Test
  @DisplayName("skal publisere journalpost hendelser")
  void skalPublisereJournalpostHendelser() {
    var journalpostId1 = 123213L;
    var brukerId = "555555";
    var jfEnhet = "4833";
    mockSafResponse(journalpostId1, brukerId, jfEnhet);

    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setJournalpostId(journalpostId1);
    record.setHendelsesType("JournalpostMottatt");
    record.setTemaNytt("BID");
    hendelseListener.listen(record);

    var jsonCaptor = ArgumentCaptor.forClass(String.class);
    verify(kafkaTemplateMock).send(eq(topicJournalpost), eq(String.valueOf(journalpostId1)), jsonCaptor.capture());
    verify(safConsumer).hentJournalpost(eq(journalpostId1));

    var hendelse = """
        "hendelse":"JournalpostMottatt"
        """.trim();

    var journalpostId = String.format("""
            "journalpostId":"%s"
            """.trim(), journalpostId1);

    var aktoerId = String.format("""
            "aktoerId":"%s"
            """.trim(), brukerId);

    var journalforendeEnhet = String.format("""
            "journalforendeEnhet":"%s"
            """.trim(), jfEnhet);

    assertThat(jsonCaptor.getValue()).containsSequence(hendelse).containsSequence(journalpostId).containsSequence(aktoerId).containsSequence(journalforendeEnhet);
  }

  @Test
  @DisplayName("skal ignorere hendelse hvis tema ikke er BID")
  void shouldIgnoreWhenHendelseIsNotBID() {
    var journalpostId1 = 123213L;
    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setJournalpostId(journalpostId1);
    record.setHendelsesType("JournalpostMottatt");
    record.setTemaNytt("NOT BID");
    hendelseListener.listen(record);

    verify(kafkaTemplateMock, never()).send(any(), any(), any());
  }

  private void mockSafResponse(Long journalpostId, String brukerId, String jfEnhet){
    var safJournalpostResponse = new Journalpost();
    safJournalpostResponse.setBruker(new Bruker(brukerId, "AKTOERID"));
    safJournalpostResponse.setJournalforendeEnhet(jfEnhet);
    when(safConsumer.hentJournalpost(journalpostId)).thenReturn(safJournalpostResponse);
  }
}
