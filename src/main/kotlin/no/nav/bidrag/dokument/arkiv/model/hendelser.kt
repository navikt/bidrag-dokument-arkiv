package no.nav.bidrag.dokument.arkiv.model

import no.nav.bidrag.commons.CorrelationId
import java.time.LocalDateTime

data class JournalpostHendelse(
    var journalpostId: String,
    val hendelse: String,
    val sporing: Sporingsdata = Sporingsdata(CorrelationId.fetchCorrelationIdForThread() ?: CorrelationId.generateTimestamped(hendelse).get()),
    val detaljer: MutableMap<String, String?> = mutableMapOf()
) {
    constructor(journalpostId: Long, hendelse: String) : this(journalpostId.toString(), hendelse, Sporingsdata(CorrelationId.fetchCorrelationIdForThread() ?: CorrelationId.generateTimestamped(hendelse).get()), mutableMapOf())

    fun leggSaksbehandlerTilSporing(saksbehandler: Saksbehandler) {
        sporing.brukerident = saksbehandler.ident
        sporing.saksbehandlersNavn = saksbehandler.navn
    }

    fun leggBrukeridentTilSporing(brukerident: String?) {
        sporing.brukerident = brukerident
    }

    fun addDetaljer(key: String, value: String){
        detaljer[key] = value
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
