package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.arkiv.model.AvvikDetaljException
import no.nav.bidrag.dokument.dto.AvvikType
import no.nav.bidrag.dokument.dto.Avvikshendelse

object AvvikDetaljer {
    const val ENHETSNUMMER = "enhetsnummer"
    const val ENHETSNUMMER_GAMMELT = "gammeltEnhetsnummer"
    const val ENHETSNUMMER_NYTT = "nyttEnhetsnummer"
    const val FAGOMRADE = "fagomrade"
}

data class AvvikshendelseIntern(
    val avvikstype: AvvikType,
    val beskrivelse: String? = null,
    val saksbehandlersEnhet: String?,
    val journalpostId: Long = -1,
    var saksnummer: String? = null,
    private val detaljer: Map<String, String?> = HashMap()
) {
    val enhetsnummer: String get() = detaljer[AvvikDetaljer.ENHETSNUMMER] ?: throw AvvikDetaljException(AvvikDetaljer.ENHETSNUMMER)
    val enhetsnummerGammelt: String get() = detaljer[AvvikDetaljer.ENHETSNUMMER] ?: throw AvvikDetaljException(AvvikDetaljer.ENHETSNUMMER_GAMMELT)
    val enhetsnummerNytt: String get() = detaljer[AvvikDetaljer.ENHETSNUMMER_NYTT] ?: throw AvvikDetaljException(AvvikDetaljer.ENHETSNUMMER_NYTT)
    val nyttFagomrade: String get() = detaljer[AvvikDetaljer.FAGOMRADE] ?: throw AvvikDetaljException(AvvikDetaljer.FAGOMRADE)

    constructor(avvikshendelse: Avvikshendelse, opprettetAvEnhetsnummer: String, journalpostId: Long) : this(
        avvikstype = AvvikType.valueOf(avvikshendelse.avvikType),
        beskrivelse = avvikshendelse.beskrivelse,
        saksbehandlersEnhet = opprettetAvEnhetsnummer,
        journalpostId = journalpostId,
        saksnummer = avvikshendelse.saksnummer,
        detaljer=avvikshendelse.detaljer
    )

    fun toOverforEnhetRequest() = OverforEnhetRequest(journalpostId, enhetsnummerNytt)
    fun toEndreFagomradeRequest() = EndreFagomradeRequest(journalpostId, nyttFagomrade)
    fun isNyttFagomradeAnnetEnnBIDellerFAR() = nyttFagomrade !== "BID" && nyttFagomrade !== "FAR"
}

data class OverforEnhetRequest(private var journalpostId: Long, override var journalfoerendeEnhet: String?): OppdaterJournalpostRequest(journalpostId)
data class EndreFagomradeRequest(private var journalpostId: Long, override var tema: String?): OppdaterJournalpostRequest(journalpostId)
data class InngaaendeTilUtgaaendeRequest(private var journalpostId: Long, override var tema: String?): OppdaterJournalpostRequest(journalpostId)