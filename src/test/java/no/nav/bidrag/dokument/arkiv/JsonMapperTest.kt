package no.nav.bidrag.dokument.arkiv

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.arkiv.dto.EndreJournalpostCommandIntern
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.JournalpostType
import no.nav.bidrag.dokument.arkiv.dto.LagreJournalpostRequest
import no.nav.bidrag.dokument.arkiv.query.DokumentoversiktFagsakQuery
import no.nav.bidrag.dokument.arkiv.query.JournalpostQuery
import no.nav.bidrag.transport.dokument.EndreDokument
import no.nav.bidrag.transport.dokument.EndreJournalpostCommand
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(BidragDokumentArkivConfig.PROFILE_TEST)
@DisplayName("Mapping av json verdier")
@SpringBootTest(classes = [BidragDokumentArkivTest::class])
@EnableMockOAuth2Server
internal class JsonMapperTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal mappe OppdaterJournalpost til json`() {
        val journalpost = Journalpost()
        journalpost.journalstatus = JournalStatus.MOTTATT
        journalpost.journalposttype = JournalpostType.I
        val endreDokument = EndreDokument(null, "55555", "55555", "Tittelen på dokument")
        val endreJournalpostCommand = EndreJournalpostCommand(
            avsenderNavn = "AvsenderNavn",
            endreDokumenter = listOf(endreDokument),
            fagomrade = "BID",
            gjelder = "1234",
            tittel = "Tittelen på journalposten",
            gjelderType = "FNR",
            tilknyttSaker = listOf("sakIdent")
        )
        val endreJournalpostIntern = EndreJournalpostCommandIntern(endreJournalpostCommand, "4805")
        val oppdaterJp = LagreJournalpostRequest(12345, endreJournalpostIntern, journalpost)
        val jsonMap = objectMapper.convertValue(oppdaterJp, MutableMap::class.java) as Map<String, Any>
        val jsonObjects = JsonObjects(jsonMap)
        assertAll(
            { assertThat(jsonObjects.objekt("avsenderMottaker")!!["navn"]).`as`("avsenderMottaker").isEqualTo("AvsenderNavn") },
            { assertThat(jsonObjects.objekt("bruker")!!["id"]).`as`("id").isEqualTo("1234") },
            { assertThat(jsonObjects.objekt("bruker")!!["idType"]).`as`("idType").isEqualTo("FNR") },
            { assertThat(jsonObjects.objekt("sak")!!["fagsakId"]).`as`("fagsakId").isEqualTo("sakIdent") },
            { assertThat(jsonObjects.objekt("sak")!!["fagsaksystem"]).`as`("fagsaksystem").isEqualTo("BISYS") },
            { assertThat(jsonObjects.objekt("sak")!!["sakstype"]).`as`("fagsaksystem").isEqualTo("FAGSAK") },
            { assertThat(jsonObjects.listeMedObjekter("dokumenter")!![0]["dokumentInfoId"]).`as`("dokumentInfoId").isEqualTo("55555") },
            { assertThat(jsonObjects.listeMedObjekter("dokumenter")!![0]["tittel"]).`as`("tittel").isEqualTo("Tittelen på dokument") },
            { assertThat(jsonMap["tema"]).`as`("tema").isEqualTo("BID") },
            { assertThat(jsonMap["tittel"]).`as`("tittel").isEqualTo("Tittelen på journalposten") }
        )
    }

    @Test
    fun `skal mappe json streng til Map`() {
        val opprettJournalpostRequestAsJson = java.lang.String.join(
            "\n",
            "{",
            "\"avsenderMottaker\": { \"navn\": \"Birger\" },",
            "\"behandlingstema\": \"BI01\",",
            "\"bruker\": { \"id\": \"06127412345\", \"idType\": \"FNR\" },",
            "\"dokumenter\": [{ \"brevkode\": \"BREVKODEN\", \"dokumentKategori\": \"dokumentKategori\", \"tittel\": \"Tittelen på dokumentet\" }],",
            "\"eksternReferanseId\": \"dokreferanse\",",
            "\"journalfoerendeEnhet\": \"666\",",
            "\"journalpostType\": \"N\",",
            "\"kanal\": \"nav.no\",",
            "\"sak\": { \"arkivsaksnummer\": \"1900001\", \"arkivsaksystem\": \"GSAK\" },",
            "\"tema\": \"BID\",",
            "\"tittel\": \"Tittelen på journalposten\"",
            "}"
        )
        val jsonMap = objectMapper.readValue(opprettJournalpostRequestAsJson, Map::class.java)
        assertAll(
            { assertThat(jsonMap["avsenderMottaker"]).`as`("avsenderMottaker").isNotNull() },
            { assertThat(jsonMap["behandlingstema"]).`as`("behandlingstema").isEqualTo("BI01") },
            { assertThat(jsonMap["bruker"]).`as`("bruker").isNotNull() },
            { assertThat(jsonMap["dokumenter"]).`as`("dokumenter").isNotNull() },
            { assertThat(jsonMap["eksternReferanseId"]).`as`("eksternReferanseId").isEqualTo("dokreferanse") },
            { assertThat(jsonMap["journalfoerendeEnhet"]).`as`("journalfoerendeEnhet").isEqualTo("666") },
            { assertThat(jsonMap["journalpostType"]).`as`("journalpostType").isEqualTo("N") },
            { assertThat(jsonMap["kanal"]).`as`("kanal").isEqualTo("nav.no") },
            { assertThat(jsonMap["sak"]).`as`("sak").isNotNull() },
            { assertThat(jsonMap["tema"]).`as`("tema").isEqualTo("BID") },
            { assertThat(jsonMap["tittel"]).`as`("tittel").isEqualTo("Tittelen på journalposten") }
        )
    }

    @Test
    fun `skal mappe saf dokumentOversiktFagsak query til Map`() {
        val safQuery = DokumentoversiktFagsakQuery("666", listOf("BID"))
        assertAll(
            { assertThat(safQuery.getQuery()).`as`("querystring").contains("fagsakId: \$fagsakId").contains("tema:\$tema") },
            { assertThat(safQuery.getVariables()).`as`("Variables").containsEntry("fagsakId", "666").containsEntry("tema", listOf("BID")) }
        )
    }

    @Test
    fun `skal mappe saf journalpost query til Map`() {
        val safQuery = JournalpostQuery(1235L)
        assertAll(
            { assertThat(safQuery.getQuery()).`as`("querystring").contains("journalpostId: \$journalpostId") },
            { assertThat(safQuery.getVariables()).`as`("Variables").containsEntry("journalpostId", "1235") }
        )
    }

    private class JsonObjects(private val jsonMap: Map<String, Any>) {
        fun objekt(key: String): Map<String, String>? {
            return jsonMap[key] as Map<String, String>?
        }

        fun listeMedObjekter(key: String): List<Map<String, Any>>? {
            return jsonMap[key] as List<Map<String, Any>>?
        }
    }
}
