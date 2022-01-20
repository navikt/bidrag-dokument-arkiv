package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.dto.JournalpostDto
import org.apache.commons.lang3.Validate

data class DokDistDistribuerJournalpostRequest(
    var journalpostId: Long,
    var batchId: String? = null,
    var bestillendeFagsystem: String = "BISYS",
    var dokumentProdApp: String = "bidragDokumentArkiv",
    var adresse: DokDistDistribuerTilAdresse? = null
)

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

data class DistribuerJournalpostRequest(
    var adresse: DistribuerTilAdresse? = null
) {
    fun hasAdresse(): Boolean = adresse != null
    fun toDokDistDistribuerJournalpostRequest(journalpostId: Long): DokDistDistribuerJournalpostRequest =
        DokDistDistribuerJournalpostRequest(journalpostId = journalpostId, adresse = adresse?.toDokDistAdresse())
}

data class DistribuerJournalpostResponse(
    var journalpostId: Long,
    var bestillingsId: String
)

data class DokDistDistribuerJournalpostResponse(
    var bestillingsId: String,
) {
    fun toDistribuerJournalpostResponse(journalpostId: Long): DistribuerJournalpostResponse {
        return DistribuerJournalpostResponse(journalpostId, bestillingsId)
    }
}

enum class AdresseType {
    NORSK_POSTADRESSE,
    UTENLANDSK_POSTADRESSE
}

data class DistribuerTilAdresse(
    var adresselinje1: String,
    var adresselinje2: String? = null,
    var adresselinje3: String? = null,
    var adressetype: AdresseType,
    var land: String? = null,
    var postnummer: String? = null,
    var poststed: String? = null,
) {
    fun toDokDistAdresse(): DokDistDistribuerTilAdresse {
        val dokDistAdresseType = if (adressetype == AdresseType.UTENLANDSK_POSTADRESSE) DokDistAdresseType.utenlandskPostadresse else DokDistAdresseType.norskPostadresse
        return DokDistDistribuerTilAdresse(adresselinje1, adresselinje2, adresselinje3, dokDistAdresseType, land, postnummer, poststed)
    }
}
fun validerKanDistribueres(journalpost: Journalpost?, distribuerJournalpostRequest: DistribuerJournalpostRequest?) {
    Validate.isTrue(journalpost != null, "Fant ingen journalpost")
    Validate.isTrue(journalpost?.journalstatus == JournalStatus.FERDIGSTILT, "Journalpost må ha status FERDIGSTILT")
    Validate.isTrue(journalpost?.hentTilknyttetSaker()?.size == 1, "Journalpost må ha totalt 1 sak før distribusjon, journalposten har ${journalpost?.hentTilknyttetSaker()?.size} saker")
    Validate.isTrue(journalpost?.tema == "BID", "Journalpost må ha tema BID")
    Validate.isTrue(journalpost?.hasMottakerId() == true, "Journalpost må ha satt mottakerId")
    Validate.isTrue(distribuerJournalpostRequest != null && distribuerJournalpostRequest.hasAdresse(), "Adresse må være satt i input")
}