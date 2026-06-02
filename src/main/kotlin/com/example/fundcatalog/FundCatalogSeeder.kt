package com.example.fundcatalog

import com.example.fundcatalog.domain.CatalogFund
import com.example.fundcatalog.domain.FundCategory
import com.example.fundcatalog.domain.FundCategory.DEBT
import com.example.fundcatalog.domain.FundCategory.ELSS
import com.example.fundcatalog.domain.FundCategory.EQUITY
import com.example.fundcatalog.domain.FundCategory.GOLD
import com.example.fundcatalog.domain.FundCategory.HYBRID
import com.example.fundcatalog.domain.FundCategory.INDEX
import com.example.fundcatalog.domain.FundCategory.INTERNATIONAL
import com.example.fundcatalog.domain.FundCategory.LIQUID
import com.example.fundcatalog.domain.NavPoint
import com.example.fundcatalog.domain.RiskLevel
import com.example.fundcatalog.domain.RiskLevel.HIGH
import com.example.fundcatalog.domain.RiskLevel.LOW
import com.example.fundcatalog.domain.RiskLevel.LOW_TO_MODERATE
import com.example.fundcatalog.domain.RiskLevel.MODERATE
import com.example.fundcatalog.domain.RiskLevel.MODERATELY_HIGH
import com.example.fundcatalog.domain.RiskLevel.VERY_HIGH
import com.example.fundcatalog.repository.CatalogFundRepository
import com.example.fundcatalog.repository.NavPointRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sin

/**
 * Seeds a realistic, market-like fund universe (~36 funds across every category) plus 3 years of
 * monthly NAV history per fund for the detail chart. Idempotent: skips if already populated.
 */
@Component
class FundCatalogSeeder(
    private val funds: CatalogFundRepository,
    private val navPoints: NavPointRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        if (funds.count() > 0L) return
        val today = LocalDate.now()

        // Each fund's last arg is its real AMFI scheme code (Direct-Growth) — the nightly ingestion
        // refreshes NAV from AMFI for these. The few without a clean AMFI match keep seeded NAVs.
        val universe = listOf(
            // EQUITY
            fund("Axis Bluechip Fund", "Axis MF", EQUITY, "Large Cap", HIGH, "1.62", "38200", 4, "18.4", "16.2", "14.8", "58.42", "Nifty 100 TRI", "Shreyash Devalkar", "120465"),
            fund("Mirae Asset Large Cap", "Mirae Asset", EQUITY, "Large Cap", HIGH, "1.54", "37600", 4, "19.1", "17.0", "15.6", "102.15", "Nifty 100 TRI", "Gaurav Misra", "118825"),
            fund("ICICI Pru Bluechip Fund", "ICICI Prudential", EQUITY, "Large Cap", HIGH, "1.45", "54100", 5, "21.3", "19.4", "16.1", "94.70", "Nifty 100 TRI", "Anish Tawakley", "120586"),
            fund("Kotak Emerging Equity", "Kotak MF", EQUITY, "Mid Cap", VERY_HIGH, "1.49", "41800", 5, "27.6", "24.1", "21.0", "112.30", "Nifty Midcap 150", "Pankaj Tibrewal"),
            fund("PGIM India Midcap Opp", "PGIM India", EQUITY, "Mid Cap", VERY_HIGH, "1.71", "9800", 4, "24.9", "23.0", "20.4", "62.05", "Nifty Midcap 150", "Aniruddha Naha", "125307"),
            fund("SBI Small Cap Fund", "SBI MF", EQUITY, "Small Cap", VERY_HIGH, "1.66", "24500", 5, "31.2", "27.8", "24.6", "168.90", "Nifty Smallcap 250", "R. Srinivasan", "125497"),
            fund("Nippon India Small Cap", "Nippon India", EQUITY, "Small Cap", VERY_HIGH, "1.55", "46200", 5, "34.0", "29.5", "26.1", "152.40", "Nifty Smallcap 250", "Samir Rachh", "118778"),
            fund("Parag Parikh Flexi Cap", "PPFAS MF", EQUITY, "Flexi Cap", VERY_HIGH, "1.33", "62100", 5, "23.7", "21.9", "19.8", "74.61", "Nifty 500 TRI", "Rajeev Thakkar", "122639"),
            fund("HDFC Flexi Cap Fund", "HDFC MF", EQUITY, "Flexi Cap", VERY_HIGH, "1.48", "52800", 4, "25.4", "22.6", "18.9", "1620.55", "Nifty 500 TRI", "Roshi Jain", "118955"),
            fund("Axis Focused 25 Fund", "Axis MF", EQUITY, "Focused", VERY_HIGH, "1.70", "13400", 3, "16.8", "14.3", "13.5", "47.92", "Nifty 500 TRI", "Sachin Relekar", "120468"),
            fund("Canara Robeco Emerging Eq", "Canara Robeco", EQUITY, "Large & Mid Cap", VERY_HIGH, "1.58", "20100", 4, "22.1", "20.3", "18.2", "228.10", "Nifty LargeMidcap 250", "Amit Nadekar"),
            fund("ICICI Pru Value Discovery", "ICICI Prudential", EQUITY, "Value", VERY_HIGH, "1.50", "44900", 4, "26.2", "23.8", "19.1", "402.77", "Nifty 500 TRI", "Sankaran Naren", "120323"),
            // DEBT
            fund("HDFC Corporate Bond", "HDFC MF", DEBT, "Corporate Bond", LOW_TO_MODERATE, "0.45", "28700", 4, "7.4", "6.8", "7.1", "29.74", "CRISIL Corp Bond", "Anupam Joshi", "118987"),
            fund("ICICI Pru Short Term", "ICICI Prudential", DEBT, "Short Duration", LOW_TO_MODERATE, "0.49", "20300", 4, "7.2", "6.6", "6.9", "54.11", "CRISIL Short Term", "Manish Banthia", "120754"),
            fund("SBI Magnum Gilt Fund", "SBI MF", DEBT, "Gilt", MODERATE, "0.46", "8900", 4, "8.1", "7.0", "7.3", "61.28", "CRISIL Dynamic Gilt", "Dinesh Ahuja", "119707"),
            fund("Kotak Banking & PSU Debt", "Kotak MF", DEBT, "Banking & PSU", LOW_TO_MODERATE, "0.39", "6100", 3, "7.0", "6.5", "6.8", "59.04", "NIFTY Banking & PSU", "Deepak Agrawal", "123693"),
            fund("Aditya Birla SL Dynamic Bond", "Aditya Birla SL", DEBT, "Dynamic Bond", MODERATE, "0.62", "1700", 3, "8.4", "6.9", "6.7", "39.55", "CRISIL Dynamic Bond", "Bhupesh Bameta", "119505"),
            fund("HDFC Credit Risk Debt", "HDFC MF", DEBT, "Credit Risk", MODERATE, "0.98", "8200", 3, "8.0", "7.4", "7.6", "22.41", "CRISIL Credit Risk", "Shobhit Mehrotra", "128051"),
            // HYBRID
            fund("ICICI Pru Balanced Advantage", "ICICI Prudential", HYBRID, "Dynamic Allocation", MODERATELY_HIGH, "1.42", "53600", 4, "14.6", "13.1", "12.4", "67.33", "CRISIL Hybrid 50+50", "Sankaran Naren", "120377"),
            fund("HDFC Balanced Advantage", "HDFC MF", HYBRID, "Dynamic Allocation", MODERATELY_HIGH, "1.38", "78900", 5, "16.2", "15.0", "13.8", "412.66", "NIFTY 50 Hybrid", "Anil Bamboli", "118968"),
            fund("SBI Equity Hybrid Fund", "SBI MF", HYBRID, "Aggressive Hybrid", MODERATELY_HIGH, "1.45", "67200", 4, "15.1", "13.7", "13.0", "271.88", "CRISIL Hybrid 35+65", "R. Srinivasan", "119609"),
            fund("ICICI Pru Reg Savings", "ICICI Prudential", HYBRID, "Conservative Hybrid", MODERATE, "0.92", "3200", 3, "10.4", "9.1", "9.5", "67.10", "CRISIL Hybrid 85+15", "Manish Banthia", "120616"),
            // LIQUID
            fund("Aditya Birla SL Liquid", "Aditya Birla SL", LIQUID, "Liquid", LOW, "0.21", "42300", 4, "7.0", "5.6", "5.5", "372.55", "CRISIL Liquid", "Kaustubh Gupta", "119568"),
            fund("HDFC Overnight Fund", "HDFC MF", LIQUID, "Overnight", LOW, "0.10", "13800", 3, "6.6", "5.2", "5.1", "3401.20", "CRISIL Overnight", "Anil Bamboli", "119110"),
            // ELSS
            fund("Mirae Asset Tax Saver", "Mirae Asset", ELSS, "ELSS", VERY_HIGH, "1.58", "20800", 5, "22.3", "20.6", "18.9", "44.18", "Nifty 500 TRI", "Neelesh Surana", "135781"),
            fund("Axis Long Term Equity", "Axis MF", ELSS, "ELSS", VERY_HIGH, "1.55", "34100", 3, "16.9", "14.8", "15.2", "81.66", "Nifty 500 TRI", "Shreyash Devalkar", "120503"),
            fund("Quant ELSS Tax Saver", "Quant MF", ELSS, "ELSS", VERY_HIGH, "1.77", "9600", 5, "29.8", "26.4", "23.1", "388.42", "Nifty 500 TRI", "Sandeep Tandon", "120847"),
            // INDEX
            fund("UTI Nifty 50 Index", "UTI MF", INDEX, "Nifty 50", VERY_HIGH, "0.20", "16700", 4, "17.2", "15.8", "14.4", "147.83", "Nifty 50 TRI", "Sharwan Goyal", "120716"),
            fund("HDFC Index Nifty 50", "HDFC MF", INDEX, "Nifty 50", VERY_HIGH, "0.20", "12300", 4, "17.1", "15.7", "14.3", "212.04", "Nifty 50 TRI", "Arun Agarwal", "119063"),
            fund("ICICI Pru Sensex Index", "ICICI Prudential", INDEX, "Sensex", VERY_HIGH, "0.16", "1900", 3, "16.6", "15.3", "14.0", "26.55", "S&P BSE Sensex TRI", "Kayzad Eghlim"),
            fund("Motilal Oswal Nifty Next 50", "Motilal Oswal", INDEX, "Nifty Next 50", VERY_HIGH, "0.36", "1100", 3, "20.9", "17.1", "15.5", "21.88", "Nifty Next 50 TRI", "Swapnil Mayekar", "147796"),
            // GOLD
            fund("Nippon India Gold Savings", "Nippon India", GOLD, "Gold FoF", HIGH, "0.34", "2100", 4, "15.7", "13.2", "12.9", "26.94", "Domestic Gold Price", "Mehul Dama", "118663"),
            fund("HDFC Gold Fund", "HDFC MF", GOLD, "Gold FoF", HIGH, "0.38", "2600", 4, "15.4", "13.0", "12.6", "23.61", "Domestic Gold Price", "Arun Agarwal", "119132"),
            // INTERNATIONAL
            fund("Motilal Oswal Nasdaq 100 FoF", "Motilal Oswal", INTERNATIONAL, "Nasdaq 100", VERY_HIGH, "0.58", "4700", 5, "29.4", "21.8", "22.6", "34.12", "Nasdaq 100 TRI (INR)", "Ankush Sood", "145552"),
            fund("Franklin India Feeder US Opp", "Franklin Templeton", INTERNATIONAL, "US Equity", VERY_HIGH, "0.62", "3300", 4, "24.1", "18.7", "19.9", "68.45", "Russell 3000 (INR)", "Sandeep Manam"),
            fund("Edelweiss Gr China Equity FoF", "Edelweiss MF", INTERNATIONAL, "Greater China", VERY_HIGH, "1.43", "1500", 2, "12.6", "4.2", "8.8", "27.30", "MSCI Golden Dragon", "Bhavesh Jain", "140243"),
        )
        funds.saveAll(universe)

        val history = ArrayList<NavPoint>(universe.size * 37)
        universe.forEach { f ->
            val monthlyGrowth = 1.0 + (f.return1y.toDouble() / 100.0) / 12.0
            for (k in 36 downTo 0) {
                val base = f.currentNav.toDouble() / monthlyGrowth.pow(k.toDouble())
                val wiggle = 1.0 + 0.015 * sin(k.toDouble())
                val nav = BigDecimal(base * wiggle).setScale(4, RoundingMode.HALF_UP)
                history += NavPoint(UUID.randomUUID(), f.id, today.minusMonths(k.toLong()), nav)
            }
        }
        navPoints.saveAll(history)
    }

    private fun fund(
        name: String, amc: String, cat: FundCategory, sub: String, risk: RiskLevel,
        expense: String, aum: String, rating: Int, r1: String, r3: String, r5: String,
        nav: String, benchmark: String, manager: String, amfi: String? = null,
    ) = CatalogFund(
        id = UUID.randomUUID(), name = name, amc = amc, category = cat, subCategory = sub,
        riskLevel = risk, expenseRatio = BigDecimal(expense), aumCrore = BigDecimal(aum),
        rating = rating, return1y = BigDecimal(r1), return3y = BigDecimal(r3), return5y = BigDecimal(r5),
        currentNav = BigDecimal(nav), navDate = LocalDate.now(), benchmark = benchmark,
        fundManager = manager, minSip = 500, minLumpsum = 5000, amfiSchemeCode = amfi,
    )
}
