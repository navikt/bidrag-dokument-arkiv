package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.consumer.SafConsumer
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.dto.DokumentArkivSystemDto
import no.nav.bidrag.dokument.dto.DokumentFormatDto
import no.nav.bidrag.dokument.dto.DokumentStatusDto
import no.nav.bidrag.dokument.dto.ÅpneDokumentMetadata
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class DokumentService(
    safConsumers: ResourceByDiscriminator<SafConsumer>,
    journalpostServices: ResourceByDiscriminator<JournalpostService>,
) {
    private val safConsumer: SafConsumer
    private val journalpostService: JournalpostService

    init {
        safConsumer = safConsumers.get(Discriminator.REGULAR_USER)
        journalpostService = journalpostServices.get(Discriminator.REGULAR_USER)
    }

    fun hentDokument(journalpostId: Long?, dokumentReferanse: String?): ResponseEntity<ByteArray> {
        LOGGER.info("Henter dokument med journalpostId={} og dokumentReferanse={}", journalpostId, dokumentReferanse)
        return safConsumer.hentDokument(journalpostId, java.lang.Long.valueOf(dokumentReferanse))
    }

    fun hentDokumentMetadata(journalpostId: Long? = null, dokumentReferanse: String?): List<ÅpneDokumentMetadata> {
        if (journalpostId == null && dokumentReferanse != null){
            return listOf(journalpostService.finnTilknyttedeJournalposter(dokumentReferanse)
                .map { ÅpneDokumentMetadata(
                    arkivsystem = DokumentArkivSystemDto.JOARK,
                    dokumentreferanse = dokumentReferanse,
                    journalpostId = "JOARK-${it.journalpostId}",
                    format = DokumentFormatDto.PDF,
                    status = DokumentStatusDto.FERDIGSTILT
                ) }.first())
        }

        return journalpostService.hentJournalpost(journalpostId).orElse(null)?.dokumenter?.map {
            ÅpneDokumentMetadata(
                arkivsystem = DokumentArkivSystemDto.JOARK,
                dokumentreferanse = it.dokumentInfoId,
                journalpostId = "JOARK-$journalpostId",
                format = DokumentFormatDto.PDF,
                status = DokumentStatusDto.FERDIGSTILT
            )
        }?.filter { dokumentReferanse.isNullOrEmpty() || it.dokumentreferanse == dokumentReferanse } ?: emptyList()

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DokumentService::class.java)
    }
}