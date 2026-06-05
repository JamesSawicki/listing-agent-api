package com.jimrealty.listingagent.controller;

import com.jimrealty.listingagent.amenity.AmenityScoreService;
import com.jimrealty.listingagent.repository.ListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin endpoints for managing amenity scores.
 *
 * Both endpoints require auth via SecurityConfig's catch-all rule
 * (anything not explicitly public requires HTTP Basic Auth).
 */
@RestController
@RequestMapping("/api/scoring")
public class AmenityScoringController {

    private static final Logger log = LoggerFactory.getLogger(AmenityScoringController.class);

    private final AmenityScoreService scoreService;
    private final ListingRepository listingRepository;

    public AmenityScoringController(
            AmenityScoreService scoreService,
            ListingRepository listingRepository) {
        this.scoreService = scoreService;
        this.listingRepository = listingRepository;
    }

    /**
     * Synchronously recompute scores for one listing.
     * Returns the updated listing (with new amenityScores JSON).
     * 5-15 seconds typical response time.
     */
    @PostMapping("/listings/{id}")
    public ResponseEntity<?> recomputeOne(@PathVariable Long id) {
        log.info("Manual recompute requested for listing {}", id);
        scoreService.scoreAndSave(id);
        return listingRepository.findById(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Asynchronously trigger the full batch refresh of all in-scope listings.
     * Returns 202 Accepted immediately. Check application logs for progress.
     * Same code path as the monthly cron job.
     */
    @PostMapping("/refresh-all")
    public ResponseEntity<Map<String, Object>> refreshAll() {
        log.info("Manual refresh-all-scores triggered");
        scoreService.refreshAllAsync();
        return ResponseEntity.accepted().body(Map.of(
            "status", "queued",
            "message", "Batch refresh started. Watch application logs for progress."
        ));
    }
}