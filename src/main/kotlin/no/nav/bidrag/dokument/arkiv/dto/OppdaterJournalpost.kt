package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.bidrag.dokument.arkiv.utils.DateUtils
import no.nav.bidrag.transport.dokument.DistribuerTilAdresse
import org.apache.logging.log4j.util.Strings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class OppdaterFlaggNyDistribusjonBestiltRequest(
    private var journalpostId: Long,
    private var journalpost: Journalpost,
) :
    OppdaterJournalpostRequest(journalpostId) {
    init {
        journalpost.tilleggsopplysninger.setNyDistribusjonBestiltFlagg()
        tilleggsopplysninger = journalpost.tilleggsopplysninger
    }
}

data class OppdaterDistribusjonsInfoRequest(
    val settStatusEkspedert: Boolean,
    val utsendingsKanal: JournalpostUtsendingKanal,
)

data class OpprettNyReturLoggRequest(private var journalpost: Journalpost, private val kommentar: String? = null) :
    OppdaterJournalpostRequest(journalpostId = journalpost.hentJournalpostIdLong()) {
    init {
        val dateNow = LocalDate.now()
        val returDetaljerLogDO = journalpost.tilleggsopplysninger.hentReturDetaljerLogDO()
        val returDetaljLoggWithSameDate = returDetaljerLogDO.find { it.dato == dateNow }
        if (returDetaljLoggWithSameDate != null) {
            journalpost.tilleggsopplysninger.unlockReturDetaljerLog(dateNow)
        } else {
            journalpost.tilleggsopplysninger.addReturDetaljLog(
                ReturDetaljerLogDO(
                    kommentar ?: "Returpost",
                    dateNow,
                ),
            )
        }
        tilleggsopplysninger = journalpost.tilleggsopplysninger
    }
}

data class LagreReturDetaljForSisteReturRequest(private var journalpost: Journalpost) :
    OppdaterJournalpostRequest(journalpostId = journalpost.hentJournalpostIdLong()) {
    init {
        journalpost.tilleggsopplysninger.addReturDetaljLog(
            ReturDetaljerLogDO(
                "Returpost",
                journalpost.hentDatoRetur()!!,
            ),
        )
        tilleggsopplysninger = journalpost.tilleggsopplysninger
    }
}

data class LagreAvsenderNavnRequest(
    private var journalpostId: Long,
    private var avsenderNavn: String,
) :
    OppdaterJournalpostRequest(journalpostId = journalpostId) {
    init {
        avsenderMottaker = AvsenderMottaker(avsenderNavn)
    }
}

data class LockReturDetaljerRequest(private var journalpost: Journalpost) :
    OppdaterJournalpostRequest(journalpostId = journalpost.hentJournalpostIdLong()) {
    init {
        val updatedTillegsopplysninger = TilleggsOpplysninger()
        updatedTillegsopplysninger.addAll(journalpost.tilleggsopplysninger)
        updatedTillegsopplysninger.lockAllReturDetaljerLog()
        tilleggsopplysninger = updatedTillegsopplysninger
    }
}

data class LeggTilBeskjedPåTittel(
    private var journalpostId: Long,
    private var journalpost: Journalpost,
    private val beskjed: String,
) :
    OppdaterJournalpostRequest(journalpostId) {
    init {
        val hoveddokument = journalpost.hentHoveddokument()!!
        dokumenter =
            listOf(
                Dokument(
                    dokumentInfoId = hoveddokument.dokumentInfoId,
                    tittel = "${hoveddokument.tittel} ($beskjed)",
                ),
            )
    }
}

data class OppdaterDokumentdatoTilIdag(
    private var journalpostId: Long,
    private var journalpost: Journalpost,
) :
    OppdaterJournalpostRequest(journalpostId) {
    init {
        datoDokument = LocalDateTime.now().toString()
    }
}

data class OppdaterJournalpostTilleggsopplysninger(
    private var journalpostId: Long,
    private var journalpost: Journalpost,
) :
    OppdaterJournalpostRequest(journalpostId) {
    init {
        tilleggsopplysninger = journalpost.tilleggsopplysninger
    }
}

data class OppdaterJournalpostDistribusjonsInfoRequest(
    private var journalpostId: Long,
    private var journalpost: Journalpost,
) :
    OppdaterJournalpostRequest(journalpostId) {
    init {
        journalpost.tilleggsopplysninger.setDistribusjonBestillt()
        tilleggsopplysninger = journalpost.tilleggsopplysninger
    }
}

data class LagreAdresseRequest(
    private var journalpostId: Long,
    private val mottakerAdresse: DistribuerTilAdresse?,
    private var journalpost: Journalpost,
) : OppdaterJournalpostRequest(journalpostId) {
    init {
        val mottakerAdresseDO = mapToAdresseDO(mottakerAdresse)
        if (journalpost.isUtgaaendeDokument() && mottakerAdresseDO != null) {
            journalpost.tilleggsopplysninger.addMottakerAdresse(mottakerAdresseDO)
            tilleggsopplysninger = journalpost.tilleggsopplysninger
        }
    }

    private fun mapToAdresseDO(adresse: DistribuerTilAdresse?): DistribuertTilAdresseDo? {
        return if (adresse != null) {
            DistribuertTilAdresseDo(
                adresselinje1 = adresse.adresselinje1,
                adresselinje2 = adresse.adresselinje2,
                adresselinje3 = adresse.adresselinje3,
                land = adresse.land!!,
                poststed = adresse.poststed,
                postnummer = adresse.postnummer,
            )
        } else {
            null
        }
    }
}

data class LagreJournalfortAvIdentRequest(
    private var journalpostId: Long,
    private var journalpost: Journalpost,
    private var journalfortAvIdent: String,
) : OppdaterJournalpostRequest(journalpostId) {
    init {
        journalpost.tilleggsopplysninger.setJournalfortAvIdent(journalfortAvIdent)
        tilleggsopplysninger = journalpost.tilleggsopplysninger
    }
}

data class LagreJournalpostRequest(
    private var journalpostId: Long,
    private var endreJournalpostCommand: EndreJournalpostCommandIntern,
    private var journalpost: Journalpost,
) : OppdaterJournalpostRequest(journalpostId) {
    init {
        if (journalpost.isStatusMottatt()) {
            tittel = endreJournalpostCommand.endreJournalpostCommand.tittel
        }

        if (journalpost.isNotat()) {
            datoDokument = endreJournalpostCommand.endreJournalpostCommand.dokumentDato?.let {
                LocalDateTime.of(
                    it,
                    LocalTime.MIDNIGHT,
                )
            }?.toString()
        }

        if (endreJournalpostCommand.endreJournalpostCommand.endreDokumenter.isNotEmpty()) {
            dokumenter = endreJournalpostCommand.endreJournalpostCommand.endreDokumenter
                .map { dokument ->
                    Dokument(
                        dokument.dokId.toString(),
                        dokument.tittel,
                        dokument.brevkode,
                    )
                }
        }

        if (journalpost.isInngaaendeDokument()) {
            if (!journalpost.erJournalførtSenereEnnEttÅrSiden()) {
                avsenderMottaker =
                    AvsenderMottaker(endreJournalpostCommand.hentAvsenderNavn(journalpost))
            }
            datoMottatt =
                DateUtils.formatDate(endreJournalpostCommand.endreJournalpostCommand.dokumentDato)
        }

        if (journalpost.isStatusMottatt()) updateValuesForMottattJournalpost()

        if (journalpost.isUtgaaendeDokument()) {
            val endreReturDetaljer =
                endreJournalpostCommand.endreJournalpostCommand.endreReturDetaljer?.filter {
                    Strings.isNotEmpty(it.beskrivelse)
                }
            if (!endreReturDetaljer.isNullOrEmpty()) {
                endreReturDetaljer
                    .forEach {
                        if (it.originalDato != null) {
                            journalpost.tilleggsopplysninger.updateReturDetaljLog(
                                it.originalDato!!,
                                ReturDetaljerLogDO(it.beskrivelse, it.nyDato ?: it.originalDato!!),
                            )
                        } else if (journalpost.manglerReturDetaljForSisteRetur() && it.nyDato != null && !journalpost.hasReturDetaljerWithDate(
                                it.nyDato!!,
                            )
                        ) {
                            journalpost.tilleggsopplysninger.addReturDetaljLog(
                                ReturDetaljerLogDO(it.beskrivelse, it.nyDato!!),
                            )
                        }
                    }
                tilleggsopplysninger = journalpost.tilleggsopplysninger
            }

            val adresseDo = endreJournalpostCommand.endreAdresse
            if (adresseDo != null) {
                journalpost.tilleggsopplysninger.addMottakerAdresse(adresseDo)
                tilleggsopplysninger = journalpost.tilleggsopplysninger
            }
        }
    }

    private fun updateValuesForMottattJournalpost() {
        val saksnummer =
            if (endreJournalpostCommand.harEnTilknyttetSak()) endreJournalpostCommand.hentTilknyttetSak() else null
        sak = if (saksnummer != null) Sak(saksnummer) else null

        bruker = if (endreJournalpostCommand.hentGjelder() != null) {
            Bruker(
                endreJournalpostCommand.hentGjelder(),
                endreJournalpostCommand.hentGjelderType().name,
            )
        } else if (journalpost.bruker != null) {
            Bruker(journalpost.bruker?.id, journalpost.bruker?.type)
        } else {
            null
        }
        tema =
            if (endreJournalpostCommand.hentFagomrade() != null) endreJournalpostCommand.hentFagomrade() else journalpost.tema
    }
}

@JsonIgnoreProperties(ignoreUnknown = true, value = ["journalpostId"])
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed class OppdaterJournalpostRequest(private var journalpostId: Long? = -1) {
    open var sak: Sak? = null
    open var tittel: String? = null
    open var journalfoerendeEnhet: String? = null
    open var datoRetur: String? = null
    open var datoDokument: String? = null
    open var tilleggsopplysninger: List<Map<String, String>>? = null
    open var tema: String? = null
    open var datoMottatt: String? = null
    open var bruker: Bruker? = null
    open var dokumenter = emptyList<Dokument>()
    open var avsenderMottaker: AvsenderMottaker? = null

    fun hentJournalpostId() = journalpostId

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class AvsenderMottaker(
        val navn: String? = null,
        var id: String? = null,
        var idType: AvsenderMottakerIdType? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Dokument(
        val dokumentInfoId: String? = null,
        var tittel: String? = null,
        val brevkode: String? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Bruker(val id: String? = null, val idType: String? = null)

    @Suppress("unused") // properties used by jackson
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    open class Sak(val fagsakId: String? = null) {
        var fagsaksystem = if (fagsakId == null) null else "BISYS"
        open var sakstype = if (fagsakId == null) null else Sakstype.FAGSAK
    }

    data class GenerellSak(override var sakstype: Sakstype? = Sakstype.GENERELL_SAK) : Sak()
}

data class OppdaterJournalpostResponse(var journalpostId: Long? = null)

data class FerdigstillJournalpostRequest(
    @JsonIgnore
    val journalpostId: Long,
    val journalfoerendeEnhet: String,
    val journalfortAvNavn: String? = null,
    val opprettetAvNavn: String? = null,
    val datoJournal: LocalDate? = null,
) {
    constructor(journalpostId: Long, journalfoerendeEnhet: String) : this(
        journalpostId,
        journalfoerendeEnhet,
        null,
        null,
        null,
    )
}

enum class Sakstype {
    FAGSAK,
    GENERELL_SAK,
}

enum class BrukerIdType {
    FNR,
    ORGNR,
    AKTOERID,
}

enum class Fagsaksystem {
    FS38,
    FS36,
    UFM,
    OEBS,
    OB36,
    AO01,
    AO11,
    IT01,
    PP01,
    K9,
    BISYS,
    BA,
    EF,
    KONT,
}

enum class Fagomrade {
    BID,
    FAR,
}
