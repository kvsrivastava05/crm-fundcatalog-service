package com.example.fundcatalog.web

import com.example.fundcatalog.repository.CatalogFundRepository
import com.example.fundcatalog.security.AuthFilter
import com.example.fundcatalog.security.JwtVerifier
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.nio.charset.StandardCharsets
import java.util.Date

@SpringBootTest
class FundCatalogMvcTest {
    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var funds: CatalogFundRepository

    private val secret = "test-secret-test-secret-test-secret-32bytes!!"
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilter<DefaultMockMvcBuilder>(AuthFilter(JwtVerifier(secret)), "/*")
            .build()
    }

    private fun auth(): HttpHeaders {
        val jwt = Jwts.builder().subject("b0000000-0000-0000-0000-000000000001")
            .issuedAt(Date()).expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(key).compact()
        return HttpHeaders().apply { setBearerAuth(jwt) }
    }

    private fun status(path: String, headers: HttpHeaders? = null): Int {
        val req = get(path)
        if (headers != null) req.headers(headers)
        return mvc.perform(req).andReturn().response.status
    }

    @Test
    fun `health is open without a token`() {
        assertEquals(200, status("/funds/health"))
    }

    @Test
    fun `explore search returns a cached page`() {
        val res = mvc.perform(get("/funds?sort=rating&page=0&size=6").headers(auth())).andReturn().response
        assertEquals(200, res.status)
        assertTrue(res.contentAsString.contains("\"totalElements\":36"))
        assertNotNull(res.getHeader("Cache-Control"))
    }

    @Test
    fun `categories, detail, nav and filters work`() {
        assertEquals(200, status("/funds/categories", auth()))
        val id = funds.findAll().first().id
        assertEquals(200, status("/funds/$id", auth()))
        assertEquals(200, status("/funds/$id/nav?range=1Y", auth()))
        assertEquals(200, status("/funds?category=DEBT", auth()))
        assertEquals(404, status("/funds/11111111-1111-1111-1111-111111111111", auth()))
        assertEquals(400, status("/funds?category=NONSENSE", auth())) // enum bind failure -> 400
    }

    @Test
    fun `sub-categories, compare and sub-category filter work`() {
        assertEquals(200, status("/funds/subcategories", auth()))
        assertEquals(200, status("/funds?subCategory=ELSS", auth()))
        val ids = funds.findAll().take(2).joinToString(",") { it.id.toString() }
        val compare = mvc.perform(get("/funds/compare?ids=$ids").headers(auth())).andReturn().response
        assertEquals(200, compare.status)
        assertEquals(400, status("/funds/compare?ids=not-a-uuid", auth())) // bad UUID -> 400
    }

    @Test
    fun `admin ingest-nav endpoint runs and reports a result`() {
        // test AMFI url is a dead address, so it reports fetched=false without hitting the network
        val res = mvc.perform(post("/funds/admin/ingest-nav").headers(auth())).andReturn().response
        assertEquals(200, res.status)
        assertTrue(res.contentAsString.contains("\"fetched\":false"))
    }

    @Test
    fun `requests without a valid token are 401`() {
        assertEquals(401, status("/funds"))
        assertEquals(401, mvc.perform(get("/funds").header("Authorization", "Bearer garbage")).andReturn().response.status)
        assertEquals(401, mvc.perform(get("/funds").header("Authorization", "Bearer ")).andReturn().response.status) // empty token
        assertEquals(401, mvc.perform(get("/funds").header("Authorization", "Basic abc")).andReturn().response.status) // not Bearer
    }

    @Test
    fun `CORS preflight is allowed through without a token`() {
        val res = mvc.perform(
            options("/funds").header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "GET"),
        ).andReturn().response
        assertEquals(200, res.status)
        assertNotNull(res.getHeader("Access-Control-Allow-Origin"))
    }
}
