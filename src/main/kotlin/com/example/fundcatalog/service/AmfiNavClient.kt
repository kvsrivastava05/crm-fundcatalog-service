package com.example.fundcatalog.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Downloads AMFI's daily NAVAll.txt. Returns the body on 2xx, else null (any error -> null). */
@Component
class AmfiNavClient(
    @Value("\${app.amfi.nav-url}") private val url: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val http: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL) // amfiindia.com -> portal.amfiindia.com
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    fun fetch(): String? = try {
        val request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) response.body() else null
    } catch (ex: Exception) {
        log.warn("AMFI NAV fetch failed for {}: {}", url, ex.toString())
        null
    }
}
