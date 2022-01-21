package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.dto.AdresseType
import no.nav.bidrag.dokument.dto.DistribuerJournalpostRequest
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse
import org.apache.commons.lang3.Validate

data class DokDistDistribuerJournalpostRequest(
    var journalpostId: Long,
    var batchId: String? = null,
    var bestillendeFagsystem: String = "BISYS",
    var dokumentProdApp: String = "bidrag-dokument-arkiv",
    var adresse: DokDistDistribuerTilAdresse? = null
) {

    private fun mapAdresse(distribuerTilAdresse: DistribuerTilAdresse?): DokDistDistribuerTilAdresse? {
        val adresse = distribuerTilAdresse ?: return null
        val dokDistAdresseType = if (adresse.adressetype == AdresseType.UTENLANDSK_POSTADRESSE) DokDistAdresseType.utenlandskPostadresse else DokDistAdresseType.norskPostadresse
        return DokDistDistribuerTilAdresse(
            adresselinje1 = adresse.adresselinje1,
            adresselinje2 = adresse.adresselinje2,
            adresselinje3 =  adresse.adresselinje3,
            adressetype = dokDistAdresseType,
            land =  adresse.land,
            postnummer =  adresse.postnummer,
            poststed = adresse.poststed
        )
    }

    constructor(journalpostId: Long, distribuerTilAdresse: DistribuerTilAdresse?): this(journalpostId = journalpostId) {
        adresse = mapAdresse(distribuerTilAdresse)
    }
}

enum class DokDistAdresseType{
    norskPostadresse,
    utenlandskPostadresse
}

data class DokDistDistribuerTilAdresse(
    var adresselinje1: String,
    var adresselinje2: String? = null,
    var adresselinje3: String? = null,
    var adressetype: DokDistAdresseType,
    var land: String? = null,
    var postnummer: String? = null,
    var poststed: String? = null,
)

data class DistribuerJournalpostRequestInternal(
    var request: DistribuerJournalpostRequest? = null
) {
    fun hasAdresse(): Boolean = request?.adresse != null
    fun getAdresse(): DistribuerTilAdresse? = request?.adresse
}

data class DokDistDistribuerJournalpostResponse(
    var bestillingsId: String,
) {
    fun toDistribuerJournalpostResponse(journalpostId: Long): DistribuerJournalpostResponse {
        return DistribuerJournalpostResponse("JOARK-${journalpostId}", bestillingsId)
    }
}

fun validerKanDistribueres(journalpost: Journalpost?, distribuerJournalpostRequest: DistribuerJournalpostRequestInternal?) {
    Validate.isTrue(journalpost != null, "Fant ingen journalpost")
    Validate.isTrue(journalpost?.journalstatus == JournalStatus.FERDIGSTILT, "Journalpost må ha status FERDIGSTILT")
    Validate.isTrue(journalpost?.hentTilknyttetSaker()?.size == 1, "Journalpost må ha totalt 1 sak før distribusjon, journalposten har ${journalpost?.hentTilknyttetSaker()?.size} saker")
    Validate.isTrue(journalpost?.tema == "BID", "Journalpost må ha tema BID")
    Validate.isTrue(journalpost?.hasMottakerId() == true, "Journalpost må ha satt mottakerId")
    Validate.isTrue(distribuerJournalpostRequest != null && distribuerJournalpostRequest.hasAdresse(), "Adresse må være satt i input")
}