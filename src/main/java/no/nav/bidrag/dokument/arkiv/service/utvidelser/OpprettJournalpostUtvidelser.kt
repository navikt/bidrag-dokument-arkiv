package no.nav.bidrag.dokument.arkiv.service.utvidelser

import no.nav.bidrag.dokument.arkiv.consumer.dto.Brukernotifikasjonstype
import no.nav.bidrag.dokument.arkiv.consumer.dto.DokumentSoknadDto
import no.nav.bidrag.dokument.arkiv.consumer.dto.EksternEttersendingsOppgave
import no.nav.bidrag.dokument.arkiv.consumer.dto.InnsendtVedleggDto
import no.nav.bidrag.dokument.arkiv.dto.EttersendingsoppgaveDo
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.domene.enums.diverse.Språk
import no.nav.bidrag.transport.dokument.JournalpostType
import no.nav.bidrag.transport.dokument.OpprettEttersendingsppgaveDto
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest

val OpprettJournalpostRequest.erNotat get() = journalposttype == JournalpostType.NOTAT
fun OpprettEttersendingsppgaveDto.toTilleggsopplysning() = EttersendingsoppgaveDo(
    tittel = tittel,
    skjemaId = skjemaId,
    språk = språk.name,
    innsendingsFristDager = innsendingsFristDager,
    vedleggsliste = vedleggsliste.map {
        EttersendingsoppgaveDo.EttersendingsoppgaveVedleggDo(
            vedleggsnr = it.vedleggsnr,
            url = it.url,
            tittel = it.tittel,
        )
    },
)

fun OpprettEttersendingsppgaveDto.tilRequest(journalpost: Journalpost) = EksternEttersendingsOppgave(
    brukerId = journalpost.hentGjelderId()!!,
    skjemanr = skjemaId,
    sprak = when (språk) {
        Språk.NB -> "nb_NO"
        Språk.NN -> "nn_NO"
        Språk.DE -> "de_DE"
        Språk.EN -> "en_GB"
        Språk.FR -> "fr_FR"
        else -> "en_GB"
    },
    tittel = tittel,
    tema = journalpost.tema!!,
    innsendingsFristDager = innsendingsFristDager,
    brukernotifikasjonstype = Brukernotifikasjonstype.oppgave,
    koblesTilEksisterendeSoknad = false,
    vedleggsListe =
    vedleggsliste.map {
        InnsendtVedleggDto(
            vedleggsnr = it.vedleggsnr,
            url = it.url,
            tittel = it.tittel,
        )
    },
)

fun DokumentSoknadDto.tilTilleggsopplysning() = EttersendingsoppgaveDo(
    tittel = tittel,
    skjemaId = skjemanr,
    språk = spraak ?: "nb",
    slettesDato = skalSlettesDato?.toLocalDate(),
    innsendingsId = innsendingsId!!,
    innsendingsFristDager = fristForEttersendelse?.toInt() ?: 14,
    fristDato = innsendingsFristDato?.toLocalDate(),
    vedleggsliste = vedleggsListe.map {
        EttersendingsoppgaveDo.EttersendingsoppgaveVedleggDo(
            vedleggsnr = it.vedleggsnr!!,
            url = it.skjemaurl,
            tittel = it.tittel,
        )
    },
)
