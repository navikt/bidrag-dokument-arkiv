package no.nav.bidrag.dokument.arkiv.service;

import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DokumentService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DokumentService.class);

  private final SafConsumer safConsumer;

  public DokumentService(
      ResourceByDiscriminator<SafConsumer> safConsumers) {
    this.safConsumer = safConsumers.get(Discriminator.REGULAR_USER);
  }

  public ResponseEntity<byte[]> hentDokument(Long journalpostId, String dokumentReferanse){
    LOGGER.info("Henter dokument med journalpostId={} og dokumentReferanse={}", journalpostId, dokumentReferanse);
    return safConsumer.hentDokument(journalpostId, Long.valueOf(dokumentReferanse));
  }

}
