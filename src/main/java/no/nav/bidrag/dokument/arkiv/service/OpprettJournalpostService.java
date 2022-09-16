package no.nav.bidrag.dokument.arkiv.service;

import java.util.*;
import java.util.stream.Collectors;
import kotlin.Pair;
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.dto.*;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.HentJournalpostFeilet;
import no.nav.bidrag.dokument.arkiv.model.KunneIkkeJournalforeOpprettetJournalpost;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv.SECURE_LOGGER;
import static no.nav.bidrag.dokument.arkiv.dto.OpprettJournalpostKt.*;

@Service
public class OpprettJournalpostService {
  private static final Logger LOGGER = LoggerFactory.getLogger(OpprettJournalpostService.class);
  private final DokarkivConsumer dokarkivConsumer;
  private final SafConsumer safConsumer;
  private final  DokumentService dokumentService;
  private final  EndreJournalpostService endreJournalpostService;

  public OpprettJournalpostService(ResourceByDiscriminator<DokarkivConsumer> dokarkivConsumers, ResourceByDiscriminator<SafConsumer> safConsumers, DokumentService dokumentService, EndreJournalpostService endreJournalpostService) {
    this.dokarkivConsumer = dokarkivConsumers.get(Discriminator.REGULAR_USER);
    this.safConsumer = safConsumers.get(Discriminator.REGULAR_USER);
    this.dokumentService = dokumentService;
    this.endreJournalpostService = endreJournalpostService;
  }

  public JoarkOpprettJournalpostResponse opprettOgJournalforJournalpost(OpprettJournalpost request, List<String> knyttTilSaker){
    var tilknyttetSak = knyttTilSaker.get(0);
    request.medSak(tilknyttetSak);
    populerMedDokumenterByteData(request);
    validerKanOppretteJournalpost(request);

    var opprettJournalpostResponse =  dokarkivConsumer.opprett(request, true);
    LOGGER.info("Opprettet ny journalpost {}", opprettJournalpostResponse.getJournalpostId());
    SECURE_LOGGER.info("Opprettet ny journalpost {}", opprettJournalpostResponse);

    try {
      if (Boolean.FALSE.equals(opprettJournalpostResponse.getJournalpostferdigstilt())){
        String message = String.format("Kunne ikke journalføre journalpost %s med feilmelding %s", opprettJournalpostResponse.getJournalpostId(), opprettJournalpostResponse.getMelding());
        LOGGER.error(message);
        throw new KunneIkkeJournalforeOpprettetJournalpost(message);
      }

      var opprettetJournalpost = hentJournalpost(opprettJournalpostResponse.getJournalpostId());

      endreJournalpostService.lagreSaksbehandlerIdentForJournalfortJournalpost(opprettetJournalpost);
      knyttSakerTilOpprettetJournalpost(opprettetJournalpost, knyttTilSaker);
    } catch (Exception e){
      LOGGER.error("Etterbehandling av opprettet journalpost feilet (knytt til flere saker eller lagre saksbehandler ident). Fortsetter behandling da feilen må behandles manuelt.", e);
    }

    return opprettJournalpostResponse;
  }

  private Journalpost hentJournalpost(Long journalpostId){
    try {
      return safConsumer.hentJournalpost(journalpostId);
    } catch (Exception e){
      throw new HentJournalpostFeilet("Det skjedde en feil ved henting av journalpost", e);
    }
  }
  private void knyttSakerTilOpprettetJournalpost(Journalpost opprettetJournalpost, List<String> knyttTilSaker){
    knyttTilSaker
            .stream()
            .filter((saksnummer)->!saksnummer.equals(opprettetJournalpost.getSak().getFagsakId()))
            .forEach((saksnummer)-> endreJournalpostService.tilknyttTilSak(saksnummer, opprettetJournalpost));
  }

  public void populerMedDokumenterByteData(OpprettJournalpost request){
    if (request.getOriginalJournalpostId() != null){
      request.getDokumenter().forEach((dok)->{
        if (dok.getDokumentvarianter().isEmpty() && Strings.isNotEmpty(dok.getDokumentInfoId())){
          var dokument = hentDokument(request.getOriginalJournalpostId(), dok.getDokumentInfoId());
          dok.setDokumentvarianter(List.of(opprettDokumentVariant(null, dokument)));
        }
      });
    }
  }

  public JoarkOpprettJournalpostResponse dupliserUtgaaendeJournalpost(Journalpost journalpost, boolean removeDistribusjonMetadata){
    validerUtgaaendeJournalpostKanDupliseres(journalpost);

    var dokumenter = hentDokumenter(journalpost);
    var opprettJournalpostRequest = createOpprettJournalpostRequest(journalpost, dokumenter, removeDistribusjonMetadata);
    opprettJournalpostRequest.setEksternReferanseId(String.format("BID_duplikat_%s", journalpost.getJournalpostId()));
    var opprettJournalpostResponse =  dokarkivConsumer.opprett(opprettJournalpostRequest, true);
    LOGGER.info("Duplisert journalpost {}, ny journalpostId {}", journalpost.getJournalpostId(), opprettJournalpostResponse.getJournalpostId());
    return opprettJournalpostResponse;
  }

  private JoarkOpprettJournalpostRequest createOpprettJournalpostRequest(Journalpost journalpost, Map<String, byte[]> dokumenter, boolean removeDistribusjonMetadata){
    var opprettJournalpostRequest = new OpprettJournalpost().dupliser(journalpost, dokumenter);
    opprettJournalpostRequest.medKanal(null);
    if (removeDistribusjonMetadata){
      var tillegssopplysninger = new TilleggsOpplysninger();
      tillegssopplysninger.addAll(journalpost.getTilleggsopplysninger());
      tillegssopplysninger.removeDistribusjonMetadata();
      tillegssopplysninger.lockAllReturDetaljerLog();
      opprettJournalpostRequest.setTilleggsopplysninger(tillegssopplysninger);
    }
    return opprettJournalpostRequest;
  }

  private Map<String, byte[]> hentDokumenter(Journalpost journalpost){
    return journalpost.getDokumenter().stream()
            .map((dokument -> new Pair<>(dokument.getDokumentInfoId(),
                    dokumentService.hentDokument(journalpost.hentJournalpostIdLong(), dokument.getDokumentInfoId()).getBody())))
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
  }

  private byte[] hentDokument(Long journalpostId, String dokumentId){
    return dokumentService.hentDokument(journalpostId, dokumentId).getBody();
  }

}
