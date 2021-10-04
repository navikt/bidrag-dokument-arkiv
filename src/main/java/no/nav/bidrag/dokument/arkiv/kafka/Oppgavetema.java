package no.nav.bidrag.dokument.arkiv.kafka;

import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;

public class Oppgavetema {

    private final String gammelt;
    private final String nytt;

    Oppgavetema(JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
        gammelt = String.valueOf(journalfoeringHendelseRecord.getTemaGammelt());
        nytt = String.valueOf(journalfoeringHendelseRecord.getTemaNytt());
    }

    public boolean erOmhandlingAvBidrag() {
        return HendelseListener.BEHANDLINGSTEMA_BIDRAG.equals(gammelt) || HendelseListener.BEHANDLINGSTEMA_BIDRAG.equals(nytt);
    }
}
