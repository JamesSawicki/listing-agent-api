package com.jimrealty.listingagent.amenity;

/**
 * A POI extracted from Overpass output: a point with a name and a category.
 * Subtype is the underlying OSM value (e.g. "cafe") preserved for unified categories.
 *
 * Record over class: this is pure data, immutable, no behavior needed.
 */
public record PointOfInterest(
    long osmId,
    String name,
    double latitude,
    double longitude,
    AmenityCategory category,
    String subtype
) {}