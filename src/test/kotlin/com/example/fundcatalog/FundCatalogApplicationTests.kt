package com.example.fundcatalog

import com.example.fundcatalog.repository.CatalogFundRepository
import com.example.fundcatalog.repository.NavPointRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class FundCatalogApplicationTests {
    @Autowired lateinit var funds: CatalogFundRepository
    @Autowired lateinit var navPoints: NavPointRepository
    @Autowired lateinit var seeder: FundCatalogSeeder

    @Test
    fun `context loads and seeds the fund universe with NAV history`() {
        assertEquals(36L, funds.count())
        assertEquals(36L * 37, navPoints.count()) // 37 monthly points per fund
    }

    @Test
    fun `seeder is idempotent`() {
        seeder.run(null)
        assertEquals(36L, funds.count())
        assertEquals(36L * 37, navPoints.count())
    }
}
