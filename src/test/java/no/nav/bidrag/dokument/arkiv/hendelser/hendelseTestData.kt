package no.nav.bidrag.dokument.arkiv.hendelser

import no.nav.bidrag.dokument.arkiv.dto.MottaksKanal
import no.nav.bidrag.dokument.arkiv.model.HendelsesType
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

fun createHendelseRecord(journalpostId: Long): JournalfoeringHendelseRecord {
    val record = JournalfoeringHendelseRecord()
    record.hendelsesId = "TEST_HENDELSE_ID"
    record.journalpostId = journalpostId
    record.hendelsesType = HendelsesType.JOURNALPOST_MOTTATT.hendelsesType
    record.temaNytt = "BID"
    record.temaGammelt = "BID"
    record.kanalReferanseId = ""
    record.behandlingstema = "BID"
    record.journalpostStatus = "MOTTATT"
    record.mottaksKanal = MottaksKanal.NAV_NO.name
    return record
}