package no.nav.bidrag.dokument.arkiv.query

data class DokumentoversiktFagsakQuery(val saksnummer: String, val tema: String) : GraphQuery() {
    private val query = """
        query dokumentoversiktFagsak(${"$"}fagsakId: String!, ${"$"}tema: [Tema]){
            dokumentoversiktFagsak(fagsak: {fagsakId: ${"$"}fagsakId, fagsaksystem: "BISYS"}, tema:${"$"}tema, foerste: 500) {
              journalposter {
                avsenderMottaker {
                  navn
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
                kanal
                journalforendeEnhet
                journalfortAvNavn
                journalpostId
                journalposttype
                journalstatus
                antallRetur
                tilleggsopplysninger {
                    nokkel
                    verdi
                } 
                relevanteDatoer {
                  dato
                  datotype
                }
                sak {
                  fagsakId
                }
                tema
                tittel
              }
            }
        }
        """.trimIndent()

    override fun getQuery(): String {
        return query
    }

    override fun getVariables(): HashMap<String, Any> {
        return hashMapOf("fagsakId" to saksnummer, "tema" to tema)
    }
}