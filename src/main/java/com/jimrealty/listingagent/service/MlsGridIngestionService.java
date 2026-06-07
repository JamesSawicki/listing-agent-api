package com.jimrealty.listingagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimrealty.listingagent.mapper.MlsGridListingMapper;
import com.jimrealty.listingagent.media.CloudflareImagesClient;
import com.jimrealty.listingagent.media.MediaIngestionService;
import com.jimrealty.listingagent.model.IngestionResult;
import com.jimrealty.listingagent.model.Listing;
import com.jimrealty.listingagent.repository.ListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Pulls listing data from the MLS Grid RESO Web API and persists it locally.
 *
 * ── Architecture ────────────────────────────────────────────────────────────
 * MLS Grid is a replication API, not a query API. You pull everything locally
 * and query your own database. There are two modes:
 *
 *   runInitialImport()  — Full pull of all current listings (MlgCanView=true).
 *                         Use once on a fresh database or after a major reset.
 *
 *   runDeltaSync()      — Incremental pull of only records changed since the
 *                         greatest ModificationTimestamp already in our DB.
 *                         Run every 15 minutes in production. Safe to run more
 *                         often — it's idempotent.
 *
 * ── Paging ──────────────────────────────────────────────────────────────────
 * MLS Grid uses OData-style cursor paging. Each response body contains:
 *   - "value": array of Property records for this page
 *   - "@odata.nextLink": full URL for the next page (absent on the last page)
 * We follow nextLink until it's gone.
 *
 * ── Upsert strategy ─────────────────────────────────────────────────────────
 * We find each incoming record by listingKey (MLS Grid's system primary key).
 * If found → update in place, preserving our surrogate DB id and any locally-
 * computed fields (amenityScores) the MLS feed knows nothing about.
 * If not found → insert as a new record.
 *
 * ── Deletion ────────────────────────────────────────────────────────────────
 * MLS Grid does not send DELETE requests. Instead, records get MlgCanView=false.
 * We check this flag on every record. If false, we delete it from our DB.
 * After 7 days, MLS Grid removes these records from the feed entirely —
 * so delta sync must run at least weekly to catch all deletes.
 *
 * ── Rate limits ─────────────────────────────────────────────────────────────
 * MLS Grid enforces: max 2 requests/second, 7200/hour, 40000/24h.
 * We sleep 600ms between page requests (safely under 2 RPS).
 * For an initial import, email support@mlsgrid.com to request a grace period.
 *
 * ── gzip ────────────────────────────────────────────────────────────────────
 * All MLS Grid responses are gzip-compressed. HttpComponentsClientHttpRequestFactory
 * (Apache HttpClient 5) handles decompression automatically. Without it, the raw
 * gzip bytes would be passed to the JSON parser and fail immediately.
 * Requires: org.apache.httpcomponents.client5:httpclient5 on the classpath.
 *
 * ── Environment variables ───────────────────────────────────────────────────
 * MLS_GRID_TOKEN              — Bearer token from your MLS Grid account
 * MLS_GRID_BASE_URL           — Default: https://api-demo.mlsgrid.com/v2
 * MLS_GRID_ORIGINATING_SYSTEM — Default: northstar
 */
@Service
public class MlsGridIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MlsGridIngestionService.class);

    /** Max records per page. MLS Grid's hard cap drops to 1000 with $expand; 500 is a safe default. */
    private static final int PAGE_SIZE = 500;

    /** Milliseconds to sleep between page requests. Keeps us safely under the 2 RPS rate limit. */
    private static final long MS_BETWEEN_REQUESTS = 600;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final MlsGridListingMapper mapper;
    private final ListingRepository listingRepository;
    private final MediaIngestionService mediaIngestionService;
    private final CloudflareImagesClient cloudflareImagesClient;
    private final String baseUrl;
    private final String originatingSystem;

    public MlsGridIngestionService(
            ObjectMapper objectMapper,
            MlsGridListingMapper mapper,
            ListingRepository listingRepository,
            MediaIngestionService mediaIngestionService,
            CloudflareImagesClient cloudflareImagesClient,
            @Value("${mls.grid.token}") String token,
            @Value("${mls.grid.base-url:https://api-demo.mlsgrid.com/v2}") String baseUrl,
            @Value("${mls.grid.originating-system:northstar}") String originatingSystem) {

        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.listingRepository = listingRepository;
        this.mediaIngestionService = mediaIngestionService;
        this.cloudflareImagesClient = cloudflareImagesClient;
        this.baseUrl = baseUrl;
        this.originatingSystem = originatingSystem;

        // HttpComponentsClientHttpRequestFactory uses Apache HttpClient 5.
        // Why not the default SimpleClientHttpRequestFactory (HttpURLConnection)?
        // HttpURLConnection does NOT automatically decompress gzip responses — you'd
        // need to manually wrap the stream in GZIPInputStream. Apache HttpClient 5
        // handles Content-Encoding: gzip transparently.
        //
        // Requires: org.apache.httpcomponents.client5:httpclient5 in pom.xml/build.gradle.
        // Spring Boot's dependency management provides the version.
        this.restClient = RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept-Encoding", "gzip, deflate")
                .build();
    }

    // -------------------------------------------------------------------------
    // Public entry points
    // -------------------------------------------------------------------------

    /**
     * Full import: pulls all records where MlgCanView=true.
     *
     * Use this for the first run on an empty database, or after a full data reset.
     * For all subsequent runs, use runDeltaSync() — it's faster and respects rate limits.
     *
     * Note the MlgCanView=true filter: on initial import we DON'T want records already
     * marked for deletion. On delta sync (runDeltaSync), we OMIT this filter so we
     * receive delete signals (MlgCanView=false) for records that changed since last sync.
     */
    public IngestionResult runInitialImport() {
        log.info("=== Starting MLS Grid initial import [system={}] ===", originatingSystem);
        String filter = String.format(
                "OriginatingSystemName eq '%s' and MlgCanView eq true",
                originatingSystem);
        return runIngestion(filter, "initial-import");
    }

    /**
     * Delta sync: pulls only records modified since the greatest ModificationTimestamp
     * currently in our database.
     *
     * If the database is empty, falls back to a full historical pull (same as runInitialImport
     * minus the MlgCanView filter — meaning we'll process any deletion signals too).
     *
     * Why no MlgCanView filter here? During delta sync, a record with MlgCanView=false
     * is an explicit deletion signal from MLS Grid. We need to receive it to know to
     * delete our local copy. Filtering it out would cause stale records to accumulate.
     */
    public IngestionResult runDeltaSync() {
        // Find the most recent modification timestamp we've already ingested.
        // If the DB is empty, use the epoch so we pull everything.
        Instant lastSeen = listingRepository.findMaxModificationTimestamp()
                .orElse(Instant.parse("2000-01-01T00:00:00.000Z"));

        log.info("=== Starting MLS Grid delta sync [system={}, since={}] ===",
                originatingSystem, lastSeen);

        String filter = String.format(
                "OriginatingSystemName eq '%s' and ModificationTimestamp gt %s",
                originatingSystem, lastSeen);
        return runIngestion(filter, "delta-sync");
    }

    // -------------------------------------------------------------------------
    // Core paging loop
    // -------------------------------------------------------------------------

    /**
     * Executes an ingestion run for a given OData filter string.
     *
     * Follows the @odata.nextLink cursor through all result pages, processing
     * each record with processRecord(). Logs progress per page and a summary at end.
     */
    private IngestionResult runIngestion(String filter, String operationLabel) {
        int inserted = 0, updated = 0, deleted = 0, errors = 0;
        Instant start = Instant.now();

        // The $expand=Media,Rooms is what makes photo and room data come inline on
        // the Property record, rather than requiring separate resource lookups.
        // Tradeoff: the per-request limit drops from 5000 to 1000 when using $expand,
        // and responses are larger. PAGE_SIZE=500 is a safe, conservative page size.
        String nextUrl = baseUrl + "/Property?$filter=" + encodeFilter(filter)
                + "&$expand=Media,Rooms&$top=" + PAGE_SIZE;

        int pageNumber = 0;

        while (nextUrl != null) {
            pageNumber++;
            log.info("[{}] Page {} — {}", operationLabel, pageNumber, nextUrl);

            try {
                String body = fetchPage(nextUrl);
                if (body == null) break;

                JsonNode root = objectMapper.readTree(body);

                // Process every Property record on this page
                for (JsonNode property : root.path("value")) {
                    try {
                        RecordOutcome outcome = processRecord(property);
                        switch (outcome) {
                            case INSERTED -> inserted++;
                            case UPDATED  -> updated++;
                            case DELETED  -> deleted++;
                        }
                    } catch (Exception e) {
                        errors++;
                        log.error("[{}] Error on record {}: {}",
                                operationLabel,
                                property.path("ListingKey").asText("unknown"),
                                e.getMessage());
                    }
                }

                // Advance to next page, or end loop if we've exhausted the results
                JsonNode nextLink = root.path("@odata.nextLink");
                nextUrl = (nextLink.isMissingNode() || nextLink.isNull())
                        ? null
                        : nextLink.asText();

                // Respect the 2 RPS rate limit — sleep before the next request
                if (nextUrl != null) {
                    Thread.sleep(MS_BETWEEN_REQUESTS);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[{}] Sleep interrupted — stopping at page {}", operationLabel, pageNumber);
                break;
            } catch (RestClientException e) {
                // HTTP-level error (4xx/5xx) — log and abort the run
                errors++;
                log.error("[{}] HTTP error on page {}: {}", operationLabel, pageNumber, e.getMessage());
                break;
            } catch (Exception e) {
                errors++;
                log.error("[{}] Unexpected error on page {}: {}",
                        operationLabel, pageNumber, e.getMessage(), e);
                break;
            }
        }

        IngestionResult result = new IngestionResult(inserted, updated, deleted, errors,
                Duration.between(start, Instant.now()));

        log.info("=== [{}] Complete — inserted={}, updated={}, deleted={}, errors={}, time={} ===",
                operationLabel, inserted, updated, deleted, errors, result.duration());
        return result;
    }

    // -------------------------------------------------------------------------
    // Single-record processing
    // -------------------------------------------------------------------------

    /**
     * Processes one Property record from the feed: either upsert or delete.
     *
     * @return RecordOutcome indicating what action was taken
     * @throws IllegalArgumentException if the record has no ListingKey
     */
    private RecordOutcome processRecord(JsonNode property) {
        String listingKey = property.path("ListingKey").asText(null);
        if (listingKey == null || listingKey.isBlank()) {
            throw new IllegalArgumentException("Record has no ListingKey — cannot process");
        }

        boolean canView = property.path("MlgCanView").asBoolean(true);

        if (!canView) {
            // MlgCanView=false is MLS Grid's deletion mechanism.
            // Delete the local row + clean up any Cloudflare-hosted media we still own.
            listingRepository.findByListingKey(listingKey).ifPresent(existing -> {
                deleteCloudflareMedia(existing);
                listingRepository.delete(existing);
                log.debug("Deleted listing [{}]", listingKey);
            });
            return RecordOutcome.DELETED;
        }

        // Map the RESO JSON to our entity
        Listing mapped = mapper.toListing(property);

        Optional<Listing> existing = listingRepository.findByListingKey(listingKey);

        if (existing.isPresent()) {
            // UPDATE path: preserve surrogate id + locally-computed fields the MLS
            // feed knows nothing about (amenityScores, photoIds, etc.).
            Listing prev = existing.get();
            mapped.setId(prev.getId());
            mapped.setAmenityScores(prev.getAmenityScores());

            // Re-ingest media only when the upstream Media array actually changed —
            // otherwise carry forward the existing Cloudflare Image IDs and skip
            // the expensive download + upload cycle.
            boolean mediaChanged = !Objects.equals(mapped.getMediaJson(), prev.getMediaJson());
            if (!mediaChanged) {
                mapped.setPhotoIds(prev.getPhotoIds());
                mapped.setFloorPlanIds(prev.getFloorPlanIds());
                mapped.setVirtualTourUrls(prev.getVirtualTourUrls());
                mapped.setMediaIngestionStatus(prev.getMediaIngestionStatus());
            } else {
                // Mark pending; the async pipeline will flip it to COMPLETE/FAILED.
                mapped.setMediaIngestionStatus("PENDING");
            }

            Listing saved = listingRepository.save(mapped);
            if (mediaChanged) {
                mediaIngestionService.ingestForListingAsync(saved.getId());
            }
            return RecordOutcome.UPDATED;

        } else {
            // INSERT path: no id set — JPA auto-generates one via IDENTITY strategy.
            mapped.setMediaIngestionStatus("PENDING");
            Listing saved = listingRepository.save(mapped);
            mediaIngestionService.ingestForListingAsync(saved.getId());
            return RecordOutcome.INSERTED;
        }
    }

    /**
     * Best-effort cleanup of Cloudflare-hosted media when a listing is being
     * deleted (MlgCanView=false). Failures here are logged but do not block
     * the deletion — orphaned CF images are wasted storage, not a correctness bug.
     */
    private void deleteCloudflareMedia(Listing listing) {
        if (!cloudflareImagesClient.isConfigured()) return;
        deleteIdsJson(listing.getPhotoIds());
        deleteIdsJson(listing.getFloorPlanIds());
    }

    private void deleteIdsJson(String idsJson) {
        if (idsJson == null || idsJson.isBlank()) return;
        try {
            JsonNode arr = objectMapper.readTree(idsJson);
            if (!arr.isArray()) return;
            for (JsonNode idNode : arr) {
                String id = idNode.asText(null);
                if (id != null && !id.isBlank()) {
                    cloudflareImagesClient.deleteImage(id);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to delete Cloudflare media for orphaned listing: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // HTTP
    // -------------------------------------------------------------------------

    /**
     * Executes a single GET request and returns the response body as a String.
     *
     * Why return String instead of a typed DTO? We need to inspect @odata.nextLink
     * alongside the "value" array, and parse errors on individual records should not
     * abort the entire page. Working with the raw JSON string and then reading it
     * with ObjectMapper gives us that control.
     */
    private String fetchPage(String url) {
        String body = restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            log.warn("Empty response body from {}", url);
        }
        return body;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * URL-encodes an OData filter string for safe inclusion in a query parameter.
     *
     * URLEncoder encodes spaces as '+', but OData expects '%20'.
     * We replace '+' with '%20' after encoding.
     *
     * The single quotes around string values ('northstar') are also encoded as %27.
     * MLS Grid accepts both encoded and unencoded single quotes, but encoded is safer.
     */
    private String encodeFilter(String filter) {
        return URLEncoder.encode(filter, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Outcome of processing a single Property record. */
    private enum RecordOutcome { INSERTED, UPDATED, DELETED }
}