package no.nav.bidrag.dokument.arkiv.dto

import java.time.LocalDate

private const val DATO_JOURNALFORT = "DATO_JOURNALFOERT"
private const val DATO_REGISTRERT = "DATO_REGISTRERT"

data class Journalpost(
        var avsenderMottaker: AvsenderMottaker? = null,
        var bruker: Bruker? = null,
        var dokumenter: List<Dokument> = emptyList(),
        var journalforendeEnhet: String? = null,
        var journalfortAvNavn: String? = null,
        var journalpostId: String? = null,
        var journalposttype: String? = null,
        var journalstatus: String? = null,
        var relevanteDatoer: List<DatoType> = emptyList(),
        var tema: String? = null,
        var tittel: String? = null
) {
    fun hentDatoJournalfort(): LocalDate? {
        val journalfort = relevanteDatoer
                .find { it.datotype == DATO_JOURNALFORT }

        return journalfort?.somDato()
    }

    fun hentDatoRegistrert(): LocalDate? {
        val registrert = relevanteDatoer
                .find { it.datotype == DATO_REGISTRERT }

        return registrert?.somDato()
    }
}

data class AvsenderMottaker(
        var navn: String? = null
)

data class Bruker(
        var id: String? = null,
        var type: String? = null
)

data class Dokument(
        var tittel: String? = null
)

data class DatoType(
        var dato: String? = null,
        var datotype: String? = null
) {
    fun somDato(): LocalDate {
        val datoStreng = dato?.substring(0, 10)

        if (datoStreng != null) {
            return LocalDate.parse(datoStreng)
        }

        throw IllegalStateException("Kunne ikke trekke ut dato fra: " + dato)
    }
}
