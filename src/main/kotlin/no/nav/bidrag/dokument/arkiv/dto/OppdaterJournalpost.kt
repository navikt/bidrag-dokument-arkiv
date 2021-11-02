package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

data class LagreJournalpostRequest(private var journalpostId: Long, private var endreJournalpostCommand: EndreJournalpostCommandIntern, private var journalpost: Journalpost): OppdaterJournalpostRequest(journalpostId) {
    init {
        avsenderMottaker = AvsenderMottaker(endreJournalpostCommand.hentAvsenderNavn(journalpost))
        tittel = endreJournalpostCommand.endreJournalpostCommand.tittel
        if (journalpost.isStatusMottatt()) {
            val journalpostSak = if (journalpost.hasSak()) journalpost.sak?.fagsakId else null
            val saksnummer = if (journalpostSak == null && endreJournalpostCommand.harEnTilknyttetSak()) {
                endreJournalpostCommand.hentTilknyttetSak()
            } else {
                journalpostSak
            }
            bruker = if (endreJournalpostCommand.hentGjelder()!=null) Bruker(endreJournalpostCommand.hentGjelder(), endreJournalpostCommand.hentGjelderType()) else Bruker()
            sak = if (saksnummer != null) Sak(saksnummer) else null
            tema = if (endreJournalpostCommand.hentFagomrade() != null) endreJournalpostCommand.hentFagomrade() else journalpost.tema
            dokumenter = endreJournalpostCommand.endreJournalpostCommand.endreDokumenter
                .map { dokument -> Dokument(dokument.dokId.toString(), dokument.tittel, dokument.brevkode) }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true, value = [ "journalpostId" ])
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed class OppdaterJournalpostRequest(private var journalpostId: Long? = -1) {
    open var sak: Sak? = null
    open var tittel: String? = null
    open var journalfoerendeEnhet: String? = null
    open var tema: String? = null
    open var bruker: Bruker? = null
    open var dokumenter = emptyList<Dokument>()
    open var avsenderMottaker: AvsenderMottaker? = null

    fun hentJournalpostId() = journalpostId
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class AvsenderMottaker(val navn: String? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Dokument(val dokumentInfoId: String? = null, val tittel: String? = null, val brevkode: String? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Bruker(val id: String? = null, val idType: String? = null)

    @Suppress("unused") // properties used by jackson
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Sak(val fagsakId: String? = null) {
        val fagsaksystem = if (fagsakId == null) null else "BISYS"
        val sakstype = if (fagsakId === null) null else "FAGSAK"
    }
}

data class OppdaterJournalpostResponse(var journalpostId: Long? = null)

data class FerdigstillJournalpostRequest(
        @JsonIgnore
        var journalpostId: Long,
        var journalfoerendeEnhet: String
)
