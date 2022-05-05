package no.nav.bidrag.dokument.arkiv.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDFDokumentProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(PDFDokumentProcessor.class);

  private PDDocument originalDocument;
  private PDDocument convertedDocument;
  private PDFRenderer pdfRenderer;
  public byte[] konverterAlleSiderTilA4(byte[] dokumentFil) {
    var tempFile = new File("/tmp/tmpfile.pdf");
    try (PDDocument originalDocument = PDDocument.load(dokumentFil);) {
      this.originalDocument = originalDocument;
      this.convertedDocument = new PDDocument();
      this.pdfRenderer = new PDFRenderer(originalDocument);
      processPages();

      this.convertedDocument.setAllSecurityToBeRemoved(true);
      this.convertedDocument.save(tempFile);
      this.convertedDocument.close();
      this.originalDocument.close();
      return fileToByte(tempFile);
    } catch (Exception e) {
      LOGGER.error("Det skjedde en feil ved konverting av PDF dokument til A4", e);
      return dokumentFil;
    } finally {
      tempFile.delete();
    }
  }

  private boolean isPageSizeA4(PDPage pdPage){
    var a4PageMediaBox = PDRectangle.A4;
    var pageMediaBox = pdPage.getMediaBox();
    return isSameWithMargin(pageMediaBox.getHeight(), a4PageMediaBox.getHeight(), 1F) && isSameWithMargin(pageMediaBox.getWidth(), a4PageMediaBox.getWidth(), 1F);
  }

  private void processPages() throws IOException {
    for (int pageNumber = 0; pageNumber < originalDocument.getNumberOfPages(); pageNumber++) {
      var originalPage = originalDocument.getPage(pageNumber);
      var updatedPage = originalDocument.getPage(pageNumber);
      updatedPage.setRotation(0);
      if (!isPageSizeA4(originalPage)) {
        updatedPage = new PDPage();
        updatedPage.setMediaBox(PDRectangle.A4);
        updatedPage.setRotation(originalPage.getRotation());
        convertedDocument.addPage(updatedPage);
        convertPageToA4(updatedPage, pageNumber);
      } else {
        convertedDocument.addPage(updatedPage);
      }
    }
  }

  private void convertPageToA4(PDPage newPage, int pageNumber) throws IOException {
    Double renderScale = 4D;
    Double pageScale = 1 / renderScale;
    BufferedImage bufferedImage = pdfRenderer.renderImage(pageNumber, renderScale.floatValue());
    PDImageXObject pdImage = LosslessFactory.createFromImage(originalDocument, bufferedImage);
    Double heightScaled = pdImage.getHeight() * pageScale;
    Double widthScaled = pdImage.getWidth() * pageScale;

    try (PDPageContentStream contentStream = new PDPageContentStream(convertedDocument, newPage, PDPageContentStream.AppendMode.OVERWRITE, false, true)) {
      contentStream.drawImage(pdImage, newPage.getMediaBox().getLowerLeftX(), newPage.getMediaBox().getLowerLeftY(), PDRectangle.A4.getWidth(), PDRectangle.A4.getHeight());
    }
  }

  private boolean isSameWithMargin(Float val1, Float val2, Float margin){
    return Math.abs(val1-val2)<margin;
  }

  private byte[] fileToByte(File file) throws IOException {
    FileInputStream inputStream = new FileInputStream(file);
    byte[] byteArray = new byte[(int)file.length()];
    inputStream.read(byteArray);
    inputStream.close();
    return byteArray;
  }
}
