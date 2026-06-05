package com.jimrealty.listingagent.amenity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The full scorecard for one listing. Shape (serialized):
 * {
 *   "generatedAt": "2026-06-04T15:23:00Z",
 *   "scores": {
 *     "walk_5min":  { "grocery": {...}, "pharmacy": {...}, ... },
 *     "walk_10min": { ... },
 *     ...
 *   }
 * }
 *
 * Wrapping in an outer object (vs. a raw band->category map) lets us version
 * the format later by adding a "schemaVersion" field without breaking parsers.
 */
public record AmenityScores(
    Instant generatedAt,
    Map<String, Map<String, CategoryScore>> scores
) {

    public static AmenityScores empty() {
        return new AmenityScores(Instant.now(), new LinkedHashMap<>());
    }

    public String toJson(ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AmenityScores", e);
        }
    }

    public static AmenityScores fromJson(String json, ObjectMapper mapper) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readValue(json, AmenityScores.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize AmenityScores", e);
        }
    }
}