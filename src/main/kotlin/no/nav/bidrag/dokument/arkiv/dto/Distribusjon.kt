package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.dto.DistribuerJournalpostRequest
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate

private val ALPHA2_NORGE = "NO"

data class DokDistDistribuerJournalpostRequest(
    var journalpostId: Long,
    var batchId: String? = null,
    var bestillendeFagsystem: String = "BID",
    var dokumentProdApp: String = "bidragDokArkiv",
    var adresse: DokDistDistribuerTilAdresse? = null
) {

    private fun mapAdresse(distribuerTilAdresse: DistribuerTilAdresse?): DokDistDistribuerTilAdresse? {
        val adresse = distribuerTilAdresse ?: return null
        val dokDistAdresseType = if (adresse.land == ALPHA2_NORGE) DokDistAdresseType.norskPostadresse else DokDistAdresseType.utenlandskPostadresse
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
    var adresselinje1: String? = null,
    var adresselinje2: String? = null,
    var adresselinje3: String? = null,
    var adressetype: DokDistAdresseType,
    var land: String,
    var postnummer: String? = null,
    var poststed: String? = null,
)

data class DistribuerJournalpostRequestInternal(
    var request: DistribuerJournalpostRequest? = null
) {
    fun hasAdresse(): Boolean = request?.adresse != null
    fun getAdresse(): DistribuerTilAdresse? = request?.adresse
    fun getAdresseDo(): DistribuertTilAdresseDo? {
        val adresse = getAdresse()
        return if (adresse != null) DistribuertTilAdresseDo(
            adresselinje1 = adresse.adresselinje1,
            adresselinje2 = adresse.adresselinje2,
            adresselinje3 = adresse.adresselinje3,
            land = adresse.land,
            poststed = adresse.poststed,
            postnummer = adresse.postnummer
        ) else null
    }
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
    Validate.isTrue(journalpost?.tilleggsopplysninger?.isDistribusjonBestilt() == false, "Journalpost er allerede distribuert")
    Validate.isTrue(journalpost?.tema == "BID", "Journalpost må ha tema BID")
    Validate.isTrue(journalpost?.hasMottakerId() == true, "Journalpost må ha satt mottakerId")
    Validate.isTrue(distribuerJournalpostRequest != null && distribuerJournalpostRequest.hasAdresse(), "Adresse må være satt i input")
}

fun validerAdresse(adresse: DistribuerTilAdresse?){
    Validate.isTrue(adresse != null, "Adresse må være satt")
    validateNotNullOrEmpty(adresse?.land, "Land er påkrevd på adresse")
    Validate.isTrue(adresse?.land?.length == 2, "Land må være formatert som Alpha-2 to-bokstavers landkode ")
    if (ALPHA2_NORGE == adresse?.land){
        validateNotNullOrEmpty(adresse.postnummer, "Postnummer er påkrevd på norsk adresse")
        validateNotNullOrEmpty(adresse.poststed, "Poststed er påkrevd på norsk adresse")
    } else {
        validateNotNullOrEmpty(adresse?.adresselinje1, "Adresselinje1 er påkrevd på utenlandsk adresse")
    }
}

fun validateNotNullOrEmpty(value: String?, message: String){
    Validate.isTrue(StringUtils.isNotEmpty(value), message)
}
