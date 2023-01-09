package no.nav.bidrag.dokument.arkiv.service.utvidelser

import no.nav.bidrag.dokument.dto.JournalpostType
import no.nav.bidrag.dokument.dto.OpprettJournalpostRequest

val OpprettJournalpostRequest.erNotat get() = journalposttype == JournalpostType.NOTAT