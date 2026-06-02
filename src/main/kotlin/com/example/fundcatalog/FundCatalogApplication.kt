package com.example.fundcatalog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FundCatalogApplication

fun main(args: Array<String>) {
    runApplication<FundCatalogApplication>(*args)
}
