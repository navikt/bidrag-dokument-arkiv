package no.nav.bidrag.dokument.arkiv.kafka;

import java.util.Arrays;
import java.util.Optional;

public enum HendelsesType {
    JOURNALPOST_MOTTATT("JournalpostMottatt"),
    TEMA_ENDRET("TemaEndret"),
    ENDELIG_JOURNALFORT("EndeligJournalført"),
    JOURNALPOST_UTGATT("JournalpostUtgått"),
    ENDRING("Endring");

    private final String hendelsesType;

    HendelsesType(String hendelsesType) {
        this.hendelsesType = hendelsesType;
    }

    public String getHendelsesType(){
        return hendelsesType;
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
