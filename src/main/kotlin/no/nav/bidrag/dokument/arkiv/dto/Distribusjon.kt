package no.nav.bidrag.dokument.arkiv.dto

data class DokDistDistribuerJournalpostRequest(
    var journalpostId: Long,
    var batchId: String? = null,
    var bestillendeFagsystem: String = "BISYS",
    var dokumentProdApp: String = "bidragDokumentArkiv",
    var adresse: DistribuerTilAdresse? = null
)

data class DistribuerJournalpostRequest(
    var adresse: DistribuerTilAdresse? = null
) {
    fun hasAdresse(): Boolean = adresse != null
    fun toDokDistDistribuerJournalpostRequest(journalpostId: Long): DokDistDistribuerJournalpostRequest{
        return DokDistDistribuerJournalpostRequest(journalpostId = journalpostId, adresse = adresse)
    }
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

data class DistribuerTilAdresse(
    var adresselinje1: String,
    var adresselinje2: String? = null,
    var adresselinje3: String? = null,
    var adressetype: String,
    var land: String? = null,
    var postnummer: String? = null,
    var poststed: String? = null,
)