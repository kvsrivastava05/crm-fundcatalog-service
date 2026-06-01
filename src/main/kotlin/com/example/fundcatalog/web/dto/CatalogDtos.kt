package com.example.fundcatalog.web.dto

import java.math.BigDecimal
import java.time.LocalDate

/** Generic page envelope mirrored across the CRM services so the frontend has one shape. */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrev: Boolean,
)

/** Compact fund row for the explore list. */
data class FundCard(
    val id: String,
    val name: String,
    val amc: String,
    val category: String,
    val subCategory: String,
    val riskLevel: String,
    val rating: Int,
    val expenseRatio: BigDecimal,
    val return1y: BigDecimal,
    val return3y: BigDecimal,
    val return5y: BigDecimal,
    val currentNav: BigDecimal,
)

/** Full fund profile for the detail screen. */
data class FundDetail(
    val id: String,
    val name: String,
    val amc: String,
    val category: String,
    val subCategory: String,
    val riskLevel: String,
    val rating: Int,
    val expenseRatio: BigDecimal,
    val aumCrore: BigDecimal,
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

/** A single point on the NAV history chart. */
data class NavPointDto(
    val date: LocalDate,
    val nav: BigDecimal,
)

/** Category with the number of funds in it (explore filter chips). */
data class CategoryCount(
    val category: String,
    val count: Long,
)

/** A fund "type" (sub-category like Small Cap / Large Cap) with its fund count — drives the
 *  "browse by type" chips and the compare-similar-funds flow. */
data class SubCategoryCount(
    val category: String,
    val subCategory: String,
    val count: Long,
)
