package com.example.fundcatalog.web.controller

import com.example.fundcatalog.domain.FundCategory
import com.example.fundcatalog.domain.RiskLevel
import com.example.fundcatalog.service.FundCatalogService
import com.example.fundcatalog.web.dto.CategoryCount
import com.example.fundcatalog.web.dto.FundCard
import com.example.fundcatalog.web.dto.FundDetail
import com.example.fundcatalog.web.dto.NavPointDto
import com.example.fundcatalog.web.dto.PageResponse
import com.example.fundcatalog.web.dto.SubCategoryCount
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.util.UUID

/**
 * Market-wide fund catalog. Reference data is shared and read-heavy, so list responses carry
 * public Cache-Control headers — the gateway/CDN can cache them and the service scales out as a
 * stateless reader. Everything except /funds/health requires a valid JWT (see AuthFilter).
 */
@RestController
@RequestMapping("/funds")
class FundCatalogController(
    private val service: FundCatalogService,
) {

    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "ok")

    @GetMapping
    fun search(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) category: FundCategory?,
        @RequestParam(required = false) subCategory: String?,
        @RequestParam(required = false) risk: RiskLevel?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "12") size: Int,
    ): ResponseEntity<PageResponse<FundCard>> =
        ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
            .body(service.search(q, category, risk, sort, page, size, subCategory))

    @GetMapping("/categories")
    fun categories(): ResponseEntity<List<CategoryCount>> =
        ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(service.categories())

    @GetMapping("/subcategories")
    fun subCategories(): ResponseEntity<List<SubCategoryCount>> =
        ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(service.subCategories())

    @GetMapping("/compare")
    fun compare(@RequestParam ids: List<UUID>): ResponseEntity<List<FundDetail>> =
        ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
            .body(service.compare(ids))

    @GetMapping("/{id}")
    fun detail(@PathVariable id: UUID): FundDetail = service.detail(id)

    @GetMapping("/{id}/nav")
    fun nav(
        @PathVariable id: UUID,
        @RequestParam(required = false) range: String?,
    ): ResponseEntity<List<NavPointDto>> =
        ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePublic())
            .body(service.navHistory(id, range))
}
