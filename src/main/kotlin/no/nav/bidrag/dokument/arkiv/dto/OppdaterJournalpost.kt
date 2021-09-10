package no.nav.bidrag.dokument.arkiv.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OppdaterJournalpostRequest(
        private val journalpostId: String,
        private val endreJournalpostCommand: EndreJournalpostCommandIntern,
) {
    var sak = Sak()
    var tittel: String? = null
    var tema: String? = null
    var bruker = Bruker()
    var dokumenter = emptyList<Dokument>()
    var avsenderMottaker = AvsenderMottaker()

    fun hentJournalpostId() = journalpostId;

    constructor(journalpostId: String, endreJournalpostCommand: EndreJournalpostCommandIntern, journalpost: Journalpost) : this(
            journalpostId = journalpostId,
            endreJournalpostCommand = endreJournalpostCommand
    ) {
        avsenderMottaker = AvsenderMottaker(endreJournalpostCommand.hentAvsenderNavn(journalpost))

        val saksnummer = if (endreJournalpostCommand.harEnTilknyttetSak()) {
            endreJournalpostCommand.hentTilknyttetSak()
        } else {
            journalpost.sak?.fagsakId
        }
        bruker = Bruker(endreJournalpostCommand.hentGjelder(), endreJournalpostCommand.hentGjelderType())
        tittel = endreJournalpostCommand.endreJournalpostCommand.tittel
        sak = Sak(saksnummer)
        tema = if (endreJournalpostCommand.hentFagomrade() != null) endreJournalpostCommand.hentFagomrade() else journalpost.tema
        dokumenter = endreJournalpostCommand.endreJournalpostCommand.endreDokumenter
                .map { dokument -> Dokument(dokument.dokId.toString(), dokument.tittel, dokument.brevkode) }
    }

    data class AvsenderMottaker(val navn: String? = null)
    data class Dokument(val dokumentInfoId: String? = null, val tittel: String? = null, val brevkode: String? = null)
    data class Bruker(val id: String? = null, val idType: String? = null)

    data class Sak(val fagsakId: String? = null) {
        val fagsaksystem = if (fagsakId == null) null else "BISYS"
        val sakstype = if (fagsakId === null) null else "FAGSAK"
    }
}

data class OppdaterJournalpostResponse(
        var journalpostId: Int? = null,
        var saksnummer: String? = null
)
