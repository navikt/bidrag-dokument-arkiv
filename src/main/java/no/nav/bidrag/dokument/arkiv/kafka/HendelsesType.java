package no.nav.bidrag.dokument.arkiv.kafka;

import java.util.Arrays;
import java.util.Optional;

public enum HendelsesType {
    MIDLERTIDIG_JOURNALFORT("MidlertidigJournalført"),
    TEMA_ENDRET("TemaEndret");

    private String hendelsesType;

    HendelsesType(String hendelsesType) {
        this.hendelsesType = hendelsesType;
    }

    private boolean erAv(CharSequence hendelsesType) {
        return hendelsesType != null && String.valueOf(hendelsesType).compareToIgnoreCase(this.hendelsesType) == 0;
    }

    public static Optional<HendelsesType> from(CharSequence hendelsesType) {
        return Arrays.stream(values())
                .filter(enumeration -> enumeration.erAv(hendelsesType))
                .findFirst();
    }
}
