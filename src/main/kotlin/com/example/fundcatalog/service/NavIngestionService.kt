package com.example.fundcatalog.service

import com.example.fundcatalog.domain.NavPoint
import com.example.fundcatalog.repository.CatalogFundRepository
import com.example.fundcatalog.repository.NavPointRepository
import com.example.fundcatalog.web.dto.IngestionResult
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Refreshes catalog NAVs from AMFI: download -> parse -> for each fund that carries an AMFI scheme
 * code, update its current NAV/date and append a history point (deduped per date). Funds without a
 * scheme code keep their seeded values.
 */
@Service
class NavIngestionService(
    private val client: AmfiNavClient,
    private val funds: CatalogFundRepository,
    private val navPoints: NavPointRepository,
) {
    fun ingest(): IngestionResult = process(client.fetch())

    fun process(text: String?): IngestionResult {
        if (text == null) return IngestionResult(fetched = false, schemesParsed = 0, fundsUpdated = 0, navPointsAdded = 0)
        val byCode = AmfiNavParser.parse(text)
        var updated = 0
        var added = 0
        funds.findByAmfiSchemeCodeIsNotNull().forEach { fund ->
            val latest = byCode[fund.amfiSchemeCode] ?: return@forEach
            fund.currentNav = latest.nav
            fund.navDate = latest.date
            funds.save(fund)
            updated++
            if (!navPoints.existsByFundIdAndDate(fund.id, latest.date)) {
                navPoints.save(NavPoint(UUID.randomUUID(), fund.id, latest.date, latest.nav))
                added++
            }
        }
        return IngestionResult(fetched = true, schemesParsed = byCode.size, fundsUpdated = updated, navPointsAdded = added)
    }
}
