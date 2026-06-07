package com.jimrealty.listingagent.controller;

import com.jimrealty.listingagent.media.MediaIngestionService;
import com.jimrealty.listingagent.model.IngestionResult;
import com.jimrealty.listingagent.service.MlsGridIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Admin endpoints for MLS Grid ingestion.
 *
 * Both endpoints require HTTP Basic Auth (caught by SecurityConfig's catch-all rule).
 * Credentials come from ADMIN_USERNAME / ADMIN_PASSWORD environment variables.
 *
 * Endpoint summary:
 *   POST /api/admin/ingest           — Full initial import (async, 202 immediate return)
 *   POST /api/admin/ingest/delta     — Delta sync since last known timestamp (sync, waits for result)
 *
 * Why async for /ingest and sync for /ingest/delta?
 *   A full import pages through potentially thousands of records at 600ms per page —
 *   it can run for minutes. Returning 202 immediately and watching logs is the right UX.
 *   A delta sync typically touches dozens to hundreds of records and finishes in seconds,
 *   so returning the result directly is useful for verification.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final MlsGridIngestionService ingestionService;
    private final MediaIngestionService mediaIngestionService;

    public AdminController(MlsGridIngestionService ingestionService,
                           MediaIngestionService mediaIngestionService) {
        this.ingestionService = ingestionService;
        this.mediaIngestionService = mediaIngestionService;
    }

    /**
     * POST /api/admin/ingest
     *
     * Triggers a full MLS Grid import asynchronously.
     * Returns 202 Accepted immediately — watch application logs for progress.
     *
     * Use this for:
     *   - First run on an empty database
     *   - After manually clearing the listings table
     *   - After a schema migration that requires re-ingesting all data
     *
     * Uses CompletableFuture.runAsync() to run on the ForkJoinPool's common pool.
     * For production, wire in a named executor bean instead. Adequate for dev.
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> triggerInitialImport() {
        log.info("Full initial import triggered via admin endpoint");
        CompletableFuture.runAsync(() -> {
            try {
                IngestionResult result = ingestionService.runInitialImport();
                log.info("Full import completed: inserted={}, updated={}, deleted={}, errors={}",
                        result.inserted(), result.updated(), result.deleted(), result.errors());
            } catch (Exception e) {
                log.error("Full import failed with unexpected error", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of(
                "status", "started",
                "message", "Full MLS Grid import started asynchronously. Watch application logs for progress."
        ));
    }

    /**
     * POST /api/admin/ingest/delta
     *
     * Runs a delta sync synchronously and returns the result.
     * Pulls only records with ModificationTimestamp greater than the greatest
     * timestamp currently in our database.
     *
     * Blocks until complete — suitable in dev because delta syncs finish quickly.
     * In production, schedule this via @Scheduled rather than calling it manually.
     *
     * Example response:
     * {
     *   "inserted": 3,
     *   "updated": 12,
     *   "deleted": 1,
     *   "errors": 0,
     *   "durationSeconds": 4.2,
     *   "clean": true
     * }
     */
    @PostMapping("/ingest/delta")
    public ResponseEntity<Map<String, Object>> triggerDeltaSync() {
        log.info("Delta sync triggered via admin endpoint");
        try {
            IngestionResult result = ingestionService.runDeltaSync();
            return ResponseEntity.ok(Map.of(
                    "inserted",        result.inserted(),
                    "updated",         result.updated(),
                    "deleted",         result.deleted(),
                    "errors",          result.errors(),
                    "durationSeconds", result.duration().toMillis() / 1000.0,
                    "clean",           result.isClean()
            ));
        } catch (Exception e) {
            log.error("Delta sync failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/admin/media/backfill?limit=N
     *
     * Triggers asynchronous media backfill for the first N listings whose
     * media has not yet been ingested to Cloudflare Images (or whose status
     * is FAILED).
     *
     * Returns 202 immediately. Each listing involves multiple network
     * round-trips (MLS Grid download + Cloudflare upload, plus a 600ms
     * politeness sleep per file) so even N=10 can take a minute or two.
     * Watch application logs for progress.
     *
     * Default limit is small (10) — set explicitly for larger runs:
     *   POST /api/admin/media/backfill?limit=100
     */
    @PostMapping("/media/backfill")
    public ResponseEntity<Map<String, Object>> triggerMediaBackfill(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Media backfill triggered via admin endpoint, limit={}", limit);
        CompletableFuture.runAsync(() -> {
            try {
                mediaIngestionService.backfill(limit);
            } catch (Exception e) {
                log.error("Media backfill failed with unexpected error", e);
            }
        });
        return ResponseEntity.accepted().body(Map.of(
                "status",  "started",
                "limit",   limit,
                "message", "Media backfill started asynchronously. Watch application logs for progress."
        ));
    }

    /**
     * POST /api/admin/media/listing/{id}
     *
     * Synchronously ingests media for a single listing by surrogate ID.
     * Used for verification and debugging during development. Blocks for
     * up to ~30 seconds depending on photo count.
     */
    @PostMapping("/media/listing/{id}")
    public ResponseEntity<Map<String, Object>> triggerSingleListingMediaIngest(
            @PathVariable Long id) {
        log.info("Sync media ingestion triggered for listing [{}]", id);
        try {
            MediaIngestionService.MediaIngestionResult result =
                    mediaIngestionService.ingestForListing(id);
            return ResponseEntity.ok(Map.of(
                    "listingId",          id,
                    "photosUploaded",     result.photosUploaded(),
                    "floorPlansUploaded", result.floorPlansUploaded(),
                    "tourLinks",          result.tourLinks(),
                    "failed",             result.failed()
            ));
        } catch (Exception e) {
            log.error("Sync media ingestion failed for listing [{}]", id, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}