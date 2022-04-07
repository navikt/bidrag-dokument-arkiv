package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.arkiv.model.AvvikDetaljException
import no.nav.bidrag.dokument.arkiv.utils.DateUtils
import no.nav.bidrag.dokument.dto.AvvikType
import no.nav.bidrag.dokument.dto.Avvikshendelse
import java.time.LocalDate

object AvvikDetaljer {
    const val ENHETSNUMMER = "enhetsnummer"
    const val RETUR_DATO = "returDato"
    const val UTSENDINGSKANAL = "utsendingsKanal"
    const val SETT_STATUS_EKSPEDERT = "settStatusEkspedert"
    const val ENHETSNUMMER_GAMMELT = "gammeltEnhetsnummer"
    const val ENHETSNUMMER_NYTT = "nyttEnhetsnummer"
    const val FAGOMRADE = "fagomrade"
    const val FEILREGISTRER = "feilregistrer"
}

data class AvvikshendelseIntern(
    val avvikstype: AvvikType,
    val beskrivelse: String? = null,
    val saksbehandlersEnhet: String?,
    val journalpostId: Long = -1,
    var saksnummer: String? = null,
    private val detaljer: Map<String, String?> = HashMap()
) {
    val returDato: String get() = detaljer[AvvikDetaljer.RETUR_DATO] ?: throw AvvikDetaljException(AvvikDetaljer.RETUR_DATO)
    val enhetsnummer: String get() = detaljer[AvvikDetaljer.ENHETSNUMMER] ?: throw AvvikDetaljException(AvvikDetaljer.ENHETSNUMMER)
    val enhetsnummerGammelt: String get() = detaljer[AvvikDetaljer.ENHETSNUMMER] ?: throw AvvikDetaljException(AvvikDetaljer.ENHETSNUMMER_GAMMELT)
    val enhetsnummerNytt: String get() = detaljer[AvvikDetaljer.ENHETSNUMMER_NYTT] ?: throw AvvikDetaljException(AvvikDetaljer.ENHETSNUMMER_NYTT)
    val nyttFagomrade: String get() = detaljer[AvvikDetaljer.FAGOMRADE] ?: throw AvvikDetaljException(AvvikDetaljer.FAGOMRADE)
    val utsendingsKanal: String get() = detaljer[AvvikDetaljer.UTSENDINGSKANAL] ?: throw AvvikDetaljException(AvvikDetaljer.UTSENDINGSKANAL)
    val skalFeilregistreres: Boolean get() = (detaljer[AvvikDetaljer.FEILREGISTRER] ?: "false").toBoolean()
    val isBidragFagomrade: Boolean get() = nyttFagomrade == Fagomrade.BID.name || nyttFagomrade == Fagomrade.FAR.name

    constructor(avvikshendelse: Avvikshendelse, opprettetAvEnhetsnummer: String, journalpostId: Long) : this(
        avvikstype = AvvikType.valueOf(avvikshendelse.avvikType),
        beskrivelse = avvikshendelse.beskrivelse,
        saksbehandlersEnhet = opprettetAvEnhetsnummer,
        journalpostId = journalpostId,
        saksnummer = avvikshendelse.saksnummer,
        detaljer=avvikshendelse.detaljer
    )

    fun toOverforEnhetRequest() = OverforEnhetRequest(journalpostId, enhetsnummerNytt)
    fun toEndreFagomradeRequest() = toEndreFagomradeRequest(journalpostId)
    fun toEndreFagomradeRequest(journalpostId: Long) = EndreFagomradeRequest(journalpostId, nyttFagomrade)
    fun toKnyttTilGenerellSakRequest(fagomrade: String, bruker: Bruker) = KnyttTilGenerellSakRequest(journalpostId, OppdaterJournalpostRequest.Bruker(bruker.id, bruker.type), fagomrade)
    fun toLeggTilBegrunnelsePaaTittelRequest(tittel: String) = EndreTittelRequest(journalpostId, "$tittel ($beskrivelse)")
}

data class OverforEnhetRequest(private var journalpostId: Long, override var journalfoerendeEnhet: String?): OppdaterJournalpostRequest(journalpostId)
data class EndreFagomradeRequest(private var journalpostId: Long, override var tema: String?): OppdaterJournalpostRequest(journalpostId)
data class EndreTittelRequest(private var journalpostId: Long, override var tittel: String?): OppdaterJournalpostRequest(journalpostId)
data class KnyttTilGenerellSakRequest(private var journalpostId: Long, override var bruker: Bruker?, override var tema: String?, override var sak: Sak? = GenerellSak()): OppdaterJournalpostRequest(journalpostId)
data class InngaaendeTilUtgaaendeRequest(private var journalpostId: Long, override var tema: String?): OppdaterJournalpostRequest(journalpostId)
data class RegistrerReturRequest(private var journalpostId: Long, private var _datoRetur: LocalDate, private var _tilleggsopplysninger: TilleggsOpplysninger?): OppdaterJournalpostRequest(journalpostId) {
    init {
        datoRetur = DateUtils.formatDate(_datoRetur)
        tilleggsopplysninger = _tilleggsopplysninger
    }
}