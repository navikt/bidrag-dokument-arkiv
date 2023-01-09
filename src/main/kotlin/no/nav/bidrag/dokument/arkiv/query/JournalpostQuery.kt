package no.nav.bidrag.dokument.arkiv.query

data class JournalpostQuery(val journalpostId: Long) : GraphQuery() {

    private val query = """
        query journalpost(${"$"}journalpostId: String!) {
            journalpost(journalpostId: ${"$"}journalpostId) {
              avsenderMottaker {
                navn
                id
                type
              }
              bruker {
                id
                type
              }
              dokumenter {
                dokumentInfoId
                brevkode
                tittel
              }
              sak {
                fagsakId
                tema
                sakstype
                fagsaksystem
              }
              journalforendeEnhet
              journalfortAvNavn
              journalpostId
              behandlingstema
              opprettetAvNavn
              eksternReferanseId
              kanal
              antallRetur
              journalposttype
              journalstatus
              tilleggsopplysninger {
                nokkel
                verdi
              }   
              relevanteDatoer {
                dato
                datotype
              }
              tema
              tittel
            }
        }
        """
    override fun getQuery(): String {
        return query
    }

    override fun getVariables(): HashMap<String, Any> {
        return hashMapOf("journalpostId" to journalpostId.toString())
    }
}
