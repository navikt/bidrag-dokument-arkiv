package no.nav.bidrag.dokument.arkiv.service.utvidelser

import no.nav.bidrag.transport.dokument.JournalpostType
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest

val OpprettJournalpostRequest.erNotat get() = journalposttype == JournalpostType.NOTAT
