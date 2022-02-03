package no.nav.bidrag.dokument.arkiv.dto

import no.nav.bidrag.dokument.dto.DistribuerJournalpostRequest
import no.nav.bidrag.dokument.dto.DistribuerJournalpostResponse
import no.nav.bidrag.dokument.dto.DistribuerTilAdresse
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate

private val ALPHA2_NORGE = "NO"

data class DokDistDistribuerJournalpostRequest(
    var journalpostId: Long,
    var batchId: String? = null,
    var bestillendeFagsystem: String = "BIDRAG",
    var dokumentProdApp: String = "bidragDokArkiv",
    var distribusjonstype: DistribusjonsType = DistribusjonsType.VIKTIG,
    var distribusjonstidspunkt: DistribusjonsTidspunkt = DistribusjonsTidspunkt.KJERNETID,
    var adresse: DokDistDistribuerTilAdresse? = null
) {

    private fun mapAdresse(distribuerTilAdresse: DistribuerTilAdresse?): DokDistDistribuerTilAdresse? {
        val adresse = distribuerTilAdresse ?: return null
        val dokDistAdresseType = if (adresse.land == ALPHA2_NORGE) DokDistAdresseType.norskPostadresse else DokDistAdresseType.utenlandskPostadresse
        return DokDistDistribuerTilAdresse(
            adresselinje1 = adresse.adresselinje1,
            adresselinje2 = adresse.adresselinje2,
            adresselinje3 =  adresse.adresselinje3,
            adressetype = dokDistAdresseType,
            land =  adresse.land,
            postnummer =  adresse.postnummer,
            poststed = adresse.poststed
        )
    }

    constructor(journalpostId: Long, brevkode: String?, distribuerTilAdresse: DistribuerTilAdresse?): this(journalpostId = journalpostId) {
        adresse = mapAdresse(distribuerTilAdresse)
        distribusjonstype = BrevkodeToDistribusjonstypeMapper().toDistribusjonsType(brevkode)
    }
}

enum class DistribusjonsTidspunkt {
    UMIDDELBART,
    KJERNETID
}

enum class DistribusjonsType {
    VEDTAK,
    VIKTIG,
    ANNET
}
enum class DokDistAdresseType{
    norskPostadresse,
    utenlandskPostadresse
}

data class DokDistDistribuerTilAdresse(
    var adresselinje1: String? = null,
    var adresselinje2: String? = null,
    var adresselinje3: String? = null,
    var adressetype: DokDistAdresseType,
    var land: String,
    var postnummer: String? = null,
    var poststed: String? = null,
)

data class DistribuerJournalpostRequestInternal(
    var request: DistribuerJournalpostRequest? = null
) {
    fun hasAdresse(): Boolean = request?.adresse != null
    fun getAdresse(): DistribuerTilAdresse? = request?.adresse
    fun getAdresseDo(): DistribuertTilAdresseDo? {
        val adresse = getAdresse()
        return if (adresse != null) DistribuertTilAdresseDo(
            adresselinje1 = adresse.adresselinje1,
            adresselinje2 = adresse.adresselinje2,
            adresselinje3 = adresse.adresselinje3,
            land = adresse.land,
            poststed = adresse.poststed,
            postnummer = adresse.postnummer
        ) else null
    }
}

data class DokDistDistribuerJournalpostResponse(
    var bestillingsId: String,
) {
    fun toDistribuerJournalpostResponse(journalpostId: Long): DistribuerJournalpostResponse {
        return DistribuerJournalpostResponse("JOARK-${journalpostId}", bestillingsId)
    }
}

fun validerKanDistribueres(journalpost: Journalpost?) {
    Validate.isTrue(journalpost != null, "Fant ingen journalpost")
    Validate.isTrue(journalpost?.journalstatus == JournalStatus.FERDIGSTILT, "Journalpost må ha status FERDIGSTILT")
    Validate.isTrue(journalpost?.tilleggsopplysninger?.isDistribusjonBestilt() == false, "Journalpost er allerede distribuert")
    Validate.isTrue(journalpost?.tema == "BID", "Journalpost må ha tema BID")
    Validate.isTrue(journalpost?.hasMottakerId() == true, "Journalpost må ha satt mottakerId")
    Validate.isTrue(journalpost?.isMottakerIdSamhandlerId() == false, "Journalpost mottakerId kan ikke være samhandlerId")
}

fun validerAdresse(adresse: DistribuerTilAdresse?){
    Validate.isTrue(adresse != null, "Adresse må være satt")
    validateNotNullOrEmpty(adresse?.land, "Land er påkrevd på adresse")
    Validate.isTrue(adresse?.land?.length == 2, "Land må være formatert som Alpha-2 to-bokstavers landkode ")
    if (ALPHA2_NORGE == adresse?.land){
        validateNotNullOrEmpty(adresse.postnummer, "Postnummer er påkrevd på norsk adresse")
        validateNotNullOrEmpty(adresse.poststed, "Poststed er påkrevd på norsk adresse")
    } else {
        validateNotNullOrEmpty(adresse?.adresselinje1, "Adresselinje1 er påkrevd på utenlandsk adresse")
    }
}

fun validateNotNullOrEmpty(value: String?, message: String){
    Validate.isTrue(StringUtils.isNotEmpty(value), message)
}



class BrevkodeToDistribusjonstypeMapper {
    private var brevkodemap: MutableMap<String, DistribusjonsType> = hashMapOf()
    private fun initBrevkodemap() {
        brevkodemap["BI01A01"] = DistribusjonsType.VEDTAK // Vedtak forskudd
        brevkodemap["BI01A02"] = DistribusjonsType.VEDTAK // Vedtak tilbakekreving
        brevkodemap["BI01A03"] = DistribusjonsType.VIKTIG // Revurdering forskudd kan settes opp
        brevkodemap["BI01A04"] = DistribusjonsType.VIKTIG // Revurdering forskudd settes ned
        brevkodemap["BI01A05"] = DistribusjonsType.VEDTAK // Vedtak ikke             tilbakekreving
        brevkodemap["BI01A06"] = DistribusjonsType.VIKTIG // Forhøyet forskudd 11 år
        brevkodemap["BI01A07"] = DistribusjonsType.VIKTIG // Revurdering forskudd kan justeres automatisk
        brevkodemap["BI01A08"] = DistribusjonsType.VEDTAK // Vedtak omregning av forskudd
        brevkodemap["BI01A50"] = DistribusjonsType.VEDTAK // Klage - vedtak forskudd
        brevkodemap["BI01B01"] = DistribusjonsType.VEDTAK // Vedtak barnebidrag
        brevkodemap["BI01B02"] = DistribusjonsType.VEDTAK // Vedtak tilleggsbidrag
        brevkodemap["BI01B03"] = DistribusjonsType.VEDTAK // Vedtak barnebidrag over 18 år
        brevkodemap["BI01B04"] = DistribusjonsType.VIKTIG // Revurdering Bidrags pga forskudd
        brevkodemap["BI01B05"] = DistribusjonsType.VEDTAK // VEDTAK AUTOMATISK JUSTERING BARNEBIDRAG
        brevkodemap["BI01B10"] = DistribusjonsType.VEDTAK // Opphørsvedtak
        brevkodemap["BI01B11"] = DistribusjonsType.VEDTAK // Vedtak tilleggsbidrag over 18 år
        brevkodemap["BI01B20"] = DistribusjonsType.VEDTAK // Vedtak utland skjønn fastsettelse
        brevkodemap["BI01B21"] = DistribusjonsType.VEDTAK // Vedtak utland skjønn endring
        brevkodemap["BI01B22"] = DistribusjonsType.VIKTIG // Bidrag indeksreg, BP bor i utlandet
        brevkodemap["BI01B50"] = DistribusjonsType.VEDTAK // Klage - vedtak bidrag
        brevkodemap["BI01E01"] = DistribusjonsType.VEDTAK // Vedtak særtilskudd innvilget
        brevkodemap["BI01E02"] = DistribusjonsType.VEDTAK // Vedtak særtilskudd avslag
        brevkodemap["BI01E03"] = DistribusjonsType.VEDTAK // Vedtak særtilskudd tannregulering
        brevkodemap["BI01E50"] = DistribusjonsType.VEDTAK // Klage - vedtak særtilskudd
        brevkodemap["BI01F01"] = DistribusjonsType.VEDTAK // Vedtak ettergivelse innvilget
        brevkodemap["BI01F02"] = DistribusjonsType.VEDTAK // Vedtak ettergivelse avslag
        brevkodemap["BI01F50"] = DistribusjonsType.VEDTAK // Klage - vedtak ettergivelse gjeld
        brevkodemap["BI01G01"] = DistribusjonsType.VEDTAK // Vedtak innkrev. barnebidrag og gjeld
        brevkodemap["BI01G02"] = DistribusjonsType.VEDTAK // Vedtak innkreving opphør
        brevkodemap["BI01G04"] = DistribusjonsType.VEDTAK // Vedtak innkreving særtilskudd
        brevkodemap["BI01G50"] = DistribusjonsType.VEDTAK // Klage - vedtak innkreving
        brevkodemap["BI01H01"] = DistribusjonsType.VIKTIG // Farskap innkalling mor
        brevkodemap["BI01H02"] = DistribusjonsType.VIKTIG // Innkalling farskapssak  oppgitt far
        brevkodemap["BI01H03"] = DistribusjonsType.VIKTIG // Melding om blodprøver i farskapsak
        brevkodemap["BI01H04"] = DistribusjonsType.VIKTIG // Pålegg om å framstille barn for å gi blodprøve
        brevkodemap["BI01H05"] = DistribusjonsType.VIKTIG // Pålegg om blodprøve i farskapssak
        brevkodemap["BI01I01"] = DistribusjonsType.VEDTAK // Vedtak ektefellebidrag
        brevkodemap["BI01I50"] = DistribusjonsType.VEDTAK // Klage - vedtak ektefellebidrag
        brevkodemap["BI01J50"] = DistribusjonsType.VEDTAK // Klage - vedtak gebyr
        brevkodemap["BI01K50"] = DistribusjonsType.VEDTAK // Klage - vedtak tilbakekreving
        brevkodemap["BI01P11"] = DistribusjonsType.VIKTIG // Notat P11                                   t
        brevkodemap["BI01P17"] = DistribusjonsType.VIKTIG // Innstilling til klageinstans
        brevkodemap["BI01P18"] = DistribusjonsType.VIKTIG // Saksbehandlingsnotat
        brevkodemap["BI01S01"] = DistribusjonsType.VIKTIG // Fastsettelse varsel til motparten
        brevkodemap["BI01S02"] = DistribusjonsType.VIKTIG // Fritekstbrev
        brevkodemap["BI01S03"] = DistribusjonsType.VIKTIG // 18 år varsel til motparten
        brevkodemap["BI01S04"] = DistribusjonsType.VIKTIG // Særtilskudd varsel til motparten
        brevkodemap["BI01S05"] = DistribusjonsType.VIKTIG // Ettergivelse varsel til motparten
        brevkodemap["BI01S06"] = DistribusjonsType.VIKTIG // Varsel eget tiltak nyfødt barn
        brevkodemap["BI01S07"] = DistribusjonsType.VIKTIG // Fastsettelse eget tiltak varsel til BP
        brevkodemap["BI01S08"] = DistribusjonsType.VIKTIG // Varsel revurd forskudd
        brevkodemap["BI01S09"] = DistribusjonsType.VIKTIG // Varsel opphør bidrag v 18år
        brevkodemap["BI01S10"] = DistribusjonsType.VIKTIG // Kopiforside                                 t
        brevkodemap["BI01S11"] = DistribusjonsType.VIKTIG // Varsel om automatisk justering av barnebidrag
        brevkodemap["BI01S12"] = DistribusjonsType.VIKTIG // Fastsettelse orientering til BM
        brevkodemap["BI01S13"] = DistribusjonsType.VIKTIG // Fastsettelse orientering til BP
        brevkodemap["BI01S14"] = DistribusjonsType.VIKTIG // Endring orientering til søkeren
        brevkodemap["BI01S15"] = DistribusjonsType.VIKTIG // 18år orientering til BP
        brevkodemap["BI01S16"] = DistribusjonsType.VIKTIG // 18år orientering til BM
        brevkodemap["BI01S17"] = DistribusjonsType.VIKTIG // Ettergivelse varsel til søkeren
        brevkodemap["BI01S18"] = DistribusjonsType.VIKTIG // Særtilskudd orientering til søkeren
        brevkodemap["BI01S19"] = DistribusjonsType.VIKTIG // Innkreving varsel til motparten
        brevkodemap["BI01S20"] = DistribusjonsType.VIKTIG // Klage varsel til motparten
        brevkodemap["BI01S21"] = DistribusjonsType.VIKTIG // Klage orientering til klageren
        brevkodemap["BI01S22"] = DistribusjonsType.VIKTIG // Revurd bidrag pga FO til BM
        brevkodemap["BI01S23"] = DistribusjonsType.VIKTIG // Revurd bidrag pga FO til BP
        brevkodemap["BI01S24"] = DistribusjonsType.VIKTIG // STANDARDBREV OM INNHENTING AV OPPLYSNINGER 18år
        brevkodemap["BI01S25"] = DistribusjonsType.VIKTIG // Info til BM når farskap OK
        brevkodemap["BI01S26"] = DistribusjonsType.VIKTIG // Endring varsel til motparten
        brevkodemap["BI01S27"] = DistribusjonsType.VIKTIG // Varsel opph tilbake i tid §2 ikke opphold i Riket
        brevkodemap["BI01S28"] = DistribusjonsType.VIKTIG // Varsel opph tilbake i tid §2 partene bor sammen
        brevkodemap["BI01S29"] = DistribusjonsType.VIKTIG // Varsel opph tilbake i tid §3 direkte betalinger
        brevkodemap["BI01S30"] = DistribusjonsType.VIKTIG // Varsel opph tilbake i tid §6 ikke omsorg
        brevkodemap["BI01S31"] = DistribusjonsType.VIKTIG // Fasts. eget tiltak uten innkr varsel til partene
        brevkodemap["BI01S32"] = DistribusjonsType.VIKTIG // Fastsettelse eget tiltak varsel til BM
        brevkodemap["BI01S33"] = DistribusjonsType.VIKTIG // Endring varsel eget tiltak pga 25%
        brevkodemap["BI01S34"] = DistribusjonsType.VIKTIG // Endring varsel eget tiltak pga barnetillegg
        brevkodemap["BI01S35"] = DistribusjonsType.VIKTIG // Endring varsel eget tiltak pga forholds fordel
        brevkodemap["BI01S36"] = DistribusjonsType.VIKTIG // Endring varsel eget tiltak pga forsørgingstillegg
        brevkodemap["BI01S37"] = DistribusjonsType.VIKTIG // Bortfall ektefellebidrag BP død orientering til BM
        brevkodemap["BI01S38"] = DistribusjonsType.VIKTIG // Bortfall ektefellebidrag orientering til partene
        brevkodemap["BI01S39"] = DistribusjonsType.VIKTIG // Bortfall ektefellebidrag nytt ekteskap orientering
        brevkodemap["BI01S41"] = DistribusjonsType.VEDTAK // Vedtak - bortfall nytt ekteskap
        brevkodemap["BI01S42"] = DistribusjonsType.VIKTIG // Endring ektefellebidrag orientering til søkeren
        brevkodemap["BI01S43"] = DistribusjonsType.VIKTIG // Fastsettelse ektefellebidrag orientering
        brevkodemap["BI01S44"] = DistribusjonsType.VIKTIG // Fastsettelse ektefellebidrag varsel til BP
        brevkodemap["BI01S45"] = DistribusjonsType.VIKTIG // Varsel revurd forskudd tilbake i tid
        brevkodemap["BI01S46"] = DistribusjonsType.VIKTIG // Varsel oppfostringsbidrag forholdsmessig fordeling
        brevkodemap["BI01S47"] = DistribusjonsType.VIKTIG // Endring oppfostringsbidrag orientering til BP
        brevkodemap["BI01S48"] = DistribusjonsType.VIKTIG // Endring oppfostringsbidrag orientering kommune
        brevkodemap["BI01S49"] = DistribusjonsType.VIKTIG // Endring oppfostringsbidrag varsel til motparten
        brevkodemap["BI01S50"] = DistribusjonsType.VIKTIG // Ettergivelse oppfostringsbidrag orientering til BP
        brevkodemap["BI01S51"] = DistribusjonsType.VIKTIG // Ettergivelse oppfostringsbidrag varsel kommune
        brevkodemap["BI01S52"] = DistribusjonsType.VIKTIG // Fastsettelse oppfostringsbidrag orienter kommune
        brevkodemap["BI01S53"] = DistribusjonsType.VIKTIG // Fastsettelse oppfostringsbidrag varsel til BP
        brevkodemap["BI01S54"] = DistribusjonsType.VIKTIG // Varsel tilb.kr. paragraf 5 inntekt
        brevkodemap["BI01S55"] = DistribusjonsType.VIKTIG // Varsel tilb.kr. paragraf 2 - partene bor sammen
        brevkodemap["BI01S56"] = DistribusjonsType.VIKTIG // Varsel tilb.kr. paragraf 2 ikke opphold i Riket
        brevkodemap["BI01S57"] = DistribusjonsType.VIKTIG // Varsel tilb.kr. NAV paragraf 3 direkte betalinger
        brevkodemap["BI01S58"] = DistribusjonsType.VIKTIG // Varsel tilb.kr. NAV paragraf 6 ikke omsorg
        brevkodemap["BI01S59"] = DistribusjonsType.VIKTIG // Orientering ettergivelse av tilbakekrevingsbeløp
        brevkodemap["BI01S60"] = DistribusjonsType.VIKTIG // Infobrev til partene oversendelse klageinstans
        brevkodemap["BI01S61"] = DistribusjonsType.VIKTIG // Innhenting opplysninger paragraf 10 i barnelova
        brevkodemap["BI01S62"] = DistribusjonsType.VIKTIG // Fastsettelse bidrag barnetillegg varsel partene
        brevkodemap["BI01S63"] = DistribusjonsType.VIKTIG // Fastsettelse bidrag forsørgingstillegg varsel
        brevkodemap["BI01S64"] = DistribusjonsType.VIKTIG // Varsel klage fvt 35
        brevkodemap["BI01S65"] = DistribusjonsType.VIKTIG // Varsel om motregning
        brevkodemap["BI01S67"] = DistribusjonsType.VIKTIG // Adresseforespørsel
        brevkodemap["BI01S68"] = DistribusjonsType.VIKTIG // Varsel om overføring KO-fogd
        brevkodemap["BI01S70"] = DistribusjonsType.VIKTIG // Varsel om eget tiltak trekkes
        brevkodemap["BI01X01"] = DistribusjonsType.VIKTIG // Referat fra samtale
        brevkodemap["BI01X02"] = DistribusjonsType.VIKTIG // Elektronisk dialog
        brevkodemap["BI01X10"] = DistribusjonsType.VIKTIG // Inntektsinformasjon fra A-inntekt

        brevkodemap["BI01I05"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01V03"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01V02"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01V01"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01V04"] = DistribusjonsType.VIKTIG
    }

    fun toDistribusjonsType(brevkode: String?): DistribusjonsType {
        return brevkodemap.getOrDefault(brevkode, DistribusjonsType.VIKTIG)
    }

    fun getBrevkodeMap(): Map<String, DistribusjonsType> {
        return brevkodemap
    }

    init {
        initBrevkodemap()
    }
}
