package no.nav.bidrag.dokument.arkiv.query

data class TilknyttedeJournalposterQuery(val dokumentInfoId: String) : GraphQuery() {
    private val query = """
        query tilknyttedeJournalposter(${"$"}dokumentInfoId: String!) {
             tilknyttedeJournalposter(dokumentInfoId: ${"$"}dokumentInfoId, tilknytning: GJENBRUK) {
                    journalpostId
                    journalstatus
                    sak {
                        fagsakId
                        fagsaksystem
                        sakstype
                        tema
                    }
                }
        }
        """
    override fun getQuery(): String {
        return query
    }

    override fun getVariables(): HashMap<String, Any> {
        return hashMapOf("dokumentInfoId" to dokumentInfoId)
    }
}
