package com.jimrealty.listingagent.media;

/**
 * Classifies a RESO Media record by its MediaCategory field.
 * Maps the upstream string values to our internal handling buckets.
 *
 * RESO MediaCategory values commonly seen on NorthstarMLS Property records:
 *   - "Photo"        → standard listing photos (downloaded + uploaded to CF Images)
 *   - "FloorPlan"    → floor plan diagrams (downloaded + uploaded; rendered separately)
 *   - "VirtualTour"  → external tour URLs (NOT downloaded; URL stored as CTA link)
 *   - "Video"        → video files (currently ignored)
 *   - "Document"     → supporting documents (currently ignored)
 *
 * Per Jim's June 2026 product decision: photos + floor plans + virtual tours.
 */
public enum MediaCategory {
    PHOTO,
    FLOOR_PLAN,
    VIRTUAL_TOUR,
    OTHER;

    /**
     * Classifies a raw RESO MediaCategory string into our enum.
     * Whitespace-insensitive, case-insensitive. Returns OTHER for any value
     * we don't currently handle (Video, Document, etc.).
     */
    public static MediaCategory fromResoString(String raw) {
        if (raw == null || raw.isBlank()) return PHOTO;  // unspecified = photo (MLS default)
        return switch (raw.replace(" ", "").toLowerCase()) {
            case "photo", "photograph"                          -> PHOTO;
            case "floorplan"                                    -> FLOOR_PLAN;
            case "virtualtour", "virtualtoururl", "video360"    -> VIRTUAL_TOUR;
            default                                             -> OTHER;
        };
    }
}
