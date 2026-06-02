package com.example.fundcatalog.repository

import com.example.fundcatalog.domain.CatalogFund
import com.example.fundcatalog.domain.FundCategory
import com.example.fundcatalog.domain.NavPoint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.LocalDate
import java.util.UUID

// Search is built with JPA Specifications in the service (dynamic predicates) so that absent
// filters add no SQL at all — avoids binding untyped NULL params, which Postgres rejects.
interface CatalogFundRepository : JpaRepository<CatalogFund, UUID>, JpaSpecificationExecutor<CatalogFund> {
    fun countByCategory(category: FundCategory): Long
    fun findByAmfiSchemeCodeIsNotNull(): List<CatalogFund>
}

interface NavPointRepository : JpaRepository<NavPoint, UUID> {
    fun findByFundIdOrderByDateAsc(fundId: UUID): List<NavPoint>
    fun findByFundIdAndDateGreaterThanEqualOrderByDateAsc(fundId: UUID, date: LocalDate): List<NavPoint>
}
