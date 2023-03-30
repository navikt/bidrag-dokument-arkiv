package no.nav.bidrag.dokument.arkiv.hendelser

import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal
import no.nav.bidrag.dokument.arkiv.model.JoarkHendelseType
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

fun createHendelseRecord(journalpostId: Long): JournalfoeringHendelseRecord {
    val record = JournalfoeringHendelseRecord()
    record.hendelsesId = "TEST_HENDELSE_ID"
    record.journalpostId = journalpostId
    record.hendelsesType = JoarkHendelseType.JOURNALPOST_MOTTATT.hendelsesType
    record.temaNytt = "BID"
    record.temaGammelt = "BID"
    record.kanalReferanseId = ""
    record.behandlingstema = "BID"
    record.journalpostStatus = "MOTTATT"
    record.mottaksKanal = JournalpostKanal.NAV_NO.name
    return record
}
