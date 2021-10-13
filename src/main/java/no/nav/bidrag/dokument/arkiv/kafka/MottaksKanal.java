package no.nav.bidrag.dokument.arkiv.kafka;

import java.util.Arrays;
import java.util.Optional;

public enum MottaksKanal {
    NAV_NO("NAV_NO");

    private String mottaksKanal;

    MottaksKanal(String mottaksKanal) {
        this.mottaksKanal = mottaksKanal;
    }

    private boolean erAv(CharSequence hendelsesType) {
        return hendelsesType != null && String.valueOf(hendelsesType).compareToIgnoreCase(this.mottaksKanal) == 0;
    }

    public static Optional<MottaksKanal> from(CharSequence mottaksKanal) {
        return Arrays.stream(values())
                .filter(enumeration -> enumeration.erAv(mottaksKanal))
                .findFirst();
    }
}
