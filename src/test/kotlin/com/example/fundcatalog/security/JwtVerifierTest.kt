package com.example.fundcatalog.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class JwtVerifierTest {
    private val secret = "test-secret-test-secret-test-secret-32bytes!!"
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    @Test
    fun `valid token returns the subject`() {
        val token = Jwts.builder().subject("user-123").signWith(key).compact()
        assertEquals("user-123", JwtVerifier(secret).verify(token))
    }

    @Test
    fun `garbage and wrong-secret tokens return null`() {
        assertNull(JwtVerifier(secret).verify("not-a-jwt"))
        val foreign = Jwts.builder().subject("x")
            .signWith(Keys.hmacShaKeyFor("another-secret-another-secret-32bytes!!".toByteArray(StandardCharsets.UTF_8)))
            .compact()
        assertNull(JwtVerifier(secret).verify(foreign))
    }
}
