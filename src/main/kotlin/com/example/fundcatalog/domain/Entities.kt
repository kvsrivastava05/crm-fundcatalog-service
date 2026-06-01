package com.example.fundcatalog.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * A fund in the market-wide universe (reference data, NOT tenant-scoped). Indexed on the columns
 * the explore screen filters/sorts by so the catalog stays fast as the universe grows.
 */
@Entity
@Table(
    name = "catalog_fund",
    indexes = [
        Index(name = "idx_fund_category", columnList = "category"),
        Index(name = "idx_fund_rating", columnList = "rating"),
        Index(name = "idx_fund_return1y", columnList = "return1y"),
    ],
)
class CatalogFund(
    @Id val id: UUID,
    val name: String,
    val amc: String,
    @Enumerated(EnumType.STRING) val category: FundCategory,
    val subCategory: String,
    @Enumerated(EnumType.STRING) val riskLevel: RiskLevel,
    val expenseRatio: BigDecimal,
    val aumCrore: BigDecimal,
    val rating: Int,
    val return1y: BigDecimal,
    val return3y: BigDecimal,
    val return5y: BigDecimal,
    val currentNav: BigDecimal,
    val navDate: LocalDate,
    val benchmark: String,
    val fundManager: String,
    val minSip: Int,
    val minLumpsum: Int,
)

/** One NAV observation for a fund on a date — drives the fund-detail NAV chart. */
@Entity
@Table(
    name = "nav_point",
    indexes = [Index(name = "idx_nav_fund_date", columnList = "fundId,date")],
)
class NavPoint(
    @Id val id: UUID,
    val fundId: UUID,
    val date: LocalDate,
    val nav: BigDecimal,
)
