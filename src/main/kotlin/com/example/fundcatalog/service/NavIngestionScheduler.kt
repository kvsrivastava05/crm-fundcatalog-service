package com.example.fundcatalog.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Triggers the AMFI NAV refresh on a nightly cron (default 22:00 IST, after AMFI publishes). */
@Component
class NavIngestionScheduler(
    private val ingestion: NavIngestionService,
) {
    @Scheduled(cron = "\${app.amfi.cron:0 0 22 * * *}", zone = "Asia/Kolkata")
    fun refreshNightly() {
        ingestion.ingest()
    }
}
