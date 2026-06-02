package com.example.fundcatalog.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AmfiNavParserTest {

    @Test
    fun `parses valid rows and skips headers, blanks, N_A NAV and bad dates`() {
        val text = listOf(
            "Scheme Code;ISIN Div Payout/ ISIN Growth;ISIN Div Reinvestment;Scheme Name;Net Asset Value;Date",
            "",
            "Open Ended Schemes(Equity Scheme - Small Cap Fund)",
            "ABC Mutual Fund",
            "125497;INF200K01T51;-;SBI Small Cap Fund - Direct Plan - Growth;192.3195;01-Jun-2026",
            "118825;INF769K01AX2;-;Mirae Asset Large Cap Fund - Direct Plan - Growth;121.313;01-Jun-2026",
            "999999;INFX;-;Fund With No NAV;N.A.;01-Jun-2026",
            "888888;INFY;-;Fund With Bad Date;55.5;not-a-date",
        ).joinToString("\n")

        val map = AmfiNavParser.parse(text)

        assertEquals(2, map.size)
        assertEquals(0, map.getValue("125497").nav.compareTo("192.3195".toBigDecimal()))
        assertEquals(LocalDate.of(2026, 6, 1), map.getValue("125497").date)
        assertEquals(0, map.getValue("118825").nav.compareTo("121.313".toBigDecimal()))
        assertNull(map["999999"]) // N.A. NAV skipped
        assertNull(map["888888"]) // unparseable date skipped
        assertNull(map["Scheme Code"]) // header skipped
    }
}
