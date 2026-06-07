package com.jimrealty.listingagent.media;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimrealty.listingagent.model.Listing;
import com.jimrealty.listingagent.repository.ListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Downloads listing media from MLS Grid and uploads to Cloudflare Images.
 *
 * ── Per-listing pipeline ──────────────────────────────────────────────────
 *   1. Load listing, parse mediaJson (set by MlsGridListingMapper)
 *   2. Group media by MediaCategory: Photo / FloorPlan / VirtualTour
 *   3. Sort each group by Order ascending (Order=1 = primary)
 *   4. For each Photo/FloorPlan:
 *      a. Download bytes from MediaURL with User-Agent = MLS_GRID_TOKEN
 *      b. Upload bytes to Cloudflare Images
 *      c. Append returned CF Image ID
 *      d. Sleep MS_BETWEEN_DOWNLOADS (rate-limit politeness)
 *   5. For each VirtualTour: collect URL + provider classification (no download)
 *   6. Persist photoIds / floorPlanIds / virtualTourUrls
 *      + mediaIngestionStatus = COMPLETE
 *
 * ── MLS Grid User-Agent quirk ─────────────────────────────────────────────
 * MLS Grid uses the OAuth2 Bearer token AS the User-Agent header on media
 * download requests (NOT as an Authorization header). This is non-standard
 * but explicit in MLS Grid Best Practices. Without the right User-Agent,
 * downloads return 403.
 *
 * ── Caps ──────────────────────────────────────────────────────────────────
 * MAX_PHOTOS_PER_LISTING = 125  defensive ceiling (current NorthstarMLS cap
 *                               is ~50, but headroom protects against feed
 *                               quirks and onboarding a second MLS later)
 * MAX_FLOOR_PLANS         = 30  same logic
 *
 * ── Failure handling ──────────────────────────────────────────────────────
 * Per-file failures are logged but do NOT abort the listing. After all
 * attempts: if at least one file uploaded, status = COMPLETE. If zero of N
 * attempted succeeded, status = FAILED.
 *
 * ── @Async behavior ───────────────────────────────────────────────────────
 * ingestForListingAsync() is @Async — fire-and-forget from
 * MlsGridIngestionService. Self-invocation breaks the proxy (Spring's
 * limitation): callers MUST inject this bean and call via the proxy, not
 * call this.method() from within the same class.
 */
@Service
public class MediaIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MediaIngestionService.class);

    public static final int  MAX_PHOTOS_PER_LISTING = 125;
    public static final int  MAX_FLOOR_PLANS        = 30;
    public static final long MS_BETWEEN_DOWNLOADS   = 600;

    private final ListingRepository listingRepository;
    private final ObjectMapper objectMapper;
    private final CloudflareImagesClient cloudflareClient;
    private final RestClient mlsGridMediaDownloadClient;
    private final String mlsGridToken;

    public MediaIngestionService(
            ListingRepository listingRepository,
            ObjectMapper objectMapper,
            CloudflareImagesClient cloudflareClient,
            @Value("${mls.grid.token:}") String mlsGridToken) {
        this.listingRepository = listingRepository;
        this.objectMapper = objectMapper;
        this.cloudflareClient = cloudflareClient;
        this.mlsGridToken = mlsGridToken;

        // Apache HttpClient 5 for binary downloads with proper redirect handling.
        // User-Agent is the OAuth2 token per MLS Grid docs. No Authorization header.
        this.mlsGridMediaDownloadClient = RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                .defaultHeader("User-Agent", mlsGridToken == null ? "" : mlsGridToken)
                .build();
    }

    // ── Public entry points ────────────────────────────────────────────────

    /**
     * Async wrapper. Fire-and-forget after upserting a listing from MLS Grid.
     * Returns immediately. Self-invocation breaks the proxy — callers must
     * inject this service and call via the bean.
     */
    @Async
    public CompletableFuture<Void> ingestForListingAsync(Long listingId) {
        try {
            ingestForListing(listingId);
        } catch (Exception e) {
            log.error("Async media ingestion failed for listing [{}]", listingId, e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Synchronous version. Used by the admin backfill endpoint and direct
     * sync triggers. Returns a per-listing summary.
     */
    public MediaIngestionResult ingestForListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId).orElse(null);
        if (listing == null) {
            log.warn("Listing [{}] not found for media ingestion", listingId);
            return MediaIngestionResult.empty();
        }
        return ingestForListing(listing);
    }

    /**
     * Backfills the first N listings whose media has not been fully ingested.
     * Skips listings with mediaIngestionStatus = "COMPLETE".
     * Skips listings with no mediaJson (nothing to ingest from).
     */
    public MediaIngestionBatchResult backfill(int limit) {
        if (!cloudflareClient.isConfigured()) {
            log.warn("Cloudflare Images not configured. Backfill skipped.");
            return MediaIngestionBatchResult.empty();
        }

        List<Listing> candidates = listingRepository
                .findMediaIngestionPending()
                .stream()
                .filter(l -> l.getMediaJson() != null && !l.getMediaJson().isBlank())
                .limit(limit)
                .toList();

        log.info("Media backfill: processing {} candidate listings (limit={})",
                candidates.size(), limit);

        int succeeded = 0;
        int failed = 0;
        int totalPhotosUploaded = 0;

        for (Listing listing : candidates) {
            try {
                MediaIngestionResult result = ingestForListing(listing);
                if (result.failed()) {
                    failed++;
                } else {
                    succeeded++;
                    totalPhotosUploaded += result.photosUploaded();
                }
            } catch (Exception e) {
                failed++;
                log.error("Backfill failed for listing [{}]", listing.getId(), e);
            }
        }

        log.info("Media backfill complete: {} succeeded, {} failed, {} photos uploaded total",
                succeeded, failed, totalPhotosUploaded);
        return new MediaIngestionBatchResult(
                candidates.size(), succeeded, failed, totalPhotosUploaded);
    }

    @Async
    public CompletableFuture<MediaIngestionBatchResult> backfillAsync(int limit) {
        return CompletableFuture.completedFuture(backfill(limit));
    }

    // ── Core per-listing work ──────────────────────────────────────────────

    private MediaIngestionResult ingestForListing(Listing listing) {
        if (!cloudflareClient.isConfigured()) {
            log.debug("Skipping listing [{}] — Cloudflare Images not configured", listing.getId());
            return MediaIngestionResult.empty();
        }
        if (listing.getMediaJson() == null || listing.getMediaJson().isBlank()) {
            log.debug("Listing [{}] has no mediaJson — nothing to ingest", listing.getId());
            return MediaIngestionResult.empty();
        }
        if (mlsGridToken == null || mlsGridToken.isBlank()) {
            log.warn("MLS_GRID_TOKEN not configured — cannot download media for listing [{}]",
                    listing.getId());
            return MediaIngestionResult.empty();
        }

        log.info("Media ingestion starting for listing [{}] (mls={})",
                listing.getId(), listing.getMlsId());

        listing.setMediaIngestionStatus("IN_PROGRESS");
        listingRepository.save(listing);

        try {
            JsonNode mediaArray = objectMapper.readTree(listing.getMediaJson());
            if (!mediaArray.isArray() || mediaArray.isEmpty()) {
                listing.setMediaIngestionStatus("COMPLETE");
                listingRepository.save(listing);
                return MediaIngestionResult.empty();
            }

            // Group by category (preserving original Order via sort below)
            List<JsonNode> photos = new ArrayList<>();
            List<JsonNode> floorPlans = new ArrayList<>();
            List<JsonNode> virtualTours = new ArrayList<>();

            for (JsonNode m : mediaArray) {
                String raw = m.path("MediaCategory").asText("Photo");
                switch (MediaCategory.fromResoString(raw)) {
                    case PHOTO        -> photos.add(m);
                    case FLOOR_PLAN   -> floorPlans.add(m);
                    case VIRTUAL_TOUR -> virtualTours.add(m);
                    case OTHER        -> { /* skip Video, Document, etc. */ }
                }
            }

            // Sort by Order ascending; missing/zero Order goes last
            Comparator<JsonNode> byOrder = Comparator.comparingInt(
                    n -> {
                        int o = n.path("Order").asInt(0);
                        return o <= 0 ? Integer.MAX_VALUE : o;
                    });
            photos.sort(byOrder);
            floorPlans.sort(byOrder);
            virtualTours.sort(byOrder);

            // Apply caps
            if (photos.size() > MAX_PHOTOS_PER_LISTING) {
                log.info("Listing [{}] has {} photos — capping at {}",
                        listing.getId(), photos.size(), MAX_PHOTOS_PER_LISTING);
                photos = photos.subList(0, MAX_PHOTOS_PER_LISTING);
            }
            if (floorPlans.size() > MAX_FLOOR_PLANS) {
                floorPlans = floorPlans.subList(0, MAX_FLOOR_PLANS);
            }

            List<String> photoIds      = uploadAll(photos, listing, "photo");
            List<String> floorPlanIds  = uploadAll(floorPlans, listing, "floorplan");
            List<Map<String, String>> tourLinks = extractTourLinks(virtualTours);

            listing.setPhotoIds(toJsonArray(photoIds));
            listing.setFloorPlanIds(toJsonArray(floorPlanIds));
            listing.setVirtualTourUrls(
                    tourLinks.isEmpty() ? null : objectMapper.writeValueAsString(tourLinks));

            // FAILED only when we attempted at least one photo and none succeeded.
            boolean attemptedAny = !photos.isEmpty() || !floorPlans.isEmpty();
            boolean noneSucceeded = photoIds.isEmpty() && floorPlanIds.isEmpty();
            boolean failed = attemptedAny && noneSucceeded;
            listing.setMediaIngestionStatus(failed ? "FAILED" : "COMPLETE");

            listingRepository.save(listing);

            log.info("Media ingestion {} for listing [{}]: {} photos, {} floor plans, {} tour links",
                    listing.getMediaIngestionStatus(),
                    listing.getId(),
                    photoIds.size(), floorPlanIds.size(), tourLinks.size());

            return new MediaIngestionResult(
                    photoIds.size(), floorPlanIds.size(), tourLinks.size(), failed);

        } catch (Exception e) {
            log.error("Media ingestion failed for listing [{}]", listing.getId(), e);
            listing.setMediaIngestionStatus("FAILED");
            listingRepository.save(listing);
            return MediaIngestionResult.failure();
        }
    }

    private List<String> uploadAll(List<JsonNode> mediaList, Listing listing, String typeLabel) {
        List<String> ids = new ArrayList<>(mediaList.size());
        for (int i = 0; i < mediaList.size(); i++) {
            JsonNode m = mediaList.get(i);
            String url = m.path("MediaURL").asText(null);
            if (url == null || url.isBlank()) continue;

            try {
                byte[] bytes = downloadFromMlsGrid(url);
                String filename = String.format("listing_%s_%s_%d.jpg",
                        listing.getMlsId() != null ? listing.getMlsId() : listing.getId(),
                        typeLabel, i + 1);
                String cfId = cloudflareClient.uploadBytes(bytes, filename);
                ids.add(cfId);
            } catch (Exception e) {
                log.warn("Failed to ingest {} #{} for listing [{}] (url={}): {}",
                        typeLabel, i + 1, listing.getId(), url, e.getMessage());
                // continue with the rest — partial success is better than zero
            }

            try {
                Thread.sleep(MS_BETWEEN_DOWNLOADS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Sleep interrupted — aborting upload loop for listing [{}]", listing.getId());
                break;
            }
        }
        return ids;
    }

    private byte[] downloadFromMlsGrid(String url) {
        byte[] bytes = mlsGridMediaDownloadClient.get()
                .uri(URI.create(url))
                .retrieve()
                .body(byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Empty download from " + url);
        }
        return bytes;
    }

    private List<Map<String, String>> extractTourLinks(List<JsonNode> tours) {
        List<Map<String, String>> out = new ArrayList<>();
        for (JsonNode t : tours) {
            String url = t.path("MediaURL").asText(null);
            if (url == null || url.isBlank()) continue;

            Map<String, String> link = new LinkedHashMap<>();
            link.put("provider", classifyProvider(url));
            link.put("url", url);

            String label = t.path("ShortDescription").asText("");
            if (label.isBlank()) label = "Virtual Tour";
            link.put("label", label);

            out.add(link);
        }
        return out;
    }

    private String classifyProvider(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("matterport")) return "matterport";
        if (lower.contains("iguide"))     return "iguide";
        if (lower.contains("youtube") || lower.contains("youtu.be")) return "youtube";
        if (lower.contains("vimeo"))      return "vimeo";
        return "external";
    }

    private String toJsonArray(List<String> items) {
        if (items.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Per-listing summary.
     *
     * Note on the factory naming: the record has a `boolean failed` component,
     * which auto-generates a `failed()` accessor. A static factory method also
     * named `failed()` would shadow the accessor and cause a compile error
     * ("Illegal return type of accessor"). Static factory is named `failure()`
     * to avoid the collision.
     */
    public record MediaIngestionResult(
            int photosUploaded,
            int floorPlansUploaded,
            int tourLinks,
            boolean failed) {
        public static MediaIngestionResult empty()   { return new MediaIngestionResult(0, 0, 0, false); }
        public static MediaIngestionResult failure() { return new MediaIngestionResult(0, 0, 0, true);  }
    }

    /** Backfill batch summary. */
    public record MediaIngestionBatchResult(
            int candidates,
            int succeeded,
            int failed,
            int totalPhotosUploaded) {
        public static MediaIngestionBatchResult empty() { return new MediaIngestionBatchResult(0, 0, 0, 0); }
    }
}
