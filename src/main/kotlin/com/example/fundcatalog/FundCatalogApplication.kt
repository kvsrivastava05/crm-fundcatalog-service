package com.example.fundcatalog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class FundCatalogApplication

fun main(args: Array<String>) {
    // AMFI's server omits an intermediate CA cert; let the JVM fetch it via the cert's AIA extension
    // so the chain validates (full TLS verification kept — nothing is disabled).
    System.setProperty("com.sun.security.enableAIAcaIssuers", "true")
    runApplication<FundCatalogApplication>(*args)
}
