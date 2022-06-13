package no.nav.bidrag.dokument.arkiv.dto

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate

data class Violation(val property: String, val decode: String)

fun validateNotNullOrEmpty(value: String?, message: String) {
    Validate.isTrue(StringUtils.isNotEmpty(value), message)
}
fun validateTrue(expression: Boolean?, throwable: RuntimeException?) {
    if (!expression!!) {
        throw throwable!!
    }
}