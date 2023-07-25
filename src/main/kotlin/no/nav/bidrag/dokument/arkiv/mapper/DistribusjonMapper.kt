package no.nav.bidrag.dokument.arkiv.mapper

import no.nav.bidrag.dokument.arkiv.dto.UtsendingsInfo
import no.nav.bidrag.dokument.dto.UtsendingsInfoVarselTypeDto

fun UtsendingsInfo.tilVarselTypeDto() = when (sisteVarselSendt?.type) {
    "EPOST" -> UtsendingsInfoVarselTypeDto.EPOST
    "SMS" -> UtsendingsInfoVarselTypeDto.SMS
    else -> if (smsVarselSendt != null) {
        UtsendingsInfoVarselTypeDto.SMS
    } else if (digitalpostSendt != null) {
        UtsendingsInfoVarselTypeDto.DIGIPOST
    } else if (epostVarselSendt != null) {
        UtsendingsInfoVarselTypeDto.EPOST
    } else if (fysiskpostSendt != null) {
        UtsendingsInfoVarselTypeDto.FYSISK_POST
    } else {
        null
    }
}