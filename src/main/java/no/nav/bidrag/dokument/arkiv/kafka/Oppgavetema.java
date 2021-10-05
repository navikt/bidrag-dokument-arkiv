package no.nav.bidrag.dokument.arkiv.kafka;

import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord;

public class Oppgavetema {
    public final String BEHANDLINGSTEMA_BIDRAG = "BID";
    public final String BEHANDLINGSTEMA_FAR = "FAR";

    private final String gammelt;
    private final String nytt;

    Oppgavetema(JournalfoeringHendelseRecord journalfoeringHendelseRecord) {
        gammelt = String.valueOf(journalfoeringHendelseRecord.getTemaGammelt());
        nytt = String.valueOf(journalfoeringHendelseRecord.getTemaNytt());
    }

    public boolean erOmhandlingAvBidrag() {
        return BEHANDLINGSTEMA_BIDRAG.equals(gammelt) || BEHANDLINGSTEMA_BIDRAG.equals(nytt)
            || BEHANDLINGSTEMA_FAR.equals(gammelt) || BEHANDLINGSTEMA_FAR.equals(nytt);
    }
}
