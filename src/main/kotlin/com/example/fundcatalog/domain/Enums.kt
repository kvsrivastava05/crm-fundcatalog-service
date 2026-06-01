package com.example.fundcatalog.domain

/** Broad asset class used to group the fund universe in the explore screen. */
enum class FundCategory {
    EQUITY, DEBT, HYBRID, LIQUID, ELSS, INDEX, GOLD, INTERNATIONAL
}

/** SEBI-style risk-o-meter level. */
enum class RiskLevel {
    LOW, LOW_TO_MODERATE, MODERATE, MODERATELY_HIGH, HIGH, VERY_HIGH
}
