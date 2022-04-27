package no.nav.bidrag.dokument.arkiv.stubs

import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker
import no.nav.bidrag.dokument.arkiv.dto.Bruker
import no.nav.bidrag.dokument.arkiv.dto.DatoType
import no.nav.bidrag.dokument.arkiv.dto.Dokument
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.JournalpostType
import no.nav.bidrag.dokument.arkiv.dto.OppgaveData
import no.nav.bidrag.dokument.arkiv.dto.Sak
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse


var X_ENHET_HEADER = "1234"
var BRUKER_ENHET = "4899"
var JOURNALPOST_ID = 123213213L
var JOURNALPOST_ID_2 = 55513213L
var JOURNALPOST_ID_3 = 23421321L
var NY_JOURNALPOST_ID_KNYTT_TIL_SAK = 23423331321L

var DOKUMENT_1_ID = "123123";
var DOKUMENT_1_TITTEL = "Tittel på dokument 1";
var DOKUMENT_2_ID = "523123";
var DOKUMENT_2_TITTEL = "Tittel på dokument 2";
var DOKUMENT_3_ID = "423123";
var DOKUMENT_3_TITTEL = "Tittel på dokument 3";

var AVSENDER_ID = "112312385076492416";
var AVSENDER_NAVN = "Avsender Avsendersen";
var BRUKER_AKTOER_ID = "123213213213";
var BRUKER_FNR = "333232323";
var BRUKER_TYPE_AKTOERID = "AKTOERID";

var BREVKODE = "BI01S02";
var JOURNALFORENDE_ENHET = "4833";
var DATO_DOKUMENT = DatoType("2021-08-18T13:20:33", "DATO_DOKUMENT")
var DATO_RETUR = DatoType("2021-08-18T13:20:33", "DATO_AVS_RETUR")

var TILLEGGSOPPLYSNINGER_RETUR: MutableList<Map<String, String>> =
    mutableListOf(mapOf("nokkel" to "retur0_2020-11-15", "verdi" to "Beskrivelse av retur"))


fun createDistribuerTilAdresse(): DistribuerTilAdresse {
    return DistribuerTilAdresse(
        adresselinje1 = "Adresselinje1",
        adresselinje2 = null,
        adresselinje3 = null,
        land = "NO",
        postnummer = "3000",
        poststed = "Ingen"
    )
}

fun opprettSafResponse(
    journalpostId: String = JOURNALPOST_ID.toString(),
    avsenderMottaker: AvsenderMottaker = AvsenderMottaker(AVSENDER_NAVN, AVSENDER_ID),
    bruker: Bruker = Bruker(BRUKER_AKTOER_ID, BRUKER_TYPE_AKTOERID),
    dokumenter: List<Dokument> = listOf(
        Dokument(
            dokumentInfoId = DOKUMENT_1_ID,
            tittel = DOKUMENT_1_TITTEL
        )
    ),
    tittel: String = DOKUMENT_1_TITTEL,
    journalforendeEnhet: String = JOURNALFORENDE_ENHET,
    journalpostType: JournalpostType = JournalpostType.I,
    journalstatus: JournalStatus = JournalStatus.MOTTATT,
    relevanteDatoer: List<DatoType> = listOf(DATO_DOKUMENT),
    tema: String = "BID",
    sak: Sak? = Sak("5276661")
): Journalpost {
    return Journalpost(
        avsenderMottaker = avsenderMottaker,
        bruker = bruker,
        dokumenter = dokumenter,
        journalforendeEnhet = journalforendeEnhet,
        journalpostId = journalpostId,
        journalposttype = journalpostType,
        journalstatus = journalstatus,
        relevanteDatoer = relevanteDatoer,
        tema = tema,
        tittel = tittel,
        sak = sak
    )
}

fun opprettDokumentOversiktfagsakResponse(): List<Journalpost>{
    return listOf(
        opprettSafResponse(
            journalpostId = JOURNALPOST_ID.toString(),
            dokumenter = listOf(
                Dokument(
                    tittel = DOKUMENT_1_TITTEL,
                    dokumentInfoId = DOKUMENT_1_ID
                )
            ),
            journalstatus = JournalStatus.JOURNALFOERT,
            tema = "BID"
        ),
        opprettSafResponse(
            journalpostId = JOURNALPOST_ID_2.toString(),
            dokumenter = listOf(Dokument(
                tittel = DOKUMENT_2_TITTEL,
                dokumentInfoId = DOKUMENT_2_ID
            )),
            journalstatus = JournalStatus.JOURNALFOERT,
            tema = "BID"

        ),
        opprettSafResponse(
            journalpostId = JOURNALPOST_ID_3.toString(),
            dokumenter = listOf(Dokument(
                tittel = DOKUMENT_3_TITTEL,
                dokumentInfoId = DOKUMENT_3_ID
            )),
            tema = "FAR",
            journalstatus = JournalStatus.MOTTATT
        )

    )
}
fun createOppgaveDataWithSaksnummer(saksnummer: String): OppgaveData{
    return OppgaveData(saksreferanse = saksnummer, id=2, versjon = 1, beskrivelse = "")
}
fun createOppgaveDataWithJournalpostId(journalpostId: String): OppgaveData{
    return OppgaveData(journalpostId = journalpostId, id=2, versjon = 1, beskrivelse = "")
}