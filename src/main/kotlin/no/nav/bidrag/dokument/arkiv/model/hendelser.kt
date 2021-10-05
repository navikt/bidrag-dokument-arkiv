package no.nav.bidrag.dokument.arkiv.model

import no.nav.bidrag.commons.CorrelationId
import java.time.LocalDateTime

@Suppress("unused") // brukes av jackson
data class JournalpostHendelse(
    var journalpostId: String,
    val hendelse: String,
) {
    val sporing: Sporingsdata = Sporingsdata(CorrelationId.fetchCorrelationIdForThread() ?: CorrelationId.generateTimestamped(hendelse).get())
    val detaljer: MutableMap<String, String?> =  mutableMapOf()

    constructor(journalpostId: Long, hendelse: String) : this(journalpostId.toString(), hendelse)

    fun addDetaljer(key: String, value: String){
        detaljer[key] = value
    }
}

data class Sporingsdata(val correlationId: String) {
    @Suppress("unused") // brukes av jackson
    val opprettet: LocalDateTime = LocalDateTime.now()
}