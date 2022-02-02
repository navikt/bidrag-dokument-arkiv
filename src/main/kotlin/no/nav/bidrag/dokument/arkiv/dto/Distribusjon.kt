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
        brevkodemap["BI01A01"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01A02"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01A05"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01A06"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01A08"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01A50"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01B01"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01B02"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01B03"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01B05"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01B10"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01B11"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01B20"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01B21"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01B50"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01E01"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01E02"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01E03"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01E50"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01F01"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01F02"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01F50"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01G01"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01G02"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01G04"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01G50"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01I01"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01I05"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01J50"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01K50"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01V03"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01V02"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01V01"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01S41"] = DistribusjonsType.VEDTAK
        brevkodemap["BI01I50"] = DistribusjonsType.VEDTAK

        brevkodemap["BI01A03"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01A04"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01A07"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01B04"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01B22"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01P11"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01P17"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01P18"] = DistribusjonsType.VIKTIG

        // FARSKAP
        brevkodemap["BI01H01_NN"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01H02"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01H03"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01H04"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01_H05"] = DistribusjonsType.VIKTIG

        brevkodemap["BI01S01"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S02"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S03"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S04"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S05"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S06"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S07"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S08"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S09"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S11"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S12"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S13"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S14"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S15"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S16"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S17"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S18"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S19"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S20"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S21"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S25"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S26"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S27"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S28"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S29"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S30"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S31"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S32"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S33"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S34"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S35"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S36"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S37"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S38"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S39"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S42"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S43"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S44"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S45"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S46"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S47"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S48"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S49"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S50"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S51"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S53"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S54"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S55"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S56"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S57"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S58"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S59"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S60"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S62"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S63"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S64"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S65"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S68"] = DistribusjonsType.VIKTIG
        brevkodemap["BI01S70"] = DistribusjonsType.VIKTIG
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
