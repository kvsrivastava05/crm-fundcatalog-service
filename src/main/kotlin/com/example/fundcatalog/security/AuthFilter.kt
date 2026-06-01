package com.example.fundcatalog.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Requires a valid Bearer JWT on /funds (and everything under it) — 401 otherwise — except the
 * open health check. CORS preflight (OPTIONS) is allowed through to Spring's CORS handler since it
 * never carries an Authorization header.
 */
class AuthFilter(
    private val jwt: JwtVerifier,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authenticated = bearerToken(request)?.let(jwt::verify) != null
        if (!authenticated && requiresAuth(request)) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"error":"unauthorized"}""")
            return
        }
        filterChain.doFilter(request, response)
    }

    private fun bearerToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith(BEARER) }
            ?.substring(BEARER.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun requiresAuth(request: HttpServletRequest): Boolean {
        if (request.method.equals("OPTIONS", ignoreCase = true)) return false // CORS preflight carries no token
        val uri = request.requestURI
        return (uri == FUNDS || uri.startsWith(FUNDS_PREFIX)) && uri != HEALTH
    }

    companion object {
        private const val BEARER = "Bearer "
        private const val FUNDS = "/funds"
        private const val FUNDS_PREFIX = "/funds/"
        private const val HEALTH = "/funds/health"
    }
}
