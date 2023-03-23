package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.arkiv.model.AvvikDetaljException
import no.nav.bidrag.dokument.arkiv.utils.DateUtils
import no.nav.bidrag.dokument.dto.AvvikType
import no.nav.bidrag.dokument.dto.Avvikshendelse
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse
import no.nav.bidrag.dokument.dto.FARSKAP_UTELUKKET_PREFIKS
import java.time.LocalDate

object AvvikDetaljer {
    const val ENHETSNUMMER = "enhetsnummer"
    const val RETUR_DATO = "returDato"
    const val UTSENDINGSKANAL = "utsendingsKanal"
    const val KNYTT_TIL_SAKER = "knyttTilSaker"
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
    var dokumenter: List<no.nav.bidrag.dokument.dto.DokumentDto>? = emptyList(),
    var adresse: DistribuerTilAdresse? = null,
    private val detaljer: Map<String, String?> = HashMap()
) {
    val returDato: String get() = detaljer[AvvikDetaljer.RETUR_DATO] ?: throw AvvikDetaljException(AvvikDetaljer.RETUR_DATO)
    val enhetsnummer: String get() = detaljer[AvvikDetaljer.ENHETSNUMMER] ?: throw AvvikDetaljException(AvvikDetaljer.ENHETSNUMMER)
    val enhetsnummerGammelt: String get() = detaljer[AvvikDetaljer.ENHETSNUMMER] ?: throw AvvikDetaljException(AvvikDetaljer.ENHETSNUMMER_GAMMELT)
    val enhetsnummerNytt: String get() = detaljer[AvvikDetaljer.ENHETSNUMMER_NYTT] ?: throw AvvikDetaljException(AvvikDetaljer.ENHETSNUMMER_NYTT)
    val nyttFagomrade: String get() = detaljer[AvvikDetaljer.FAGOMRADE] ?: throw AvvikDetaljException(AvvikDetaljer.FAGOMRADE)
    val utsendingsKanal: String get() = detaljer[AvvikDetaljer.UTSENDINGSKANAL] ?: throw AvvikDetaljException(AvvikDetaljer.UTSENDINGSKANAL)
    val knyttTilSaker: List<String>
        get() = detaljer[AvvikDetaljer.KNYTT_TIL_SAKER]?.split(",") ?: throw AvvikDetaljException(AvvikDetaljer.KNYTT_TIL_SAKER)
    val isBidragFagomrade: Boolean get() = nyttFagomrade == Fagomrade.BID.name || nyttFagomrade == Fagomrade.FAR.name

    constructor(avvikshendelse: Avvikshendelse, opprettetAvEnhetsnummer: String, journalpostId: Long) : this(
        avvikstype = AvvikType.valueOf(avvikshendelse.avvikType),
        beskrivelse = avvikshendelse.beskrivelse,
        saksbehandlersEnhet = opprettetAvEnhetsnummer,
        journalpostId = journalpostId,
        dokumenter = avvikshendelse.dokumenter,
        saksnummer = avvikshendelse.saksnummer,
        detaljer = avvikshendelse.detaljer,
        adresse = avvikshendelse.adresse
    )

    fun toLeggTiLFarskapUtelukketTilTittelRequest(journalpost: Journalpost) =
        EndreTittelRequest(journalpostId, "$FARSKAP_UTELUKKET_PREFIKS: ${journalpost.hentHoveddokument()?.tittel ?: journalpost.tittel}", journalpost)

    fun toEndreFagomradeRequest() = EndreFagomradeRequest(journalpostId, nyttFagomrade)
    fun toEndreFagomradeOgKnyttTilSakRequest(bruker: Bruker) =
        EndreFagomradeOgKnyttTilSakRequest(journalpostId, nyttFagomrade, OppdaterJournalpostRequest.Bruker(bruker.id, bruker.type))

    fun toEndreFagomradeJournalfortJournalpostRequest(journalpost: Journalpost) =
        EndreFagomradeJournalfortJournalpostRequest(journalpostId, journalpost)

    fun toKnyttTilGenerellSakRequest(fagomrade: String, bruker: Bruker) =
        EndreKnyttTilGenerellSakRequest(journalpostId, OppdaterJournalpostRequest.Bruker(bruker.id, bruker.type), fagomrade)

    fun toLeggTilBegrunnelsePaaTittelRequest(journalpost: Journalpost) =
        EndreTittelRequest(journalpostId, "${journalpost.hentHoveddokument()?.tittel ?: journalpost.tittel} ($beskrivelse)", journalpost)
}

data class OverforEnhetRequest(private var journalpostId: Long, override var journalfoerendeEnhet: String?) :
    OppdaterJournalpostRequest(journalpostId)

data class EndreFagomradeRequest(private var journalpostId: Long, override var tema: String?) : OppdaterJournalpostRequest(journalpostId)

data class EndreFagomradeOgKnyttTilSakRequest(
    private var journalpostId: Long,
    override var tema: String?,
    override var bruker: Bruker?,
    override var sak: Sak? = GenerellSak()
) : OppdaterJournalpostRequest(journalpostId)


data class EndreFagomradeJournalfortJournalpostRequest(private var journalpostId: Long, private var journalpost: Journalpost) :
    OppdaterJournalpostRequest(journalpostId) {
    init {
        journalpost.tilleggsopplysninger.setEndretTemaFlagg()
        tilleggsopplysninger = journalpost.tilleggsopplysninger
    }
}

data class OppdaterOriginalBestiltFlagg(private var journalpost: Journalpost) :
    OppdaterJournalpostRequest(journalpostId = journalpost.hentJournalpostIdLong()) {
    init {
        journalpost.tilleggsopplysninger.setOriginalBestiltFlagg()
        tilleggsopplysninger = journalpost.tilleggsopplysninger
    }
}

data class OpphevEndreFagomradeJournalfortJournalpostRequest(private var journalpostId: Long, private var journalpost: Journalpost) :
    OppdaterJournalpostRequest(journalpostId) {
    init {
        journalpost.tilleggsopplysninger.removeEndretTemaFlagg()
        tilleggsopplysninger = journalpost.tilleggsopplysninger
    }
}

data class EndreTittelRequest(private var journalpostId: Long, override var tittel: String?, private var journalpost: Journalpost) :
    OppdaterJournalpostRequest(journalpostId) {

    init {
        val hoveddokument = journalpost.hentHoveddokument()
        if (hoveddokument != null) dokumenter = listOf(Dokument(hoveddokument.dokumentInfoId, tittel, hoveddokument.brevkode))
        avsenderMottaker = AvsenderMottaker(journalpost.avsenderMottaker?.navn)
    }
}

data class EndreKnyttTilGenerellSakRequest(
    private var journalpostId: Long,
    override var bruker: Bruker?,
    override var tema: String?,
    override var sak: Sak? = GenerellSak()
) : OppdaterJournalpostRequest(journalpostId)

data class InngaaendeTilUtgaaendeRequest(private var journalpostId: Long, override var tema: String?) : OppdaterJournalpostRequest(journalpostId)
data class RegistrerReturRequest(
    private var journalpostId: Long,
    private var _datoRetur: LocalDate,
    private var _tilleggsopplysninger: TilleggsOpplysninger?
) : OppdaterJournalpostRequest(journalpostId) {
    init {
        datoRetur = DateUtils.formatDate(_datoRetur)
        tilleggsopplysninger = _tilleggsopplysninger
    }
}