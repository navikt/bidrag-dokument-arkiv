package no.nav.bidrag.dokument.arkiv.model

import no.nav.bidrag.commons.CorrelationId
import java.time.LocalDateTime

data class JournalpostHendelse(
    val journalpostId: String,
    val hendelse: String,
    val sporing: Sporingsdata = Sporingsdata(CorrelationId.fetchCorrelationIdForThread() ?: CorrelationId.generateTimestamped(hendelse).get()),
    val detaljer: Map<String, String?> = emptyMap()
) {

    fun leggSaksbehandlerTilSporing(saksbehandler: Saksbehandler) {
        sporing.brukerident = saksbehandler.ident
        sporing.saksbehandlersNavn = saksbehandler.navn
    }

    fun leggBrukeridentTilSporing(brukerident: String?) {
        sporing.brukerident = brukerident
    }
}

data class Sporingsdata(val correlationId: String) {
    var brukerident: String? = null
    @Suppress("unused") // brukes av jackson
    val opprettet: LocalDateTime = LocalDateTime.now()
    var saksbehandlersNavn: String? = null
}

data class Saksbehandler(
    var ident: String = "",
    var navn: String? = null
)
