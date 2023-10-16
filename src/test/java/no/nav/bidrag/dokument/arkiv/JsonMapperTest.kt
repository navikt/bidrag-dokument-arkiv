package no.nav.bidrag.dokument.arkiv

import com.fasterxml.jackson.core.JsonProcessingException
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
import no.nav.bidrag.transport.dokument.IdentType
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
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
    @DisplayName("skal mappe OppdaterJournalpost til json")
    fun skalMappeOppdaterJournalpostTilJson() {
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
            gjelderType = IdentType.FNR,
            tilknyttSaker = listOf("sakIdent")
        )
        val endreJournalpostIntern = EndreJournalpostCommandIntern(endreJournalpostCommand, "4805")
        val oppdaterJp = LagreJournalpostRequest(12345, endreJournalpostIntern, journalpost)
        val jsonMap = objectMapper.convertValue(
            oppdaterJp,
            MutableMap::class.java
        ) as Map<String, Any>
        val jsonObjects = JsonObjects(jsonMap)
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(
                    jsonObjects.objekt("avsenderMottaker")!!["navn"]
                )
                    .`as`("avsenderMottaker")
                    .isEqualTo("AvsenderNavn")
            },
            Executable {
                Assertions.assertThat(
                    jsonObjects.objekt("bruker")!!["id"]
                ).`as`("id").isEqualTo("1234")
            },
            Executable {
                Assertions.assertThat(
                    jsonObjects.objekt("bruker")!!["idType"]
                ).`as`("idType").isEqualTo("FNR")
            },
            Executable {
                Assertions.assertThat(
                    jsonObjects.objekt("sak")!!["fagsakId"]
                )
                    .`as`("fagsakId")
                    .isEqualTo("sakIdent")
            },
            Executable {
                Assertions.assertThat(
                    jsonObjects.objekt("sak")!!["fagsaksystem"]
                )
                    .`as`("fagsaksystem")
                    .isEqualTo("BISYS")
            },
            Executable {
                Assertions.assertThat(
                    jsonObjects.objekt("sak")!!["sakstype"]
                )
                    .`as`("fagsaksystem")
                    .isEqualTo("FAGSAK")
            },
            Executable {
                Assertions.assertThat(
                    jsonObjects.listeMedObjekter("dokumenter")!![0]["dokumentInfoId"]
                )
                    .`as`("dokumentInfoId")
                    .isEqualTo("55555")
            },
            Executable {
                Assertions.assertThat(
                    jsonObjects.listeMedObjekter("dokumenter")!![0]["tittel"]
                )
                    .`as`("tittel")
                    .isEqualTo("Tittelen på dokument")
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["tema"]
                ).`as`("tema").isEqualTo("BID")
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["tittel"]
                ).`as`("tittel").isEqualTo("Tittelen på journalposten")
            }
        )
    }

    @Test
    @DisplayName("skal mappe json streng til java.util.Map")
    @Throws(
        JsonProcessingException::class
    )
    fun skalMappeJsonRequest() {
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
        val jsonMap: Map<*, *> = objectMapper.readValue(
            opprettJournalpostRequestAsJson,
            MutableMap::class.java
        )
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(
                    jsonMap["avsenderMottaker"]
                ).`as`("avsenderMottaker").isNotNull()
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["behandlingstema"]
                ).`as`("behandlingstema").isEqualTo("BI01")
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["bruker"]
                ).`as`("bruker").isNotNull()
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["dokumenter"]
                ).`as`("dokumenter").isNotNull()
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["eksternReferanseId"]
                )
                    .`as`("eksternReferanseId")
                    .isEqualTo("dokreferanse")
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["journalfoerendeEnhet"]
                )
                    .`as`("journalfoerendeEnhet")
                    .isEqualTo("666")
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["journalpostType"]
                ).`as`("journalpostType").isEqualTo("N")
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["kanal"]
                ).`as`("kanal").isEqualTo("nav.no")
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["sak"]
                ).`as`("sak").isNotNull()
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["tema"]
                ).`as`("tema").isEqualTo("BID")
            },
            Executable {
                Assertions.assertThat(
                    jsonMap["tittel"]
                ).`as`("tittel").isEqualTo("Tittelen på journalposten")
            }
        )
    }

    @Test
    @DisplayName("skal mappe saf dokumentOversiktFagsak query til java.util.Map")
    fun skalMappeSafDokumentOversiktQueryTilMap() {
        val safQuery = DokumentoversiktFagsakQuery("666", listOf("BID"))
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(safQuery.getQuery())
                    .`as`("querystring")
                    .contains("fagsakId: \$fagsakId")
                    .contains("tema:\$tema")
            },
            Executable {
                Assertions.assertThat(safQuery.getVariables())
                    .`as`("Variables")
                    .containsEntry("fagsakId", "666")
                    .containsEntry("tema", listOf("BID"))
            }
        )
    }

    @Test
    @DisplayName("skal mappe saf journalpost query til java.util.Map")
    fun skalMappeSafJournalpostQueryTilMap() {
        val safQuery = JournalpostQuery(1235L)
        org.junit.jupiter.api.Assertions.assertAll(
            Executable {
                Assertions.assertThat(safQuery.getQuery())
                    .`as`("querystring")
                    .contains("journalpostId: \$journalpostId")
            },
            Executable {
                Assertions.assertThat(safQuery.getVariables())
                    .`as`("Variables")
                    .containsEntry("journalpostId", "1235")
            }
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
