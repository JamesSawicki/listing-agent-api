package com.jimrealty.listingagent.amenity;

import java.util.List;
import java.util.Map;

/**
 * The four logical categories we score. Each category maps to one or more
 * OpenStreetMap tag combinations queried via Overpass.
 *
 * RESTAURANTS is intentionally unified — buyers think "food options nearby,"
 * not "5 sit-downs and 3 cafes." Subtype counts are preserved for display.
 */
public enum AmenityCategory {

    GROCERY("grocery", List.of(
        OsmTag.of("shop", "supermarket"),
        OsmTag.of("shop", "grocery")
    )),

    PHARMACY("pharmacy", List.of(
        OsmTag.of("amenity", "pharmacy")
    )),

    HARDWARE("hardware", List.of(
        OsmTag.of("shop", "hardware"),
        OsmTag.of("shop", "doityourself")
    )),

    RESTAURANTS("restaurants", List.of(
        OsmTag.of("amenity", "restaurant"),
        OsmTag.of("amenity", "cafe"),
        OsmTag.of("amenity", "fast_food"),
        OsmTag.of("amenity", "bar"),
        OsmTag.of("amenity", "pub"),
        OsmTag.of("amenity", "ice_cream")
    ));

    private final String jsonKey;
    private final List<OsmTag> osmTags;

    AmenityCategory(String jsonKey, List<OsmTag> osmTags) {
        this.jsonKey = jsonKey;
        this.osmTags = osmTags;
    }

    public String jsonKey() { return jsonKey; }
    public List<OsmTag> osmTags() { return osmTags; }

    /**
     * Classify an OSM element's tag map into a category. Returns null
     * if the element doesn't match any category we track.
     */
    public static AmenityCategory classify(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        for (AmenityCategory cat : values()) {
            for (OsmTag t : cat.osmTags) {
                if (t.value().equals(tags.get(t.key()))) {
                    return cat;
                }
            }
        }
        return null;
    }

    /** Return the OSM subtype value (e.g. "cafe" within RESTAURANTS) or null. */
    public String subtypeOf(Map<String, String> tags) {
        if (tags == null) return null;
        for (OsmTag t : osmTags) {
            if (t.value().equals(tags.get(t.key()))) {
                return t.value();
            }
        }
        return null;
    }

    public record OsmTag(String key, String value) {
        public static OsmTag of(String k, String v) { return new OsmTag(k, v); }
    }
}