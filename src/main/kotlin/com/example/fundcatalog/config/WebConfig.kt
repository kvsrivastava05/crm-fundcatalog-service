package com.example.fundcatalog.config

import com.example.fundcatalog.security.AuthFilter
import com.example.fundcatalog.security.JwtVerifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val jwtVerifier: JwtVerifier,
    @Value("\${app.cors.allowed-origin:*}") private val allowedOrigin: String,
) : WebMvcConfigurer {

    @Bean
    fun authFilter(): FilterRegistrationBean<AuthFilter> =
        FilterRegistrationBean(AuthFilter(jwtVerifier)).apply {
            order = Ordered.HIGHEST_PRECEDENCE
            addUrlPatterns("/*")
        }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(allowedOrigin)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
    }
}
