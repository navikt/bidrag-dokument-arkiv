package no.nav.bidrag.dokument.arkiv.model

import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import java.util.Arrays
import java.util.Optional

enum class JoarkHendelseType(val hendelsesType: String) {
    JOURNALPOST_MOTTATT("JournalpostMottatt"),
    TEMA_ENDRET("TemaEndret"),
    ENDELIG_JOURNALFORT("EndeligJournalført"),
    JOURNALPOST_UTGATT("JournalpostUtgått"),
    UKJENT("Ukjent");

    private fun erAv(hendelsesType: CharSequence?): Boolean {
        return hendelsesType != null && hendelsesType.toString().compareTo(this.hendelsesType, ignoreCase = true) == 0
    }

    companion object {
        fun from(hendelsesType: CharSequence?): Optional<JoarkHendelseType> {
            return Arrays.stream(values())
                .filter { enumeration: JoarkHendelseType -> enumeration.erAv(hendelsesType) }
                .findFirst()
        }
    }
}
class JournalpostTema internal constructor(journalfoeringHendelseRecord: JournalfoeringHendelseRecord) {
    val BEHANDLINGSTEMA_BIDRAG = "BID"
    val BEHANDLINGSTEMA_FAR = "FAR"
    private val gammelt: String
    private val nytt: String

    init {
        gammelt = journalfoeringHendelseRecord.temaGammelt ?: ""
        nytt = journalfoeringHendelseRecord.temaNytt ?: ""
    }

    fun erOmhandlingAvBidrag(): Boolean {
        return BEHANDLINGSTEMA_BIDRAG == gammelt || BEHANDLINGSTEMA_BIDRAG == nytt || BEHANDLINGSTEMA_FAR == gammelt || BEHANDLINGSTEMA_FAR == nytt
    }
}