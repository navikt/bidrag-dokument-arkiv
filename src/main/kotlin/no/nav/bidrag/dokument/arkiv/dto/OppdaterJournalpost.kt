package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.bidrag.dokument.arkiv.utils.DateUtils
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse
import org.apache.logging.log4j.util.Strings

data class OppdaterJournalpostDistribusjonsInfoRequest(private var journalpostId: Long, private var journalpost: Journalpost): OppdaterJournalpostRequest(journalpostId) {
    init {
        journalpost.tilleggsopplysninger.setDistribusjonBestillt()
        tilleggsopplysninger = journalpost.tilleggsopplysninger
    }
}

data class LagreAdresseRequest(private var journalpostId: Long, private val mottakerAdresse: DistribuerTilAdresse?, private var journalpost: Journalpost): OppdaterJournalpostRequest(journalpostId){
        init {
            val mottakerAdresseDO = mapToAdresseDO(mottakerAdresse)
            if (journalpost.isUtgaaendeDokument() && mottakerAdresseDO != null){
                journalpost.tilleggsopplysninger.addMottakerAdresse(mottakerAdresseDO)
                tilleggsopplysninger = journalpost.tilleggsopplysninger
            }
        }

    private fun mapToAdresseDO(adresse: DistribuerTilAdresse?): DistribuertTilAdresseDo? {
        return if (adresse != null) DistribuertTilAdresseDo(
            adresselinje1 = adresse.adresselinje1,
            adresselinje2 = adresse.adresselinje2,
            adresselinje3 = adresse.adresselinje3,
            land = adresse.land!!,
            poststed = adresse.poststed,
            postnummer = adresse.postnummer
        ) else null
    }
}

data class LagreJournalpostRequest(private var journalpostId: Long, private var endreJournalpostCommand: EndreJournalpostCommandIntern, private var journalpost: Journalpost): OppdaterJournalpostRequest(journalpostId) {
    init {
        tittel = endreJournalpostCommand.endreJournalpostCommand.tittel

        if (endreJournalpostCommand.endreJournalpostCommand.endreDokumenter.isNotEmpty()){
            dokumenter = endreJournalpostCommand.endreJournalpostCommand.endreDokumenter
                .map { dokument -> Dokument(dokument.dokId.toString(), dokument.tittel, dokument.brevkode) }
        }

        if (journalpost.isInngaaendeDokument()){
            avsenderMottaker = AvsenderMottaker(endreJournalpostCommand.hentAvsenderNavn(journalpost))
            datoMottatt = DateUtils.formatDate(endreJournalpostCommand.endreJournalpostCommand.dokumentDato)
        }

        if (journalpost.isStatusMottatt()) updateValuesForMottattJournalpost()

        if (journalpost.isUtgaaendeDokument()){
            val endreReturDetaljer = endreJournalpostCommand.endreJournalpostCommand.endreReturDetaljer?.filter { Strings.isNotEmpty(it.beskrivelse) }
            if (endreReturDetaljer != null && endreReturDetaljer.isNotEmpty()){
                endreReturDetaljer
                    .forEach { journalpost.tilleggsopplysninger.updateReturDetaljLog(it.originalDato, ReturDetaljerLogDO(it.beskrivelse, it.nyDato ?: it.originalDato)) }
                tilleggsopplysninger = journalpost.tilleggsopplysninger
            }

            val adresseDo = endreJournalpostCommand.endreAdresse
            if (adresseDo != null){
                journalpost.tilleggsopplysninger.addMottakerAdresse(adresseDo)
                tilleggsopplysninger = journalpost.tilleggsopplysninger
            }
        }
    }

    private fun updateValuesForMottattJournalpost(){
        val saksnummer = if (endreJournalpostCommand.harEnTilknyttetSak()) endreJournalpostCommand.hentTilknyttetSak() else null
        sak = if (saksnummer != null) Sak(saksnummer) else null

        bruker = if (endreJournalpostCommand.hentGjelder()!=null) Bruker(endreJournalpostCommand.hentGjelder(), endreJournalpostCommand.hentGjelderType())
                 else if (journalpost.bruker != null) Bruker(journalpost.bruker?.id, journalpost.bruker?.type)
                 else null
        tema = if (endreJournalpostCommand.hentFagomrade() != null) endreJournalpostCommand.hentFagomrade() else journalpost.tema
    }
}

@JsonIgnoreProperties(ignoreUnknown = true, value = [ "journalpostId" ])
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed class OppdaterJournalpostRequest(private var journalpostId: Long? = -1) {
    open var sak: Sak? = null
    open var tittel: String? = null
    open var journalfoerendeEnhet: String? = null
    open var datoRetur: String? = null
    open var tilleggsopplysninger: List<Map<String, String>>? = null
    open var tema: String? = null
    open var datoMottatt: String? = null
    open var bruker: Bruker? = null
    open var dokumenter = emptyList<Dokument>()
    open var avsenderMottaker: AvsenderMottaker? = null

    fun hentJournalpostId() = journalpostId
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class AvsenderMottaker(val navn: String? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Dokument(val dokumentInfoId: String? = null, val tittel: String? = null, val brevkode: String? = null)

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

    data class GenerellSak(override var sakstype: Sakstype? = Sakstype.GENERELL_SAK): Sak()
}

data class OppdaterJournalpostResponse(var journalpostId: Long? = null)

data class FerdigstillJournalpostRequest(
        @JsonIgnore
        var journalpostId: Long,
        var journalfoerendeEnhet: String
)

enum class Sakstype {
    FAGSAK, GENERELL_SAK
}

enum class BrukerIdType {
    FNR, ORGNR, AKTOERID
}

enum class Fagsaksystem {
    FS38, FS36, UFM, OEBS, OB36, AO01, AO11, IT01, PP01, K9, BISYS, BA, EF, KONT
}

enum class Fagomrade {
    BID, FAR
}