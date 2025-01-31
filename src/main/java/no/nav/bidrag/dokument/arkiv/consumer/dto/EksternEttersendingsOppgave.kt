
package no.nav.bidrag.dokument.arkiv.consumer.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class HentEtterseningsoppgaveRequest(
    val brukerId: String,
    val skjemanr: String,
)
data class EksternEttersendingsOppgave(
    val brukerId: String,
    val skjemanr: String,
    val sprak: String,
    val tema: String,
    val vedleggsListe: List<InnsendtVedleggDto>,
    val tittel: String? = null,
    val brukernotifikasjonstype: Brukernotifikasjonstype? = null,
    val koblesTilEksisterendeSoknad: Boolean? = null,
    val innsendingsFristDager: Int = 14,
    val mellomlagringDager: Int? = null,
)

enum class Brukernotifikasjonstype {
    @Suppress("ktlint:standard:enum-entry-name-case")
    utkast,

    @Suppress("ktlint:standard:enum-entry-name-case")
    oppgave,
}

data class InnsendtVedleggDto(
    // NAVs identifikasjon av dokumenttype enten som Skjemanummer eller vedleggsnummer.
    val vedleggsnr: String,
    // NAVs offisielle tittel til søknad eller dokument.
    val tittel: String? = null,
    // Noen vedlegg er NAV-skjemaer som brukeren må fylle ut først og laste det opp som vedlegg. Vedleggsurl er en lenke til skjemaet (enten PDF eller til fyllut) som brukeren kan laste ned/fylle ut.
    val url: String? = null,
    // Ledetekst som indikerer hva det er ønsket at søker skal kommentere (på bokmål).
    val opplastingsValgKommentarLedetekst: String? = null,
    // Søkers begrunnelse for opplastingsvalg.
    val opplastingsValgKommentar: String? = null,
)

data class DokumentSoknadDto(
    // Ident til søker som har opprettet søknad i fyllUt tjenesten.
    @get:JsonProperty("brukerId")
    val brukerId: String,
    // NAV skjemanummeret for identifisering av søknaden som er opprettet.
    @get:JsonProperty("skjemanr")
    val skjemanr: String,
    // NAVs offisielle tittel til søknad eller dokument.
    @get:JsonProperty("tittel")
    val tittel: String,
    // Temaet som forsendelsen tilhører.
    @get:JsonProperty("tema")
    val tema: String,
    @get:JsonProperty("status")
    val status: Status,
    // Opprettet dato og tid som UTC
    @get:JsonProperty("opprettetDato")
    val opprettetDato: OffsetDateTime,
    // Liste av dokumenter (hoveddokument og vedlegg) til søknad
    @get:JsonProperty("vedleggsListe")
    val vedleggsListe: List<VedleggDto>,
    // Identifikasjon av mellomlagret søknad
    @get:JsonProperty("id")
    val id: Long? = null,
    // Unik søknadsid.
    @get:JsonProperty("innsendingsId")
    val innsendingsId: String? = null,
    // Unik søknadsid.
    @get:JsonProperty("ettersendingsId")
    val ettersendingsId: String? = null,
    // Søkers foretrukne språk. Det er alltid støtte for nb_NO (norsk bokmål). Det er varierende støtte for andre språk på ulike skjema.
    @get:JsonProperty("spraak")
    val spraak: String? = null,
    // Endret dato og tid som UTC
    @get:JsonProperty("endretDato")
    val endretDato: OffsetDateTime? = null,
    // Opprettet dato og tid som UTC
    @get:JsonProperty("innsendtDato")
    val innsendtDato: OffsetDateTime? = null,
    // Angir i hvilket steg i front-end søker står. Settes default til 0 ved oppretting av søknad.
    @get:JsonProperty("visningsSteg")
    val visningsSteg: Long? = null,
    @get:JsonProperty("visningsType")
    val visningsType: VisningsType? = null,
    // Angir om det skal være mulig å opprette og laste opp filer på vedlegg av type Annet (N6) på søknaden.
    @get:JsonProperty("kanLasteOppAnnet")
    val kanLasteOppAnnet: Boolean? = null,
    // Frist for innsending av søknad / ettersendingssøknad. Dato og tid som UTC
    @get:JsonProperty("innsendingsFristDato")
    val innsendingsFristDato: OffsetDateTime? = null,
    // Tidspunkt for innsending av første søknad, kun relevant for ettersendinger. Dato og tid som UTC
    @get:JsonProperty("forsteInnsendingsDato")
    val forsteInnsendingsDato: OffsetDateTime? = null,
    // Frist i antall dager etter innsending av søknad for ettersending av vedlegg
    @get:JsonProperty("fristForEttersendelse")
    val fristForEttersendelse: Long? = null,
    @get:JsonProperty("arkiveringsStatus")
    val arkiveringsStatus: ArkiveringsStatus? = null,
    // Markerer hvis det er systemet (ikke brukeren selv) som har tatt initiativ til å lage søknaden. Når en bruker tar initiativ til å opprette en søknad/ettersending lages det et *utkast*. Når systemet ser at det mangler påkrevde vedlegg som skal ettersendes, lages det en *oppgave* i stedet.
    @get:JsonProperty("erSystemGenerert")
    val erSystemGenerert: Boolean? = null,
    @get:JsonProperty("soknadstype")
    val soknadstype: Soknadstype? = null,
    // Skjemanr som brukes i url path (små bokstaver uten mellomrom eller spesialtegn)
    @get:JsonProperty("skjemaPath")
    val skjemaPath: String? = null,
    // Applikasjon som har opprettet søknaden
    @get:JsonProperty("applikasjon")
    val applikasjon: String? = null,
    // Dato søknaden vil slettes. En skedulert slettejobb kjøres hver kveld.
    @get:JsonProperty("skalSlettesDato")
    val skalSlettesDato: OffsetDateTime? = null,
    // Antall dager søknaden skal mellomlagres før den slettes
    @get:JsonProperty("mellomlagringDager")
    val mellomlagringDager: Int? = null,
) {
    /**
     *
     *
     * Values: OPPRETTET,UTFYLT,INNSENDT,SLETTET_AV_BRUKER,AUTOMATISK_SLETTET
     */
    enum class Status(
        val value: String,
    ) {
        @JsonProperty(value = "Opprettet")
        OPPRETTET("Opprettet"),

        @JsonProperty(value = "Utfylt")
        UTFYLT("Utfylt"),

        @JsonProperty(value = "Innsendt")
        INNSENDT("Innsendt"),

        @JsonProperty(value = "SlettetAvBruker")
        SLETTET_AV_BRUKER("SlettetAvBruker"),

        @JsonProperty(value = "AutomatiskSlettet")
        AUTOMATISK_SLETTET("AutomatiskSlettet"),
    }

    /**
     *
     *
     * Values: FYLL_UT,DOKUMENTINNSENDING,ETTERSENDING,LOSPOST
     */
    enum class VisningsType(
        val value: String,
    ) {
        @JsonProperty(value = "fyllUt")
        FYLL_UT("fyllUt"),

        @JsonProperty(value = "dokumentinnsending")
        DOKUMENTINNSENDING("dokumentinnsending"),

        @JsonProperty(value = "ettersending")
        ETTERSENDING("ettersending"),

        @JsonProperty(value = "lospost")
        LOSPOST("lospost"),
    }

    /**
     *
     *
     * Values: IKKE_SATT,ARKIVERT,ARKIVERING_FEILET
     */
    enum class ArkiveringsStatus(
        val value: String,
    ) {
        @JsonProperty(value = "IkkeSatt")
        IKKE_SATT("IkkeSatt"),

        @JsonProperty(value = "Arkivert")
        ARKIVERT("Arkivert"),

        @JsonProperty(value = "ArkiveringFeilet")
        ARKIVERING_FEILET("ArkiveringFeilet"),
    }

    /**
     *
     *
     * Values: SOKNAD,ETTERSENDELSE
     */
    enum class Soknadstype(
        val value: String,
    ) {
        @JsonProperty(value = "soknad")
        SOKNAD("soknad"),

        @JsonProperty(value = "ettersendelse")
        ETTERSENDELSE("ettersendelse"),
    }
}

data class VedleggDto(
    // NAVs offisielle tittel til søknad eller dokument.
    @get:JsonProperty("tittel")
    val tittel: String,
    // Visnings navn for et vedlegg.
    @get:JsonProperty("label")
    val label: String,
    // true dersom vedlegget/dokumentet er søknadens hoveddokument
    @get:JsonProperty("erHoveddokument")
    val erHoveddokument: Boolean,
    // true dersom vedlegget/dokumentet er variant at søknadens hoveddokument (på json format)
    @get:JsonProperty("erVariant")
    val erVariant: Boolean,
    // true dersom vedlegget/dokumentet er på PDF/a format
    @get:JsonProperty("erPdfa")
    val erPdfa: Boolean,
    // true dersom vedlegget/dokumentet er påkevd for å kunne bli behandlet av saksbehandler.
    @get:JsonProperty("erPakrevd")
    val erPakrevd: Boolean,
    @get:JsonProperty("opplastingsStatus")
    val opplastingsStatus: OpplastingsStatus,
    // Opprettet dato og tid som UTC
    @get:JsonProperty("opprettetdato")
    val opprettetdato: OffsetDateTime,
    // Id for det mellomlagrede vedlegget
    @get:JsonProperty("id")
    val id: Long? = null,
    // NAVs identifikasjon av dokumenttype enten som Skjemanummer eller vedleggsnummer.
    @get:JsonProperty("vedleggsnr")
    val vedleggsnr: String? = null,
    // Utdypende forklaring til et vedlegg.
    @get:JsonProperty("beskrivelse")
    val beskrivelse: String? = null,
    // Unik identifikasjon
    @get:JsonProperty("uuid")
    val uuid: String? = null,
    @get:JsonProperty("mimetype")
    val mimetype: Mimetype? = null,
    // Dokument innhold som en Byte array
    @get:JsonProperty("document")
    val document: ByteArray? = null,
    // Lenke til skjema som kan lastes ned for å fylles ut
    @get:JsonProperty("skjemaurl")
    val skjemaurl: String? = null,
    // Endret dato og tid som UTC
    @get:JsonProperty("innsendtdato")
    val innsendtdato: OffsetDateTime? = null,
    // Unik vedleggsId som tilsvarer en gitt komponent i formio. Den legges på vedlegget når det opprettes fra FyllUt. Den legges ikke på når det lages N6 vedlegg fra sendInn. Hoveddokument har heller ikke formioId siden det ikke tilsvarer en gitt komponent i formio.
    @get:JsonProperty("formioId")
    val formioId: String? = null,
    // Ledetekst som indikerer hva det er ønsket at søker skal kommentere (på bokmål).
    @get:JsonProperty("opplastingsValgKommentarLedetekst")
    val opplastingsValgKommentarLedetekst: String? = null,
    // Søkers begrunnelse for opplastingsvalg.
    @get:JsonProperty("opplastingsValgKommentar")
    val opplastingsValgKommentar: String? = null,
) {
    /**
     *
     *
     * Values: IKKE_VALGT,LASTET_OPP,INNSENDT,SEND_SENERE,SENDES_AV_ANDRE,SENDES_IKKE,LASTET_OPP_IKKE_RELEVANT_LENGER,LEVERT_DOKUMENTASJON_TIDLIGERE,HAR_IKKE_DOKUMENTASJONEN,NAV_KAN_HENTE_DOKUMENTASJON
     */
    enum class OpplastingsStatus(
        val value: String,
    ) {
        @JsonProperty(value = "IkkeValgt")
        IKKE_VALGT("IkkeValgt"),

        @JsonProperty(value = "LastetOpp")
        LASTET_OPP("LastetOpp"),

        @JsonProperty(value = "Innsendt")
        INNSENDT("Innsendt"),

        @JsonProperty(value = "SendSenere")
        SEND_SENERE("SendSenere"),

        @JsonProperty(value = "SendesAvAndre")
        SENDES_AV_ANDRE("SendesAvAndre"),

        @JsonProperty(value = "SendesIkke")
        SENDES_IKKE("SendesIkke"),

        @JsonProperty(value = "LastetOppIkkeRelevantLenger")
        LASTET_OPP_IKKE_RELEVANT_LENGER("LastetOppIkkeRelevantLenger"),

        @JsonProperty(value = "LevertDokumentasjonTidligere")
        LEVERT_DOKUMENTASJON_TIDLIGERE("LevertDokumentasjonTidligere"),

        @JsonProperty(value = "HarIkkeDokumentasjonen")
        HAR_IKKE_DOKUMENTASJONEN("HarIkkeDokumentasjonen"),

        @JsonProperty(value = "NavKanHenteDokumentasjon")
        NAV_KAN_HENTE_DOKUMENTASJON("NavKanHenteDokumentasjon"),
    }

    /**
     *
     *
     * Values: APPLICATION_SLASH_PDF,APPLICATION_SLASH_JSON,IMAGE_SLASH_PNG,IMAGE_SLASH_JPEG,APPLICATION_SLASH_XML
     */
    enum class Mimetype(
        val value: String,
    ) {
        @JsonProperty(value = "application/pdf")
        APPLICATION_SLASH_PDF("application/pdf"),

        @JsonProperty(value = "application/json")
        APPLICATION_SLASH_JSON("application/json"),

        @JsonProperty(value = "image/png")
        IMAGE_SLASH_PNG("image/png"),

        @JsonProperty(value = "image/jpeg")
        IMAGE_SLASH_JPEG("image/jpeg"),

        @JsonProperty(value = "application/xml")
        APPLICATION_SLASH_XML("application/xml"),
    }
}
