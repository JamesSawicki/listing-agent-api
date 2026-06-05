package com.jimrealty.listingagent.amenity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimrealty.listingagent.model.Listing;
import com.jimrealty.listingagent.repository.ListingRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orchestrates the amenity-scoring pipeline:
 *   coordinates -> Mapbox isochrones -> Overpass POIs ->
 *   JTS point-in-polygon -> Haversine distance -> aggregated scorecard.
 *
 * Public methods:
 *   - computeScores(lat, lng)            : pure function, no DB
 *   - scoreAndSave(id)                   : synchronous, used by admin endpoints
 *   - scoreAndSaveAsync(id)              : fire-and-forget for listing creation
 *   - monthlyRefresh()                   : scheduled batch (cron from properties)
 */
@Service
public class AmenityScoreService {

    private static final Logger log = LoggerFactory.getLogger(AmenityScoreService.class);

    // === Constants ===
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    /** Politeness sleep between listings in monthly batch (Overpass etiquette). */
    private static final long OVERPASS_POLITE_SLEEP_MS = 1100;
    /** Retry budget for Overpass transient failures (429/504/network). */
    private static final int OVERPASS_MAX_RETRIES = 2;
    private static final int WGS84_SRID = 4326;

    // === Dependencies ===
    private final MapboxIsochroneClient mapboxClient;
    private final OverpassClient overpassClient;
    private final ListingRepository listingRepository;
    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory;

    // === Config ===
    private final boolean enabled;
    private final double bboxBufferMeters;
    private final List<String> refreshStatuses;

    public AmenityScoreService(
            MapboxIsochroneClient mapboxClient,
            OverpassClient overpassClient,
            ListingRepository listingRepository,
            ObjectMapper objectMapper,
            @Value("${amenity.scoring.enabled}") boolean enabled,
            @Value("${amenity.scoring.bbox-buffer-meters}") double bboxBufferMeters,
            @Value("${amenity.scoring.refresh-statuses}") List<String> refreshStatuses) {
        this.mapboxClient = mapboxClient;
        this.overpassClient = overpassClient;
        this.listingRepository = listingRepository;
        this.objectMapper = objectMapper;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), WGS84_SRID);
        this.enabled = enabled;
        this.bboxBufferMeters = bboxBufferMeters;
        this.refreshStatuses = refreshStatuses;
    }

    // ====================================================================
    //  PUBLIC API
    // ====================================================================

    /**
     * Pure compute. Given coordinates, runs the full pipeline and returns
     * the scorecard. No database I/O.
     */
    public AmenityScores computeScores(double latitude, double longitude) {
        // Step 1: fetch all 9 isochrones (3 Mapbox calls, one per profile)
        Map<IsochroneBand, Geometry> isochrones = fetchAllIsochrones(latitude, longitude);
        if (isochrones.isEmpty()) {
            log.warn("No isochrones returned for ({}, {})", latitude, longitude);
            return AmenityScores.empty();
        }

        // Step 2: derive a buffered bounding box from the union of all isochrones
        Envelope envelope = computeBufferedEnvelope(isochrones.values(), latitude);

        // Step 3: one Overpass call for all POIs in that envelope
        List<PointOfInterest> pois = fetchPoisWithRetry(
            envelope.getMinY(), envelope.getMinX(),
            envelope.getMaxY(), envelope.getMaxX());
        log.debug("Fetched {} POIs in envelope around ({}, {})", pois.size(), latitude, longitude);

        // Step 4: classify each POI against each band, aggregate per category
        Map<String, Map<String, CategoryScore>> scoresByBand = new LinkedHashMap<>();

        for (IsochroneBand band : IsochroneBand.values()) {
            Geometry bandGeom = isochrones.get(band);
            if (bandGeom == null) continue;

            List<PointOfInterest> poisInBand = filterPoisInside(pois, bandGeom);
            if (poisInBand.isEmpty()) continue;

            Map<String, CategoryScore> bandScores = new LinkedHashMap<>();
            for (AmenityCategory cat : AmenityCategory.values()) {
                List<PointOfInterest> inCat = poisInBand.stream()
                    .filter(p -> p.category() == cat)
                    .toList();
                if (inCat.isEmpty()) continue;
                bandScores.put(cat.jsonKey(), aggregate(inCat, cat, latitude, longitude));
            }
            if (!bandScores.isEmpty()) {
                scoresByBand.put(band.jsonKey(), bandScores);
            }
        }

        return new AmenityScores(Instant.now(), scoresByBand);
    }

    /**
     * Load listing, compute scores, persist. Synchronous.
     * Called by the admin recompute endpoint and by the monthly batch.
     */
    public void scoreAndSave(Long listingId) {
        Optional<Listing> opt = listingRepository.findById(listingId);
        if (opt.isEmpty()) {
            log.warn("Listing {} not found, skipping scoring", listingId);
            return;
        }
        Listing listing = opt.get();
        Double lat = listing.getLatitude();
        Double lng = listing.getLongitude();
        if (lat == null || lng == null) {
            log.warn("Listing {} has no coordinates, skipping scoring", listingId);
            return;
        }
        if (!isValidCoordinate(lat, lng)) {
            log.warn("Listing {} has invalid coordinates ({}, {}), skipping", listingId, lat, lng);
            return;
        }

        log.info("Scoring listing {} at ({}, {})", listingId, lat, lng);

        AmenityScores scores = computeScores(lat, lng);
        listing.setAmenityScores(scores.toJson(objectMapper));
        listingRepository.save(listing);

        log.info("Saved amenity scores for listing {}: {} bands populated",
            listingId, scores.scores().size());
    }

    /**
     * Async wrapper for listing creation. Caller fires and forgets.
     * Exceptions are caught and logged so they don't get swallowed by Spring's async machinery.
     */
    @Async
    public CompletableFuture<Void> scoreAndSaveAsync(Long listingId) {
        try {
            scoreAndSave(listingId);
        } catch (Exception e) {
            log.error("Async scoring failed for listing {}: {}", listingId, e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }
/**
     * Scheduled monthly batch. Runs on the cron set in application.properties.
     * Public entry point #1 for the refresh batch.
     */
    @Scheduled(cron = "${amenity.scoring.refresh-cron}")
    public void monthlyRefresh() {
        log.info("Cron-triggered monthly refresh starting");
        runBatchRefresh();
    }

    /**
     * Manually triggered batch refresh, runs on the async executor.
     * Public entry point #2 for the refresh batch — same work, different trigger.
     * Returns immediately; check logs for progress.
     */
    @Async
    public CompletableFuture<Void> refreshAllAsync() {
        log.info("Manually-triggered refresh starting");
        runBatchRefresh();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * The actual batch refresh work. Private so it doesn't get called externally —
     * external callers must go through monthlyRefresh() or refreshAllAsync()
     * to ensure proper @Scheduled / @Async proxy behavior.
     */
    private void runBatchRefresh() {
        if (!enabled || !mapboxClient.isConfigured()) {
            log.info("Amenity scoring disabled or Mapbox token missing, skipping refresh");
            return;
        }

        List<Listing> targets = listingRepository.findByStatusIn(refreshStatuses);
        log.info("Starting amenity refresh: {} listings, statuses {}",
            targets.size(), refreshStatuses);

        long startMs = System.currentTimeMillis();
        int success = 0, failed = 0, skipped = 0;

        for (Listing listing : targets) {
            if (listing.getLatitude() == null || listing.getLongitude() == null) {
                skipped++;
                continue;
            }
            try {
                scoreAndSave(listing.getId());
                success++;
            } catch (Exception e) {
                log.error("Failed to score listing {}: {}", listing.getId(), e.getMessage());
                failed++;
            }
            try {
                Thread.sleep(OVERPASS_POLITE_SLEEP_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Refresh interrupted after listing {}", listing.getId());
                break;
            }
        }

        long elapsedSec = (System.currentTimeMillis() - startMs) / 1000;
        log.info("Amenity refresh complete: {} success, {} failed, {} skipped in {}s",
            success, failed, skipped, elapsedSec);
    }

    // ====================================================================
    //  PRIVATE HELPERS
    // ====================================================================

    /**
     * Group bands by Mapbox profile, then make one Mapbox call per profile
     * (each returning all 3 nested contours). 3 API calls total for 9 bands.
     */
    private Map<IsochroneBand, Geometry> fetchAllIsochrones(double lat, double lng) {
        Map<String, List<IsochroneBand>> bandsByProfile = Arrays.stream(IsochroneBand.values())
            .collect(Collectors.groupingBy(IsochroneBand::mapboxProfile));

        Map<IsochroneBand, Geometry> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<IsochroneBand>> entry : bandsByProfile.entrySet()) {
            String profile = entry.getKey();
            List<IsochroneBand> bands = entry.getValue();
            List<Integer> minutes = bands.stream().map(IsochroneBand::minutes).toList();

            try {
                Map<Integer, Geometry> profileGeoms = mapboxClient.fetchIsochrones(lat, lng, profile, minutes);
                for (IsochroneBand band : bands) {
                    Geometry g = profileGeoms.get(band.minutes());
                    if (g != null) result.put(band, g);
                }
            } catch (IsochroneFetchException e) {
                log.error("Mapbox isochrone fetch failed for profile {} at ({}, {}): {}",
                    profile, lat, lng, e.getMessage());
                // continue with whatever profiles succeeded
            }
        }
        return result;
    }

    /**
     * Compute axis-aligned envelope containing all isochrones, then buffer in meters.
     * Buffer conversion uses cos(lat) correction so the longitude expansion matches
     * the latitude expansion in real-world distance.
     */
    private Envelope computeBufferedEnvelope(Collection<Geometry> geometries, double centerLat) {
        Envelope env = new Envelope();
        for (Geometry g : geometries) {
            env.expandToInclude(g.getEnvelopeInternal());
        }
        double latBufferDeg = bboxBufferMeters / 111_000.0;
        double lngBufferDeg = bboxBufferMeters / (111_000.0 * Math.cos(Math.toRadians(centerLat)));
        env.expandBy(lngBufferDeg, latBufferDeg); // expandBy(deltaX=lng, deltaY=lat)
        return env;
    }

    /**
     * Retry Overpass with backoff. Retries on any OverpassFetchException
     * regardless of underlying cause — at this layer we can't distinguish
     * cleanly between 429, 504, and network errors without parsing the wrapped cause.
     */
    private List<PointOfInterest> fetchPoisWithRetry(double minLat, double minLng, double maxLat, double maxLng) {
        int[] backoffSeconds = {2, 5};

        for (int attempt = 0; ; attempt++) {
            try {
                return overpassClient.fetchPois(minLat, minLng, maxLat, maxLng);
            } catch (OverpassFetchException e) {
                if (attempt >= OVERPASS_MAX_RETRIES) throw e;  // out of retries, propagate
                int sleep = backoffSeconds[attempt];
                log.warn("Overpass attempt {} failed: {}. Retrying in {}s",
                    attempt + 1, e.getMessage(), sleep);
                try {
                    Thread.sleep(sleep * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    /**
     * Filter POIs whose JTS Point lies inside the given Geometry.
     * For 500 POIs and 9 polygons, that's 4,500 contains() calls per listing —
     * microseconds total. No spatial index needed at this scale.
     */
    private List<PointOfInterest> filterPoisInside(List<PointOfInterest> pois, Geometry bandGeom) {
        return pois.stream()
            .filter(p -> {
                // JTS: x = lng, y = lat
                Point point = geometryFactory.createPoint(new Coordinate(p.longitude(), p.latitude()));
                return bandGeom.contains(point);
            })
            .toList();
    }

    /**
     * Aggregate a single (band, category) cell: count, nearest by Haversine, subtype breakdown.
     */
    @SuppressWarnings("null")
    private CategoryScore aggregate(List<PointOfInterest> inCategory, AmenityCategory cat,
                                     double listingLat, double listingLng) {
        int count = inCategory.size();

        PointOfInterest nearest = null;
        double nearestMeters = Double.POSITIVE_INFINITY;
        Map<String, Integer> subtypeCounts = new LinkedHashMap<>();

        for (PointOfInterest poi : inCategory) {
            double dist = haversineMeters(listingLat, listingLng, poi.latitude(), poi.longitude());
            if (dist < nearestMeters) {
                nearest = poi;
                nearestMeters = dist;
            }
            if (poi.subtype() != null) {
                subtypeCounts.merge(poi.subtype(), 1, Integer::sum);
            }
        }

        // Only emit subtypes for categories with multiple tag variants.
        // Single-tag categories (grocery, pharmacy, hardware) get null subtypes,
        // which JsonInclude(NON_NULL) on CategoryScore omits from JSON output.
        Map<String, Integer> subtypes = (cat.osmTags().size() > 1 && !subtypeCounts.isEmpty())
            ? subtypeCounts
            : null;

        return new CategoryScore(
            count,
            nearest != null ? nearest.name() : null,
            nearest != null ? (int) Math.round(nearestMeters) : null,
            subtypes
        );
    }

    private static boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180
            && !(lat == 0.0 && lng == 0.0);  // (0,0) is almost certainly bad data, not a real listing
    }

    /**
     * Great-circle distance via Haversine formula.
     * Earth radius = mean radius (IUGG). Returns meters.
     */
    private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}