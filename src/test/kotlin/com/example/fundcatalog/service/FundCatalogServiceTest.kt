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
import java.util.UUID

@SpringBootTest
class FundCatalogServiceTest {
    @Autowired lateinit var service: FundCatalogService
    @Autowired lateinit var funds: CatalogFundRepository

    @Test
    fun `default search paginates over the whole universe`() {
        val page0 = service.search(null, null, null, null, 0, 12)
        assertEquals(36, page0.totalElements)
        assertEquals(3, page0.totalPages)
        assertEquals(12, page0.content.size)
        assertTrue(page0.hasNext)
        assertTrue(!page0.hasPrev)
        // default sort is best 1Y first
        assertTrue(page0.content[0].return1y >= page0.content[1].return1y)
        val last = service.search(null, null, null, null, 2, 12)
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
        assertEquals(36, service.search("   ", null, null, null, 0, 50).totalElements) // blank -> null
        val axis = service.search("axis", null, null, null, 0, 50)
        assertEquals(3, axis.totalElements)
        assertTrue(axis.content.all { it.name.contains("Axis", true) || it.amc.contains("Axis", true) })
        assertEquals(12, service.search(null, FundCategory.EQUITY, null, null, 0, 50).totalElements)
        assertEquals(2, service.search(null, null, RiskLevel.LOW, null, 0, 50).totalElements) // the two liquid funds
        assertEquals(2, service.search(null, FundCategory.LIQUID, RiskLevel.LOW, null, 0, 50).totalElements)
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
        assertEquals(36, cats.sumOf { it.count })
        assertEquals(12, cats.first { it.category == "EQUITY" }.count)
    }
}
