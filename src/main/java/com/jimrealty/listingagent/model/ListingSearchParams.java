package com.jimrealty.listingagent.model;

import lombok.Data;

/**
 * ListingSearchParams — DTO for the search endpoint.
 *
 * Spring MVC populates this from the HTTP query string via @ModelAttribute.
 * Null means "no filter on this field."
 *
 * Bounding box (minLat/maxLat/minLng/maxLng) is added in Phase 1 of map search.
 * The map page will send these as it pans and zooms. The filter sidebar search
 * page never sends them — both endpoints co-exist on the same backend method.
 *
 * WGS-84 coordinate ranges for Twin Cities reference:
 *   Lat: ~44.7 (Carver County south) to ~45.2 (Blaine north)
 *   Lng: ~-94.0 (Watertown west) to ~-92.7 (Woodbury east)
 */
@Data
public class ListingSearchParams {

    // ------------------------------------------------------------------
    // Existing filter params
    // ------------------------------------------------------------------

    /**
     * Comma-separated status values: "Active", "Pending", "Sold", "TNAS"
     * Null defaults to Active only in the Specification.
     */
    private String status;

    private Long minPrice;
    private Long maxPrice;

    private Integer minBeds;

    /**
     * Minimum derived bath total.
     * total = (bathsFull * 1.0) + (bathsThreeQuarter * 0.75)
     *       + (bathsHalf * 0.5) + (bathsQuarter * 0.25)
     */
    private Double minBaths;

    private Integer minSqft;

    private String city;
    private String zipCode;
    private String county;
    private String neighborhood;
    private String propertyType;

    private Boolean waterfront;
    private Boolean pool;

    private Integer minGarageStalls;
    private Integer minYearBuilt;
    private Integer maxYearBuilt;

    /**
     * Sort: price_asc | price_desc (default) | newest | dom_asc | sqft_desc
     */
    private String sortBy;

    private Integer page;
    private Integer size;

    // ------------------------------------------------------------------
    // Bounding box — map search (Phase 1)
    //
    // Mapbox GL JS exposes map.getBounds() as a LngLatBounds object:
    //   { _sw: { lat, lng }, _ne: { lat, lng } }
    //
    // The map page will append these to the API call as the viewport moves:
    //   /api/listings/search?minLat=44.85&maxLat=45.05&minLng=-93.65&maxLng=-93.20
    //
    // All four must be present to activate the bbox filter — a partial bbox
    // is ignored. This prevents accidents where one coord arrives but others
    // are missing (e.g., during fast panning).
    // ------------------------------------------------------------------

    /** South boundary — minimum latitude (WGS-84 decimal degrees) */
    private Double minLat;

    /** North boundary — maximum latitude */
    private Double maxLat;

    /** West boundary — minimum longitude (more negative = further west) */
    private Double minLng;

    /** East boundary — maximum longitude */
    private Double maxLng;
}