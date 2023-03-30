package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer
import no.nav.bidrag.dokument.arkiv.dto.JournalStatus
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.dto.DokumentArkivSystemDto
import no.nav.bidrag.dokument.dto.DokumentFormatDto
import no.nav.bidrag.dokument.dto.DokumentMetadata
import no.nav.bidrag.dokument.dto.DokumentStatusDto
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class DokumentService(
    safConsumers: ResourceByDiscriminator<SafConsumer>,
    journalpostServices: ResourceByDiscriminator<JournalpostService>
) {
    private val safConsumer: SafConsumer
    private val journalpostService: JournalpostService

    init {
        safConsumer = safConsumers.get(Discriminator.REGULAR_USER)
        journalpostService = journalpostServices.get(Discriminator.REGULAR_USER)
    }

    fun hentDokument(journalpostId: Long, dokumentReferanse: String?): ResponseEntity<ByteArray> {
        LOGGER.info("Henter dokument med journalpostId=$journalpostId og dokumentReferanse=$dokumentReferanse")
        return safConsumer.hentDokument(journalpostId, java.lang.Long.valueOf(dokumentReferanse))
    }

    fun tilÅpneDokumentMetadata(journalStatus: JournalStatus?, dokumentReferanse: String?, journalpostId: Long?) = DokumentMetadata(
        arkivsystem = DokumentArkivSystemDto.JOARK,
        dokumentreferanse = dokumentReferanse,
        journalpostId = "JOARK-$journalpostId",
        format = DokumentFormatDto.PDF,
        status = when (journalStatus) {
            JournalStatus.RESERVERT, JournalStatus.UNDER_ARBEID -> DokumentStatusDto.UNDER_REDIGERING
            JournalStatus.AVBRUTT, JournalStatus.FEILREGISTRERT, JournalStatus.UTGAAR -> DokumentStatusDto.AVBRUTT
            else -> DokumentStatusDto.FERDIGSTILT
        }
    )

    fun hentDokumentMetadata(journalpostId: Long? = null, dokumentReferanse: String?): List<DokumentMetadata> {
        if (journalpostId == null && dokumentReferanse != null) {
            return listOf(
                journalpostService.finnTilknyttedeJournalposter(dokumentReferanse)
                    .map { tilÅpneDokumentMetadata(it.journalstatus, dokumentReferanse, it.journalpostId) }.first()
            )
        }

        val journalpost = journalpostService.hentJournalpost(journalpostId!!).orElse(null)
        return journalpost?.dokumenter?.map {
            tilÅpneDokumentMetadata(journalpost.journalstatus, it.dokumentInfoId, journalpostId)
        }?.filter { dokumentReferanse.isNullOrEmpty() || it.dokumentreferanse == dokumentReferanse } ?: emptyList()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DokumentService::class.java)
    }
}
