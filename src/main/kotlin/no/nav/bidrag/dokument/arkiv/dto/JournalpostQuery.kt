package no.nav.bidrag.dokument.arkiv.dto

data class JournalpostQuery(val journalpostId: Int) : GraphQuery() {
    private val query ="""
        {
            journalpost(journalpostId: "$journalpostId") {
              avsenderMottaker {
                navn
              }
              bruker {
                id
                type
              }
              dokumenter {
                tittel
              }
              sak {
                fagsakId
              }
              journalforendeEnhet
              journalfortAvNavn
              journalpostId
              journalposttype
              journalstatus
              relevanteDatoer {
                dato
                datotype
              }
              tema
              tittel
            }
        }
        """
    private val query2 = String.format("{\n" +
        "journalpost(journalpostId: \"%s\") {\n" +
        " avsenderMottaker {\n" +
        "    navn\n" +
        " }\n" +
        " bruker {\n" +
        "   id\n" +
        "   type\n" +
        " }\n" +
        " dokumenter {\n" +
        "    tittel\n" +
        " }\n" +
        " journalforendeEnhet\n" +
        " journalfortAvNavn\n" +
        " journalpostId\n" +
        " journalposttype\n" +
        " journalstatus\n" +
        " relevanteDatoer {\n" +
        "   dato\n" +
        "   datotype\n" +
        " }\n" +
        " sak { \n" +
        "   fagsakId\n" +
        " }\n" +
        " tema\n" +
        " tittel\n" +
        "}\n" +
    "}", journalpostId);
    override fun getQuery(): String {
        return query
    }
}
