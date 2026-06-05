package com.jimrealty.listingagent.amenity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * The score for a single (band, category) cell.
 * subtypes is populated only for unified categories like RESTAURANTS;
 * null/omitted for atomic categories like GROCERY.
 *
 * @JsonInclude(NON_NULL) prevents Jackson from writing "subtypes": null for
 * single-tag categories — keeps the stored JSON compact.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryScore(
    int count,
    String nearestName,
    Integer nearestMeters,
    Map<String, Integer> subtypes
) {
    public static CategoryScore empty() {
        return new CategoryScore(0, null, null, null);
    }
}