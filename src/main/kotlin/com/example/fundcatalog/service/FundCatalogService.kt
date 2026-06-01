package com.example.fundcatalog.service

import com.example.fundcatalog.domain.CatalogFund
import com.example.fundcatalog.domain.FundCategory
import com.example.fundcatalog.domain.NavPoint
import com.example.fundcatalog.domain.RiskLevel
import com.example.fundcatalog.repository.CatalogFundRepository
import com.example.fundcatalog.repository.NavPointRepository
import com.example.fundcatalog.web.dto.CategoryCount
import com.example.fundcatalog.web.dto.FundCard
import com.example.fundcatalog.web.dto.FundDetail
import com.example.fundcatalog.web.dto.NavPointDto
import com.example.fundcatalog.web.dto.PageResponse
import com.example.fundcatalog.web.dto.SubCategoryCount
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.util.UUID

@Service
class FundCatalogService(
    private val funds: CatalogFundRepository,
    private val navPoints: NavPointRepository,
) {

    /** Paged, sorted, filtered explore search over the fund universe. */
    fun search(q: String?, category: FundCategory?, risk: RiskLevel?, sort: String?, page: Int, size: Int, subCategory: String? = null): PageResponse<FundCard> {
        val pageable = PageRequest.of(page, size, sortFor(sort))
        val result = funds.findAll(specFor(q, category, risk, subCategory), pageable)
        return PageResponse(
            content = result.content.map(::toCard),
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
            hasPrev = result.hasPrevious(),
        )
    }

    fun detail(id: UUID): FundDetail = toDetail(load(id))

    /** Fund "types" (sub-categories) with counts, for browse-by-type chips. Reference data is tiny
     *  and cached, so grouping in memory keeps this portable across databases. */
    fun subCategories(): List<SubCategoryCount> =
        funds.findAll()
            .groupingBy { it.category.name to it.subCategory }
            .eachCount()
            .map { (key, count) -> SubCategoryCount(key.first, key.second, count.toLong()) }
            .sortedWith(compareBy({ it.category }, { -it.count }, { it.subCategory }))

    /** Fetch several funds (preserving the requested order, skipping unknown ids) for side-by-side
     *  comparison of funds of the same type. */
    fun compare(ids: List<UUID>): List<FundDetail> {
        val byId = funds.findAllById(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it] }.map(::toDetail)
    }

    /** NAV series for the detail chart, trimmed to the requested range (MAX = full history). */
    fun navHistory(id: UUID, range: String?): List<NavPointDto> {
        load(id) // 404 if the fund does not exist
        val days = windowDays(range)
        val points = if (days == null) {
            navPoints.findByFundIdOrderByDateAsc(id)
        } else {
            navPoints.findByFundIdAndDateGreaterThanEqualOrderByDateAsc(id, LocalDate.now().minusDays(days))
        }
        return points.map { NavPointDto(it.date, it.nav) }
    }

    /** Non-empty categories with their fund counts (explore filter chips). */
    fun categories(): List<CategoryCount> =
        FundCategory.entries
            .map { CategoryCount(it.name, funds.countByCategory(it)) }
            .filter { it.count > 0 }

    private fun load(id: UUID): CatalogFund =
        funds.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "fund not found") }

    /** Combine only the filters that are actually set, so absent ones add no SQL (null spec = all). */
    private fun specFor(q: String?, category: FundCategory?, risk: RiskLevel?, subCategory: String?): Specification<CatalogFund>? {
        val specs = mutableListOf<Specification<CatalogFund>>()
        if (category != null) specs.add(Specification { root, _, cb -> cb.equal(root.get<FundCategory>("category"), category) })
        if (subCategory != null) specs.add(Specification { root, _, cb -> cb.equal(root.get<String>("subCategory"), subCategory) })
        if (risk != null) specs.add(Specification { root, _, cb -> cb.equal(root.get<RiskLevel>("riskLevel"), risk) })
        val term = q?.trim()?.ifBlank { null }
        if (term != null) {
            val like = "%${term.lowercase()}%"
            specs.add(Specification { root, _, cb -> cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("amc")), like)) })
        }
        return specs.reduceOrNull { a, b -> a.and(b) }
    }

    private fun sortFor(sort: String?): Sort = when (sort) {
        "return3y" -> Sort.by(Sort.Direction.DESC, "return3y")
        "return5y" -> Sort.by(Sort.Direction.DESC, "return5y")
        "rating" -> Sort.by(Sort.Direction.DESC, "rating")
        "expenseRatio" -> Sort.by(Sort.Direction.ASC, "expenseRatio") // lower is better
        "name" -> Sort.by(Sort.Direction.ASC, "name")
        else -> Sort.by(Sort.Direction.DESC, "return1y") // default: best 1Y first
    }

    private fun windowDays(range: String?): Long? = when (range) {
        "1M" -> 30
        "6M" -> 180
        "1Y" -> 365
        "3Y" -> 1095
        else -> null // MAX / unspecified -> full history
    }

    private fun toCard(f: CatalogFund) = FundCard(
        id = f.id.toString(), name = f.name, amc = f.amc, category = f.category.name,
        subCategory = f.subCategory, riskLevel = f.riskLevel.name, rating = f.rating,
        expenseRatio = f.expenseRatio, return1y = f.return1y, return3y = f.return3y,
        return5y = f.return5y, currentNav = f.currentNav,
    )

    private fun toDetail(f: CatalogFund) = FundDetail(
        id = f.id.toString(), name = f.name, amc = f.amc, category = f.category.name,
        subCategory = f.subCategory, riskLevel = f.riskLevel.name, rating = f.rating,
        expenseRatio = f.expenseRatio, aumCrore = f.aumCrore, return1y = f.return1y,
        return3y = f.return3y, return5y = f.return5y, currentNav = f.currentNav,
        navDate = f.navDate, benchmark = f.benchmark, fundManager = f.fundManager,
        minSip = f.minSip, minLumpsum = f.minLumpsum,
    )
}
