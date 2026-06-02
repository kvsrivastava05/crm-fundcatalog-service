package com.example.fundcatalog.service

import com.example.fundcatalog.web.dto.AmfiNav
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses AMFI's NAVAll.txt. Lines are `SchemeCode;ISIN;ISIN;SchemeName;NAV;Date` (date `dd-MMM-yyyy`);
 * AMC/category headers and blank lines have fewer fields or a non-numeric code and are skipped, as
 * are rows with an unparseable NAV (e.g. "N.A.") or date. Returns a map keyed by scheme code.
 */
object AmfiNavParser {
    private val DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)

    fun parse(text: String): Map<String, AmfiNav> {
        val out = LinkedHashMap<String, AmfiNav>()
        for (line in text.lineSequence()) {
            val parts = line.split(';')
            if (parts.size < 6) continue
            val code = parts[0].trim()
            if (code.isEmpty() || !code.all(Char::isDigit)) continue
            val nav = parts[4].trim().toBigDecimalOrNull() ?: continue
            val date = runCatching { LocalDate.parse(parts[5].trim(), DATE) }.getOrNull() ?: continue
            out[code] = AmfiNav(code, nav, date)
        }
        return out
    }
}
