package no.nav.bidrag.dokument.arkiv.kafka;

import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer;
import no.nav.bidrag.dokument.arkiv.model.Discriminator;
import no.nav.bidrag.dokument.arkiv.model.JournalpostHendelse;
import no.nav.bidrag.dokument.arkiv.model.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator;
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HendelseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HendelseListener.class);
    public static final String JOURNALPOST_HENDELSE_OPPRETT_OPPGAVE = "OPPRETT_OPPGAVE";
    @Autowired
    private HendelserProducer producer;
    @Autowired
    private ResourceByDiscriminator<SafConsumer> safConsumers;


    @KafkaListener(
            topics = "${TOPIC_JOURNALFOERING}",
            errorHandler = "hendelseErrorHandler")
    public void listen(@Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
        CorrelationId correlationId = CorrelationId.generateTimestamped("journalfoeringshendelse");
        MDC.put("correlationId", correlationId.get());
        Oppgavetema oppgavetema = new Oppgavetema(journalfoeringHendelseRecord);
        if (!oppgavetema.erOmhandlingAvBidrag()){
            LOGGER.debug("Oppgavetema omhandler ikke bidrag ");
            return;
        }

        registrerOppgaveForHendelse(journalfoeringHendelseRecord);
        MDC.clear();
    }

    private void registrerOppgaveForHendelse(
            @Payload JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
        Optional<HendelsesType> muligType = HendelsesType.from(journalfoeringHendelseRecord.getHendelsesType());
        LOGGER.info("Ny hendelse: {}", muligType);
        JournalpostHendelse journalpostHendelse = createJournalpostHendelse(journalfoeringHendelseRecord);
        muligType.ifPresent(
                hendelsesType -> {
                    switch (hendelsesType) {
                        case JOURNALPOST_MOTTAT, TEMA_ENDRET, MIDLERTIDIG_JOURNALFORT -> {
                            LOGGER.info("Journalpost hendelse {} med data {}", hendelsesType, journalfoeringHendelseRecord);
                            producer.publish(journalpostHendelse);
                        }
                    }

                }
        );

        if (muligType.isEmpty()) {
            LOGGER.error("Ingen implementasjon for {}", journalfoeringHendelseRecord.getHendelsesType());
        }
    }

    private JournalpostHendelse createJournalpostHendelse(JournalfoeringHendelseRecord journalfoeringHendelseRecord){
        var journalpostId = journalfoeringHendelseRecord.getJournalpostId();
        var journalpostOptional = Optional.ofNullable(safConsumers.get(Discriminator.SERVICE_USER).hentJournalpost(journalpostId));
        if (journalpostOptional.isEmpty()){
            throw new JournalpostIkkeFunnetException(String.format("Fant ikke journalpost med id %s", journalpostId));
        }

        var journalpost = journalpostOptional.get();
        JournalpostHendelse journalpostHendelse = new JournalpostHendelse(
                journalfoeringHendelseRecord.getJournalpostId(),
            JOURNALPOST_HENDELSE_OPPRETT_OPPGAVE
        );
        journalpostHendelse.addDetaljer("tema", journalfoeringHendelseRecord.getTemaNytt());
        journalpostHendelse.addDetaljer("aktoerId", journalpost.getBruker().getId());
        journalpostHendelse.addDetaljer("journalforendeEnhet", journalpost.getJournalforendeEnhet());
        return journalpostHendelse;

    }
}
