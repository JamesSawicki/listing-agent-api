package com.jimrealty.listingagent.controller;

import com.jimrealty.listingagent.model.Listing;
import com.jimrealty.listingagent.model.ListingSearchParams;
import com.jimrealty.listingagent.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ListingController — REST endpoints for listing data.
 *
 * Endpoint summary:
 *   GET    /api/listings                → all listings (existing, no auth required)
 *   GET    /api/listings/{id}           → single listing by id (existing, no auth)
 *   GET    /api/listings/search         → filtered/paginated search (NEW, no auth)
 *   POST   /api/listings                → create listing (existing, auth required)
 *   PUT    /api/listings/{id}           → update listing (existing, auth required)
 *   DELETE /api/listings/{id}           → delete listing (existing, auth required)
 *
 * IMPORTANT: /api/listings/search MUST be declared before /api/listings/{id}.
 * Spring MVC evaluates @GetMapping paths in declaration order. If {id} is
 * declared first, a request for /api/listings/search will match it with
 * id="search", which then fails at service layer with a NumberFormatException.
 * Literal paths always beat path variables when declared first.
 */
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    // ------------------------------------------------------------------
    // Search — declare before /{id} to prevent path variable collision
    // ------------------------------------------------------------------

    /**
     * GET /api/listings/search
     *
     * @ModelAttribute binds all matching query parameters to ListingSearchParams
     * in a single step — cleaner than 12 separate @RequestParam annotations.
     *
     * Returns Page<Listing> serialized to JSON:
     * {
     *   "content": [...],        // the listing array
     *   "totalElements": 347,    // total matching results across all pages
     *   "totalPages": 15,        // ceil(totalElements / size)
     *   "number": 0,             // current page (0-indexed)
     *   "size": 24               // page size
     * }
     *
     * Example requests:
     *   /api/listings/search
     *   /api/listings/search?status=Active&minPrice=500000&maxPrice=1000000&minBeds=4
     *   /api/listings/search?waterfront=true&sortBy=price_desc
     *   /api/listings/search?status=Active,Pending,TNAS&city=Wayzata
     *   /api/listings/search?status=Sold&page=1&size=12
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Listing>> searchListings(@ModelAttribute @NonNull ListingSearchParams params) {
        return ResponseEntity.ok(listingService.searchListings(params));
    }

    // ------------------------------------------------------------------
    // Existing endpoints
    // ------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<Listing>> getAllListings() {
        return ResponseEntity.ok(listingService.getAllListings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Listing> getListingById(@PathVariable @NonNull Long id) {
        return ResponseEntity.ok(listingService.getListingById(id));
    }

    @PostMapping
    public ResponseEntity<Listing> createListing(@Valid @RequestBody @NonNull Listing listing) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listingService.createListing(listing));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Listing> updateListing(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody Listing listing) {
        return ResponseEntity.ok(listingService.updateListing(id, listing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable @NonNull Long id) {
        listingService.deleteListing(id);
        return ResponseEntity.noContent().build();
    }
}