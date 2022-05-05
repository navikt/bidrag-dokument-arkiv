package no.nav.bidrag.dokument.arkiv.service;

import com.google.common.io.ByteSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.Dokument;
import no.nav.bidrag.dokument.arkiv.dto.Journalpost;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.bidrag.dokument.dto.JournalpostIkkeFunnetException;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class DokumentService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DokumentService.class);

  private final SafConsumer safConsumer;
  private final JournalpostService journalpostService;

  public DokumentService(
      ResourceByDiscriminator<SafConsumer> safConsumers,
      ResourceByDiscriminator<JournalpostService> journalpostServices) {
    this.journalpostService = journalpostServices.get(Discriminator.REGULAR_USER);
    this.safConsumer = safConsumers.get(Discriminator.REGULAR_USER);
  }

  public ResponseEntity<byte[]> hentDokument(Long journalpostId, String dokumentReferanse, boolean resizeToA4) {
    ResponseEntity<byte[]> response = hentDokument(journalpostId, dokumentReferanse);

    if (resizeToA4){
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_PDF)
          .header(HttpHeaders.CONTENT_DISPOSITION, response.getHeaders().getContentDisposition().toString())
          .body(new PDFDokumentProcessor().konverterAlleSiderTilA4(response.getBody()));
    }

    return response;
  }

  private ResponseEntity<byte[]> hentDokument(Long journalpostId, String dokumentReferanse){
    if (Objects.isNull(dokumentReferanse)) {
      return hentAlleDokumenter(journalpostId);
    }

    return safConsumer.hentDokument(journalpostId, Long.valueOf(dokumentReferanse));
  }
  public ResponseEntity<byte[]> hentAlleDokumenter(Long journalpostId){
    Journalpost journalpost = journalpostService.hentJournalpost(journalpostId)
        .orElseThrow(() -> new JournalpostIkkeFunnetException("Fant ikke journalpost med id lik " + journalpostId));
    var mergedFileName = String.format("%s_ARKIV.pdf", journalpostId);
    var mergedFileNameTmp = "/tmp/"+mergedFileName;
    try {
      mergeAlleDokumenter(journalpostId, journalpost.getDokumenter(), mergedFileNameTmp);
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_PDF)
          .header(HttpHeaders.CONTENT_DISPOSITION, String.format("inline; filename=%s", mergedFileName))
          .body(getByteDataAndDeleteFile(mergedFileNameTmp));
    } catch (Exception e){
      LOGGER.error("Det skjedde en feil ved henting av dokument for journalpost {}", journalpostId, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  private void mergeAlleDokumenter(Long journalpostId, List<Dokument> dokumentList, String mergedFileName) throws IOException {
    PDFMergerUtility mergedDocument = new PDFMergerUtility();
    mergedDocument.setDestinationFileName(mergedFileName);
    for (var dokument: dokumentList){
      var dokumentResponse = hentDokument(journalpostId, dokument.getDokumentInfoId());
      var dokumentInputStream = ByteSource.wrap(dokumentResponse.getBody()).openStream();
      mergedDocument.addSource(dokumentInputStream);
      dokumentInputStream.close();
    }
    mergedDocument.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
  }

  private byte[] getByteDataAndDeleteFile(String filename) throws IOException {
    var file = new File(filename);
    try {
      return fileToByte(new File(filename));
    } finally {
      file.delete();
    }
  }

  private byte[] fileToByte(File file) throws IOException {
      FileInputStream inputStream = new FileInputStream(file);
      byte[] byteArray = new byte[(int)file.length()];
      inputStream.read(byteArray);
      inputStream.close();
      return byteArray;
  }
}
