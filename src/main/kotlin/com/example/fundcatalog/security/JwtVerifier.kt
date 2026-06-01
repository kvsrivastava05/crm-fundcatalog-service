package com.example.fundcatalog.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * Verifies an HS256 JWT (signature + expiry) minted by crm-auth-service and returns the subject
 * (user id) when valid, else null. The fund catalog is shared *reference* data — it authenticates
 * the caller but, unlike the tenant-scoped services, it does not filter by tenant.
 */
@Component
class JwtVerifier(
    @Value("\${app.jwt.secret}") secret: String,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun verify(token: String): String? =
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject
        } catch (ex: Exception) {
            null
        }
}
