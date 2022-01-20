package no.nav.bidrag.dokument.arkiv.dto

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