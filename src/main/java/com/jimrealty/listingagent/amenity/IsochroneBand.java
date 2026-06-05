package com.jimrealty.listingagent.amenity;

/**
 * The 9 isochrone bands we compute per listing: 3 travel modes × 3 time bands.
 * jsonKey is what gets written to amenityScores JSON.
 * mapboxProfile is the Mapbox routing profile name (walking|cycling|driving).
 */
public enum IsochroneBand {
    WALK_5  ("walk_5min",   "walking", 5),
    WALK_10 ("walk_10min",  "walking", 10),
    WALK_15 ("walk_15min",  "walking", 15),
    BIKE_5  ("bike_5min",   "cycling", 5),
    BIKE_10 ("bike_10min",  "cycling", 10),
    BIKE_15 ("bike_15min",  "cycling", 15),
    DRIVE_5 ("drive_5min",  "driving", 5),
    DRIVE_10("drive_10min", "driving", 10),
    DRIVE_15("drive_15min", "driving", 15);

    private final String jsonKey;
    private final String mapboxProfile;
    private final int minutes;

    IsochroneBand(String jsonKey, String mapboxProfile, int minutes) {
        this.jsonKey = jsonKey;
        this.mapboxProfile = mapboxProfile;
        this.minutes = minutes;
    }

    public String jsonKey()        { return jsonKey; }
    public String mapboxProfile()  { return mapboxProfile; }
    public int minutes()           { return minutes; }
}