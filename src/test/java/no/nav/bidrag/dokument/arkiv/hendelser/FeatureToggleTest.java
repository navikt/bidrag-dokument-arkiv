package no.nav.bidrag.dokument.arkiv.hendelser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import no.nav.bidrag.dokument.arkiv.BidragDokumentArkivLocal;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Bruker;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.kafka.HendelseListener;
import no.nav.bidrag.dokument.arkiv.kafka.HendelsesType;
import no.nav.bidrag.dokument.arkiv.kafka.MottaksKanal;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = { "FEATURE_ENABLED=NONE" },
    classes = BidragDokumentArkivLocal.class
)
@ActiveProfiles("test")
public class FeatureToggleTest {
  @MockBean
  private KafkaTemplate<String, String> kafkaTemplateMock;

  @Autowired
  private HendelseListener hendelseListener;

  @MockBean
  private SafConsumer safConsumer;

  @Test
  @DisplayName("skal ignorere hendelse hvis hendelse ikke er enablet")
  void shouldIgnoreWhenHendelseWhenFeatureToggleIsOff() {
    var journalpostId = 123213L;
    var brukerId = "555555";
    var jfEnhet = "4833";
    mockSafResponse(journalpostId, brukerId,"AKTOERID", jfEnhet);

    JournalfoeringHendelseRecord record = new JournalfoeringHendelseRecord();
    record.setJournalpostId(journalpostId);
    record.setHendelsesType(HendelsesType.JOURNALPOST_MOTTATT.getHendelsesType());
    record.setTemaNytt("BID");
    record.setMottaksKanal(MottaksKanal.NAV_NO.name());
    hendelseListener.listen(record);

    verify(kafkaTemplateMock, never()).send(any(), any(), any());
    verify(safConsumer).hentJournalpost(eq(journalpostId));
  }

  private void mockSafResponse(Long journalpostId, String brukerId, String brukerType, String jfEnhet){
    var safJournalpostResponse = new Journalpost();
    safJournalpostResponse.setJournalpostId(journalpostId.toString());
    safJournalpostResponse.setBruker(new Bruker(brukerId, brukerType));
    safJournalpostResponse.setJournalforendeEnhet(jfEnhet);
    when(safConsumer.hentJournalpost(journalpostId)).thenReturn(safJournalpostResponse);
  }
}
