package no.nav.bidrag.dokument.arkiv.stubs

import no.nav.bidrag.dokument.dto.DistribuerTilAdresse

fun createDistribuerTilAdresse(): DistribuerTilAdresse{
    return DistribuerTilAdresse(
        adresselinje1 = "Adresselinje1",
        adresselinje2 = null,
        adresselinje3 = null,
        land = "NO",
        postnummer = "3000",
        poststed = "Ingen"
    )
}