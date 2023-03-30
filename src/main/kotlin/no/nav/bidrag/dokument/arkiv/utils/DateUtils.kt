package no.nav.bidrag.dokument.arkiv.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateUtils {
    companion object {

        fun formatDate(date: LocalDate?): String? = date?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        fun parseDate(date: String?): LocalDate? {
            if (!isValid(date)) {
                return null
            }
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }

        fun isValid(dateString: String?): Boolean {
            try {
                LocalDate.parse(dateString)
                return true
            } catch (e: Exception) {
                return false
            }
        }
    }
}
