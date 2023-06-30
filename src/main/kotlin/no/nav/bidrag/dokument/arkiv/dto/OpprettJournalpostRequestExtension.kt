package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.transport.dokument.AvsenderMottakerDto
import no.nav.bidrag.transport.dokument.AvsenderMottakerDtoIdType
import no.nav.bidrag.transport.dokument.IdentType
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest

fun OpprettJournalpostRequest.hasAvsenderMottaker(): Boolean = !avsenderMottaker?.navn.isNullOrEmpty() || !avsenderMottaker?.ident.isNullOrEmpty()
fun OpprettJournalpostRequest.hasSak(): Boolean = skalFerdigstilles && tilknyttSaker.isNotEmpty()
fun OpprettJournalpostRequest.hasGjelder(): Boolean = !gjelder?.ident.isNullOrEmpty() || !gjelderIdent.isNullOrEmpty()
fun OpprettJournalpostRequest.hentGjelderIdent() = gjelderIdent ?: gjelder?.ident
fun AvsenderMottakerDto.erSamhandler(): Boolean = type == AvsenderMottakerDtoIdType.SAMHANDLER

fun JoarkOpprettJournalpostRequest.hasAvsenderMottaker(): Boolean = !avsenderMottaker?.navn.isNullOrEmpty() || !avsenderMottaker?.id.isNullOrEmpty()
fun JoarkOpprettJournalpostRequest.hasSak(): Boolean = !sak?.fagsakId.isNullOrEmpty()
fun OpprettJournalpostRequest.hentGjelderType(): IdentType? = when (hentGjelderIdent()?.length) {
    11 -> IdentType.FNR
    13 -> IdentType.AKTOERID
    9 -> IdentType.ORGNR
    else -> null
}
