package com.example.fundcatalog.service

import com.example.fundcatalog.domain.FundCategory
import com.example.fundcatalog.domain.RiskLevel
import com.example.fundcatalog.repository.CatalogFundRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
class FundCatalogServiceTest {
    @Autowired lateinit var service: FundCatalogService
    @Autowired lateinit var funds: CatalogFundRepository

    @Test
    fun `default search paginates over the whole universe`() {
        val page0 = service.search(null, null, null, null, 0, 12)
        assertEquals(50, page0.totalElements)
        assertEquals(5, page0.totalPages)
        assertEquals(12, page0.content.size)
        assertTrue(page0.hasNext)
        assertTrue(!page0.hasPrev)
        // default sort is best 1Y first
        assertTrue(page0.content[0].return1y >= page0.content[1].return1y)
        val last = service.search(null, null, null, null, 4, 12)
        assertTrue(last.hasPrev)
        assertTrue(!last.hasNext)
    }

    @Test
    fun `every sort option is honored`() {
        assertTrue(service.search(null, null, null, "return3y", 0, 5).content.let { it[0].return3y >= it[1].return3y })
        assertTrue(service.search(null, null, null, "return5y", 0, 5).content.let { it[0].return5y >= it[1].return5y })
        assertTrue(service.search(null, null, null, "rating", 0, 5).content.let { it[0].rating >= it[1].rating })
        assertTrue(service.search(null, null, null, "expenseRatio", 0, 5).content.let { it[0].expenseRatio <= it[1].expenseRatio })
        assertTrue(service.search(null, null, null, "name", 0, 5).content.let { it[0].name <= it[1].name })
    }

    @Test
    fun `filters by free-text, category and risk - blank query is ignored`() {
        assertEquals(50, service.search("   ", null, null, null, 0, 50).totalElements) // blank -> null
        val axis = service.search("axis", null, null, null, 0, 50)
        assertEquals(4, axis.totalElements)
        assertTrue(axis.content.all { it.name.contains("Axis", true) || it.amc.contains("Axis", true) })
        assertEquals(18, service.search(null, FundCategory.EQUITY, null, null, 0, 50).totalElements)
        assertEquals(3, service.search(null, null, RiskLevel.LOW, null, 0, 50).totalElements) // the three liquid funds
        assertEquals(3, service.search(null, FundCategory.LIQUID, RiskLevel.LOW, null, 0, 50).totalElements)
    }

    @Test
    fun `detail returns the fund, unknown id is 404`() {
        val any = funds.findAll().first()
        assertEquals(any.name, service.detail(any.id).name)
        assertThrows(ResponseStatusException::class.java) { service.detail(UUID.randomUUID()) }
    }

    @Test
    fun `nav history honors every range and 404s an unknown fund`() {
        val id = funds.findAll().first().id
        val max = service.navHistory(id, null).size
        assertEquals(37, max)
        assertEquals(37, service.navHistory(id, "ZZZ").size) // unknown range -> full history
        val m1 = service.navHistory(id, "1M").size
        val m6 = service.navHistory(id, "6M").size
        val y1 = service.navHistory(id, "1Y").size
        val y3 = service.navHistory(id, "3Y").size
        assertTrue(m1 in 1..max && m1 <= m6 && m6 <= y1 && y1 <= y3 && y3 <= max)
        assertThrows(ResponseStatusException::class.java) { service.navHistory(UUID.randomUUID(), "1Y") }
    }

    @Test
    fun `categories lists every non-empty category and counts add up`() {
        val cats = service.categories()
        assertEquals(8, cats.size)
        assertEquals(50, cats.sumOf { it.count })
        assertEquals(18, cats.first { it.category == "EQUITY" }.count)
    }

    @Test
    fun `sub-categories list distinct fund types with counts`() {
        val subs = service.subCategories()
        assertEquals(50, subs.sumOf { it.count })
        val smallCap = subs.first { it.subCategory == "Small Cap" }
        assertEquals("EQUITY", smallCap.category)
        assertEquals(3, smallCap.count) // SBI + Nippon + Quant
        assertEquals(4, subs.first { it.subCategory == "Large Cap" }.count)
    }

    @Test
    fun `search filters by sub-category`() {
        val smallCaps = service.search(null, FundCategory.EQUITY, null, null, 0, 50, "Small Cap")
        assertEquals(3, smallCaps.totalElements)
        assertTrue(smallCaps.content.all { it.subCategory == "Small Cap" })
    }

    @Test
    fun `market pulse ranks recent gainers, losers and category moves`() {
        val pulse = service.marketPulse(5)
        assertEquals(50, pulse.fundCount)
        assertEquals(5, pulse.topGainers.size)
        assertEquals(5, pulse.topLosers.size)
        assertEquals(8, pulse.categoryMovers.size)
        assertTrue(pulse.asOf != null && !pulse.asOf!!.isAfter(LocalDate.now()))
        // gainers high-to-low, losers low-to-high, the best gainer beats the worst loser
        assertTrue(pulse.topGainers.zipWithNext().all { (a, b) -> a.changePct >= b.changePct })
        assertTrue(pulse.topLosers.zipWithNext().all { (a, b) -> a.changePct <= b.changePct })
        assertTrue(pulse.topGainers.first().changePct >= pulse.topLosers.first().changePct)
        assertTrue(pulse.categoryMovers.zipWithNext().all { (a, b) -> a.avgChangePct >= b.avgChangePct })
        assertTrue(pulse.topGainers.first().name.isNotBlank() && pulse.topGainers.first().rating in 1..5)
    }

    @Test
    fun `compare returns the requested funds in order and skips unknown ids`() {
        val smallCapIds = funds.findAll().filter { it.subCategory == "Small Cap" }.map { it.id }
        val result = service.compare(smallCapIds)
        assertEquals(smallCapIds.map { it.toString() }, result.map { it.id })
        assertEquals(1, service.compare(listOf(smallCapIds.first(), UUID.randomUUID())).size) // unknown skipped
        assertTrue(service.compare(emptyList()).isEmpty())
    }
}
