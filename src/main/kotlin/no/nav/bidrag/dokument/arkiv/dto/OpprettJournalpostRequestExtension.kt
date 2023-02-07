package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.dto.AvsenderMottakerDto
import no.nav.bidrag.dokument.dto.AvsenderMottakerDtoIdType
import no.nav.bidrag.dokument.dto.JournalpostType
import no.nav.bidrag.dokument.dto.OpprettJournalpostRequest

fun OpprettJournalpostRequest.hasAvsenderMottaker(): Boolean = !avsenderMottaker?.navn.isNullOrEmpty() || !avsenderMottaker?.ident.isNullOrEmpty()
fun OpprettJournalpostRequest.hasSak(): Boolean = skalFerdigstilles && tilknyttSaker.isNotEmpty()
fun OpprettJournalpostRequest.hasGjelder(): Boolean = !gjelder?.ident.isNullOrEmpty() || !gjelderIdent.isNullOrEmpty()
fun OpprettJournalpostRequest.hentJournalførendeEnhet(): String? = journalførendeEnhet ?: journalfoerendeEnhet
fun OpprettJournalpostRequest.hentGjelderIdent() = gjelderIdent ?: gjelder?.ident
fun AvsenderMottakerDto.erSamhandler(): Boolean = type == AvsenderMottakerDtoIdType.SAMHANDLER

fun JoarkOpprettJournalpostRequest.hasAvsenderMottaker(): Boolean = !avsenderMottaker?.navn.isNullOrEmpty() || !avsenderMottaker?.id.isNullOrEmpty()
fun JoarkOpprettJournalpostRequest.hasSak(): Boolean = !sak?.fagsakId.isNullOrEmpty()
fun OpprettJournalpostRequest.hentGjelderType(): BrukerIdType? = when(hentGjelderIdent()?.length){
                                                                    11 -> BrukerIdType.FNR
                                                                    13 -> BrukerIdType.AKTOERID
                                                                    9 -> BrukerIdType.ORGNR
                                                                    else -> null
                                                                 }