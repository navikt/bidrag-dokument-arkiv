package no.nav.bidrag.dokument.arkiv.service

import no.nav.bidrag.dokument.arkiv.BidragDokumentArkiv
import no.nav.bidrag.dokument.arkiv.consumer.BidragOrganisasjonConsumer
import no.nav.bidrag.dokument.arkiv.consumer.DokarkivConsumer
import no.nav.bidrag.dokument.arkiv.consumer.PersonConsumer
import no.nav.bidrag.dokument.arkiv.dto.AvsenderMottaker
import no.nav.bidrag.dokument.arkiv.dto.AvvikshendelseIntern
import no.nav.bidrag.dokument.arkiv.dto.BestillOriginalOppgaveRequest
import no.nav.bidrag.dokument.arkiv.dto.BestillReskanningOppgaveRequest
import no.nav.bidrag.dokument.arkiv.dto.BestillSplittingoppgaveRequest
import no.nav.bidrag.dokument.arkiv.dto.Dokument
import no.nav.bidrag.dokument.arkiv.dto.EndreFagomradeRequest
import no.nav.bidrag.dokument.arkiv.dto.Fagomrade
import no.nav.bidrag.dokument.arkiv.dto.FerdigstillJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.JoarkOpprettJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.Journalpost
import no.nav.bidrag.dokument.arkiv.dto.JournalpostKanal
import no.nav.bidrag.dokument.arkiv.dto.JournalpostUtsendingKanal
import no.nav.bidrag.dokument.arkiv.dto.LagreAvsenderNavnRequest
import no.nav.bidrag.dokument.arkiv.dto.OppdaterJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.OppdaterOriginalBestiltFlagg
import no.nav.bidrag.dokument.arkiv.dto.OppgaveEnhet
import no.nav.bidrag.dokument.arkiv.dto.OpphevEndreFagomradeJournalfortJournalpostRequest
import no.nav.bidrag.dokument.arkiv.dto.OverforEnhetRequest
import no.nav.bidrag.dokument.arkiv.dto.RegistrerReturRequest
import no.nav.bidrag.dokument.arkiv.dto.ReturDetaljerLogDO
import no.nav.bidrag.dokument.arkiv.dto.Saksbehandler
import no.nav.bidrag.dokument.arkiv.dto.SaksbehandlerMedEnhet
import no.nav.bidrag.dokument.arkiv.dto.TilknyttetJournalpost
import no.nav.bidrag.dokument.arkiv.dto.bestillReskanningKommentar
import no.nav.bidrag.dokument.arkiv.dto.bestillSplittingKommentar
import no.nav.bidrag.dokument.arkiv.dto.med
import no.nav.bidrag.dokument.arkiv.dto.dupliserJournalpost
import no.nav.bidrag.dokument.arkiv.dto.fjern
import no.nav.bidrag.dokument.arkiv.dto.opprettDokumentVariant
import no.nav.bidrag.dokument.arkiv.dto.validateTrue
import no.nav.bidrag.dokument.arkiv.kafka.HendelserProducer
import no.nav.bidrag.dokument.arkiv.model.AvvikDetaljException
import no.nav.bidrag.dokument.arkiv.model.AvvikNotSupportedException
import no.nav.bidrag.dokument.arkiv.model.Discriminator
import no.nav.bidrag.dokument.arkiv.model.ResourceByDiscriminator
import no.nav.bidrag.dokument.arkiv.model.UgyldigAvvikException
import no.nav.bidrag.dokument.arkiv.security.SaksbehandlerInfoManager
import no.nav.bidrag.dokument.dto.AvvikType
import no.nav.bidrag.dokument.dto.BehandleAvvikshendelseResponse
import no.nav.bidrag.dokument.dto.DokumentDto
import no.nav.bidrag.dokument.dto.JournalpostIkkeFunnetException
import org.apache.logging.log4j.util.Strings
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Base64
import java.util.Optional
import java.util.function.Consumer
import java.util.stream.Stream

@Service
class AvvikService(
    val hendelserProducer: HendelserProducer,
    val endreJournalpostService: EndreJournalpostService,
    val distribuerJournalpostService: DistribuerJournalpostService,
    val oppgaveService: OppgaveService,
    val saksbehandlerInfoManager: SaksbehandlerInfoManager,
    val opprettJournalpostService: OpprettJournalpostService,
    dokarkivConsumers: ResourceByDiscriminator<DokarkivConsumer?>,
    personConsumers: ResourceByDiscriminator<PersonConsumer?>,
    journalpostService: ResourceByDiscriminator<JournalpostService?>,
) {
    private final val journalpostService: JournalpostService
    private final val dokarkivConsumer: DokarkivConsumer
    private final val personConsumer: PersonConsumer

    init {
        this.journalpostService = journalpostService.get(Discriminator.REGULAR_USER)
        this.personConsumer = personConsumers.get(Discriminator.REGULAR_USER)
        this.dokarkivConsumer = dokarkivConsumers.get(Discriminator.REGULAR_USER)
    }

    fun hentAvvik(jpid: Long): List<AvvikType> {
        return journalpostService.hentJournalpost(jpid).get().tilAvvik()
    }

    fun behandleAvvik(behandleAvvikRequest: AvvikshendelseIntern): Optional<BehandleAvvikshendelseResponse> {
        return journalpostService.hentJournalpost(behandleAvvikRequest.journalpostId)
            .map { jp: Journalpost -> behandleAvvik(jp, behandleAvvikRequest) }
            .orElseThrow { JournalpostIkkeFunnetException("Fant ikke journalpost med id lik " + behandleAvvikRequest.journalpostId) }
    }

    fun behandleAvvik(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern): Optional<BehandleAvvikshendelseResponse> {
        if (!erGyldigAvviksBehandling(journalpost, avvikshendelseIntern.avvikstype)) {
            throw UgyldigAvvikException(
                String.format(
                    "Ikke gyldig avviksbehandling %s for journalpost %s", avvikshendelseIntern.avvikstype,
                    avvikshendelseIntern.journalpostId
                )
            )
        }
        when (avvikshendelseIntern.avvikstype) {
            AvvikType.BESTILL_ORIGINAL -> bestillOriginal(journalpost, avvikshendelseIntern)
            AvvikType.BESTILL_SPLITTING -> bestillSplitting(journalpost, avvikshendelseIntern)
            AvvikType.BESTILL_RESKANNING -> bestillReskanning(journalpost, avvikshendelseIntern)
            AvvikType.KOPIER_FRA_ANNEN_FAGOMRADE -> kopierFraAnnenFagomrade(journalpost, avvikshendelseIntern)
            AvvikType.OVERFOR_TIL_ANNEN_ENHET -> overforJournalpostTilEnhet(journalpost, avvikshendelseIntern.enhetsnummerNytt)
            AvvikType.ENDRE_FAGOMRADE -> endreFagomrade(journalpost, avvikshendelseIntern)
            AvvikType.SEND_TIL_FAGOMRADE -> onlyLogging()
            AvvikType.TREKK_JOURNALPOST -> trekkJournalpost(journalpost, avvikshendelseIntern)
            AvvikType.FEILFORE_SAK -> feilregistrerSakstilknytning(avvikshendelseIntern.journalpostId)
            AvvikType.REGISTRER_RETUR -> registrerRetur(journalpost, avvikshendelseIntern)
            AvvikType.BESTILL_NY_DISTRIBUSJON -> bestillNyDistribusjon(journalpost, avvikshendelseIntern)
            AvvikType.MANGLER_ADRESSE -> manglerAdresse(journalpost)
            AvvikType.FARSKAP_UTELUKKET -> farskapUtelukket(journalpost, avvikshendelseIntern)
            else -> throw AvvikNotSupportedException("Avvik ${avvikshendelseIntern.avvikstype} ikke støttet")
        }
        publiserHendelse(journalpost, avvikshendelseIntern.saksbehandlersEnhet)
        BidragDokumentArkiv.SECURE_LOGGER.info(
            "Avvik {} ble utført på journalpost {} av bruker {} og enhet {} med beskrivelse {} - avvik {}",
            avvikshendelseIntern.avvikstype, avvikshendelseIntern.journalpostId, saksbehandlerInfoManager.hentSaksbehandlerBrukerId(),
            avvikshendelseIntern.saksbehandlersEnhet, avvikshendelseIntern.beskrivelse, avvikshendelseIntern
        )
        return Optional.of(BehandleAvvikshendelseResponse(avvikshendelseIntern.avvikstype))
    }

    private fun publiserHendelse(journalpost: Journalpost, enhet: String?) {
        if (journalpost.isInngaaendeDokument()) {
            hendelserProducer.publishJournalpostUpdated(journalpost.hentJournalpostIdLong()!!, enhet)
        }
    }

    private fun bestillSplitting(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        if (avvikshendelseIntern.beskrivelse.isNullOrEmpty()) {
            throw UgyldigAvvikException("Avvik bestill splitting må inneholde beskrivelse")
        }
        val saksbehandler = hentSaksbehandler(avvikshendelseIntern.saksbehandlersEnhet!!)
        if (journalpost.isStatusJournalfort()) {
            oppgaveService.opprettOppgaveTilFagpost(BestillSplittingoppgaveRequest(journalpost, saksbehandler, avvikshendelseIntern.beskrivelse))
            dokarkivConsumer.feilregistrerSakstilknytning(journalpost.hentJournalpostIdLong())
        } else {
            val beskrivelse = bestillSplittingKommentar(avvikshendelseIntern.beskrivelse)
            oppgaveService.leggTilKommentarPaaJournalforingsoppgave(journalpost, saksbehandler, beskrivelse)
            overforJournalpostTilEnhet(journalpost, OppgaveEnhet.FAGPOST)
        }
    }

    private fun bestillReskanning(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        val saksbehandler = hentSaksbehandler(avvikshendelseIntern.saksbehandlersEnhet!!)
        if (journalpost.isStatusJournalfort()) {
            oppgaveService.opprettOppgaveTilFagpost(BestillReskanningOppgaveRequest(journalpost, saksbehandler, avvikshendelseIntern.beskrivelse))
            dokarkivConsumer.feilregistrerSakstilknytning(journalpost.hentJournalpostIdLong())
        } else {
            val beskrivelse = bestillReskanningKommentar(avvikshendelseIntern.beskrivelse)
            oppgaveService.leggTilKommentarPaaJournalforingsoppgave(journalpost, saksbehandler, beskrivelse)
            overforJournalpostTilEnhet(journalpost, OppgaveEnhet.FAGPOST)
        }
    }

    private fun bestillOriginal(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        val saksbehandler = hentSaksbehandler(avvikshendelseIntern.saksbehandlersEnhet!!)
        oppgaveService.opprettOppgaveTilFagpost(
            BestillOriginalOppgaveRequest(
                journalpost,
                avvikshendelseIntern.enhetsnummer,
                saksbehandler,
                avvikshendelseIntern.beskrivelse
            )
        )
        dokarkivConsumer.endre(OppdaterOriginalBestiltFlagg(journalpost))
    }

    private fun overforJournalpostTilEnhet(journalpost: Journalpost, enhet: String) {
        if (journalpost.journalforendeEnhet != enhet) {
            dokarkivConsumer.endre(OverforEnhetRequest(journalpost.hentJournalpostIdLong()!!, enhet))
        }
    }

    private fun hentSaksbehandler(enhet: String): SaksbehandlerMedEnhet {
        return saksbehandlerInfoManager.hentSaksbehandler()
            .map { it.tilEnhet(enhet) }
            .orElseGet { SaksbehandlerMedEnhet(Saksbehandler(), enhet) }
    }


    fun kopierFraAnnenFagomrade(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        if (journalpost.isUtgaaendeDokument()) {
            throw UgyldigAvvikException("Kan ikke kopiere en utgående dokument fra annen fagområde")
        }
        if (journalpost.isTemaBidrag()) {
            throw UgyldigAvvikException("Kan ikke kopiere journalpost som allerede tilhører Bidrag")
        }
        if (avvikshendelseIntern.dokumenter != null && avvikshendelseIntern.dokumenter!!.isEmpty()) {
            throw UgyldigAvvikException("Kopiert journalpost må inneholde minst en dokument")
        }
        val knyttTilSaker = avvikshendelseIntern.knyttTilSaker
        if (knyttTilSaker.isEmpty()) {
            throw UgyldigAvvikException("Fant ingen saker i request. Journalpost må knyttes til minst en sak")
        }

        val hoveddokumentTittel = avvikshendelseIntern.dokumenter!![0].tittel
        val nyJournalpostTittel = hoveddokumentTittel ?: journalpost.hentTittel()!!

        val request = dupliserJournalpost(journalpost) {
            med journalførendeenhet avvikshendelseIntern.saksbehandlersEnhet
            med tittel "$nyJournalpostTittel (Kopiert fra dokument: ${journalpost.hentTittel()})"
            avvikshendelseIntern.dokumenter!!.forEach(Consumer { (dokumentreferanse, _, _, tittel, dokument, brevkode): DokumentDto ->
                val dokumentByte = if (Strings.isNotEmpty(dokument)) Base64.getDecoder().decode(dokument) else null
                +JoarkOpprettJournalpostRequest.Dokument(
                    dokumentInfoId = dokumentreferanse,
                    brevkode = brevkode,
                    tittel = tittel,
                    dokumentvarianter = if (dokumentByte != null) listOf(opprettDokumentVariant(dokumentByte = dokumentByte)) else emptyList()
                )
            })
        }

        val (journalpostId) = opprettJournalpostService.opprettJournalpost(
            request,
            avvikshendelseIntern.knyttTilSaker,
            journalpost.hentJournalpostIdLong(),
            skalFerdigstilles = true
        )
        LOGGER.info("Kopiert journalpost {} til Bidrag, ny journalpostId {}", journalpost.journalpostId, journalpostId)
        oppgaveService.ferdigstillVurderDokumentOppgaver(journalpost.hentJournalpostIdLong()!!, avvikshendelseIntern.saksbehandlersEnhet!!)
    }

    fun farskapUtelukket(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        dokarkivConsumer.endre(avvikshendelseIntern.toLeggTiLFarskapUtelukketTilTittelRequest(journalpost))
    }

    fun manglerAdresse(journalpost: Journalpost) {
        oppdaterDistribusjonsInfoIngenDistribusjon(journalpost)
    }

    fun bestillNyDistribusjon(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        LOGGER.info("Bestiller ny distribusjon for journalpost {}", journalpost.journalpostId)
        if (avvikshendelseIntern.adresse == null) {
            throw UgyldigAvvikException("Adresse må settes ved bestilling av ny distribusjon")
        }
        distribuerJournalpostService.bestillNyDistribusjon(journalpost, avvikshendelseIntern.adresse)
    }

    /**
     * Used when avvikshåndtering is not triggering any action but only used for logging
     */
    fun onlyLogging() {
        // noop
    }

    fun sendTilFagomrade(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        if (journalpost.isTemaEqualTo(avvikshendelseIntern.nyttFagomrade)) {
            return
        }
        if (avvikshendelseIntern.isBidragFagomrade) {
            throw UgyldigAvvikException("Kan ikke sende journalpost mellom FAR og BID tema.")
        }

        opprettJournalpostService.opprettJournalpost(dupliserJournalpost(journalpost) {
            med tema avvikshendelseIntern.nyttFagomrade
            fjern journalførendeenhet true
            med dokumenter journalpost.dokumenter
            fjern sakstilknytning true
        }, originalJournalpostId = journalpost.hentJournalpostIdLong(), skalFerdigstilles = false)
    }

    private fun knyttTilSakPaaNyttFagomrade(avvikshendelseIntern: AvvikshendelseIntern, journalpost: Journalpost) {
        val saksnummer = journalpost.sak!!.fagsakId!!
        hentFeilregistrerteDupliserteJournalposterMedSakOgTema(saksnummer, avvikshendelseIntern.nyttFagomrade, journalpost)
            .findFirst()
            .ifPresentOrElse(
                { jp: Journalpost ->
                    opphevFeilregistrerSakstilknytning(jp.journalpostId)
                    oppdater(OpphevEndreFagomradeJournalfortJournalpostRequest(jp.hentJournalpostIdLong()!!, jp))
                }
            ) { endreJournalpostService.tilknyttTilSak(saksnummer, avvikshendelseIntern.nyttFagomrade, journalpost) }
    }

    private fun hentFeilregistrerteDupliserteJournalposterMedSakOgTema(
        saksnummer: String,
        tema: String,
        journalpost: Journalpost
    ): Stream<Journalpost> {
        return journalpostService.finnJournalposterForSaksnummer(saksnummer, listOf(tema)).stream()
            .filter { obj: Journalpost -> obj.isStatusFeilregistrert() }
            .filter { jp: Journalpost -> harSammeDokumenter(jp, journalpost) }
    }

    private fun harSammeDokumenter(journalpost1: Journalpost, journalpost2: Journalpost): Boolean {
        return journalpost1.dokumenter.stream().allMatch { (_, dokumentInfoId): Dokument ->
            journalpost2.dokumenter.stream()
                .anyMatch { (_, dokumentInfoId1): Dokument -> dokumentInfoId1 == dokumentInfoId }
        }
    }

    fun endreFagomradeJournalfortJournalpost(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        if (journalpost.isTemaEqualTo(avvikshendelseIntern.nyttFagomrade)) {
            return
        }
        if (avvikshendelseIntern.isBidragFagomrade) {
            knyttTilSakPaaNyttFagomrade(avvikshendelseIntern, journalpost)
        } else {
            sendTilFagomrade(journalpost, avvikshendelseIntern)
        }
        oppdater(avvikshendelseIntern.toEndreFagomradeJournalfortJournalpostRequest(journalpost))
        feilregistrerSakstilknytning(avvikshendelseIntern.journalpostId)
    }

    fun endreFagomrade(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        if (journalpost.isInngaaendeJournalfort()) {
            endreFagomradeJournalfortJournalpost(journalpost, avvikshendelseIntern)
        } else {
            endreFagomradeMottattJournalpost(journalpost, avvikshendelseIntern)
            oppgaveService.ferdigstillVurderDokumentOppgaver(journalpost.hentJournalpostIdLong()!!, avvikshendelseIntern.saksbehandlersEnhet!!)
        }
    }

    private fun endreFagomradeMottattJournalpost(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        if (journalpost.hasSak()) {
            oppdater(avvikshendelseIntern.toEndreFagomradeOgKnyttTilSakRequest(journalpost.bruker!!))
        } else {
            oppdater(avvikshendelseIntern.toEndreFagomradeRequest())
        }
    }

    fun registrerRetur(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        val returDato = LocalDate.parse(avvikshendelseIntern.returDato)
        if (journalpost.hasReturDetaljerWithDate(returDato)) {
            throw UgyldigAvvikException("Journalpost har allerede registrert retur med samme dato")
        }
        val beskrivelse = if (Strings.isNotEmpty(avvikshendelseIntern.beskrivelse)) avvikshendelseIntern.beskrivelse else ""
        val tilleggsOpplysninger = journalpost.tilleggsopplysninger
        tilleggsOpplysninger.addReturDetaljLog(ReturDetaljerLogDO(beskrivelse!!, returDato, false))
        oppdater(RegistrerReturRequest(journalpost.hentJournalpostIdLong()!!, returDato, tilleggsOpplysninger))
    }

    fun trekkJournalpost(journalpost: Journalpost, avvikshendelseIntern: AvvikshendelseIntern) {
        // Journalfør på GENERELL_SAK og feilfør sakstilknytning
        validateTrue(journalpost.bruker != null, AvvikDetaljException("Kan ikke trekke journalpost uten bruker"))
        validateTrue(journalpost.tema != null, AvvikDetaljException("Kan ikke trekke journalpost uten tilhørende fagområde"))
        knyttTilGenerellSak(avvikshendelseIntern, journalpost)
        klargjorForFerdigstilling(journalpost)
        dokarkivConsumer.ferdigstill(
            FerdigstillJournalpostRequest(avvikshendelseIntern.journalpostId, avvikshendelseIntern.saksbehandlersEnhet!!)
        )
        leggTilBegrunnelsePaaTittel(avvikshendelseIntern, journalpost)
        feilregistrerSakstilknytning(avvikshendelseIntern.journalpostId)
    }

    private fun klargjorForFerdigstilling(journalpost: Journalpost) {
        settAvsenderNavnLikBrukernavnHvisMangler(journalpost)
    }

    fun settAvsenderNavnLikBrukernavnHvisMangler(journalpost: Journalpost) {
        if (!journalpost.harAvsenderMottaker()) {
            val brukerNavn = personConsumer.hentPerson(journalpost.bruker!!.id)
                .orElseThrow { UgyldigAvvikException("Fant ikke person") }
                .navn
            dokarkivConsumer.endre(LagreAvsenderNavnRequest(journalpost.hentJournalpostIdLong()!!, brukerNavn!!))
            journalpost.avsenderMottaker = AvsenderMottaker(brukerNavn, null, null)
        }
    }

    fun knyttTilGenerellSak(avvikshendelseIntern: AvvikshendelseIntern, journalpost: Journalpost) {
        oppdater(avvikshendelseIntern.toKnyttTilGenerellSakRequest(journalpost.tema!!, journalpost.bruker!!))
    }

    fun leggTilBegrunnelsePaaTittel(avvikshendelseIntern: AvvikshendelseIntern, journalpost: Journalpost?) {
        if (Strings.isNotEmpty(avvikshendelseIntern.beskrivelse)) {
            validateTrue(avvikshendelseIntern.beskrivelse!!.length < 100, AvvikDetaljException("Beskrivelse kan ikke være lengre enn 100 tegn"))
            oppdater(avvikshendelseIntern.toLeggTilBegrunnelsePaaTittelRequest(journalpost!!))
        }
    }

    fun feilregistrerSakstilknytning(journalpostId: Long?) {
        dokarkivConsumer.feilregistrerSakstilknytning(journalpostId)
    }

    fun opphevFeilregistrerSakstilknytning(journalpostId: String?) {
        dokarkivConsumer.opphevFeilregistrerSakstilknytning(java.lang.Long.valueOf(journalpostId))
    }

    fun oppdater(oppdaterJournalpostRequest: OppdaterJournalpostRequest?) {
        dokarkivConsumer.endre(oppdaterJournalpostRequest)
    }

    fun erGyldigAvviksBehandling(journalpost: Journalpost, avvikType: AvvikType?): Boolean {
        return journalpost.tilAvvik().contains(avvikType)
    }

    fun oppdaterDistribusjonsInfoIngenDistribusjon(journalpost: Journalpost) {
        val tilknyttedeJournalpost = journalpostService.hentTilknyttedeJournalposter(journalpost)
        tilknyttedeJournalpost
            .forEach(Consumer { (journalpostId): TilknyttetJournalpost ->
                dokarkivConsumer.oppdaterDistribusjonsInfo(
                    journalpostId, false, JournalpostUtsendingKanal.INGEN_DISTRIBUSJON
                )
            })
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AvvikService::class.java)
    }
}