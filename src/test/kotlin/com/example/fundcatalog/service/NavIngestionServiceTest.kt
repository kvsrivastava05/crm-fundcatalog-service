package com.example.fundcatalog.service

import com.example.fundcatalog.repository.CatalogFundRepository
import com.example.fundcatalog.repository.NavPointRepository
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@SpringBootTest
@Transactional // roll back each test so the ingested NAV/points don't pollute the shared H2
class NavIngestionServiceTest {
    @Autowired lateinit var funds: CatalogFundRepository
    @Autowired lateinit var navPoints: NavPointRepository

    private fun navFile(code: String, nav: String, date: String) = listOf(
        "Scheme Code;ISIN Div Payout/ ISIN Growth;ISIN Div Reinvestment;Scheme Name;Net Asset Value;Date",
        "ABC Mutual Fund",
        "$code;INF1;-;Some Fund - Direct Plan - Growth;$nav;$date",
        "777777;INF2;-;Unrelated Scheme Not In Our Catalog - Direct Plan - Growth;10.0;$date",
    ).joinToString("\n")

    private fun serverServing(body: String): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/nav") { exchange ->
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        return server
    }

    @Test
    fun `ingest refreshes the matching fund's NAV and dedupes the history point by date`() {
        val fund = funds.findByAmfiSchemeCodeIsNotNull().first()
        val date = LocalDate.now().plusDays(5) // a date the seeder didn't create a point for
        val amfiDate = date.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH))
        val server = serverServing(navFile(fund.amfiSchemeCode!!, "999.5", amfiDate))
        try {
            val svc = NavIngestionService(AmfiNavClient("http://127.0.0.1:${server.address.port}/nav"), funds, navPoints)
            val first = svc.ingest()
            assertTrue(first.fetched)
            assertTrue(first.schemesParsed >= 2)
            assertEquals(1, first.fundsUpdated)   // only our fund's code is in the file
            assertEquals(1, first.navPointsAdded)

            val refreshed = funds.findById(fund.id).get()
            assertEquals(0, refreshed.currentNav.compareTo("999.5".toBigDecimal()))
            assertEquals(date, refreshed.navDate)
            assertTrue(navPoints.existsByFundIdAndDate(fund.id, date))

            val second = svc.ingest() // same date -> no duplicate history point
            assertEquals(1, second.fundsUpdated)
            assertEquals(0, second.navPointsAdded)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `a failed download yields a not-fetched result with no updates`() {
        val svc = NavIngestionService(AmfiNavClient("http://127.0.0.1:1/none"), funds, navPoints)
        val result = svc.ingest()
        assertFalse(result.fetched)
        assertEquals(0, result.fundsUpdated)
        assertEquals(0, result.navPointsAdded)
    }

    @Test
    fun `the nightly scheduler triggers an ingest`() {
        val svc = NavIngestionService(AmfiNavClient("http://127.0.0.1:1/none"), funds, navPoints)
        NavIngestionScheduler(svc).refreshNightly() // delegates to ingest(); dead URL -> safe no-op
    }
}
