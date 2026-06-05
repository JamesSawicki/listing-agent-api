package com.jimrealty.listingagent.amenity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fetches travel-time isochrones from the Mapbox Isochrone API and converts
 * them into JTS Geometry for point-in-polygon analysis.
 *
 * Mapbox endpoint pattern:
 *   GET /isochrone/v1/mapbox/{profile}/{lng},{lat}?contours_minutes=5,10,15&polygons=true
 *
 * Constraints:
 *   - Max 4 contour values per request
 *   - 300 requests/minute (free tier)
 *   - 1 coordinate per request
 *   - profile must be one of: walking, cycling, driving, driving-traffic
 */
@Component
public class MapboxIsochroneClient {

    private static final Logger log = LoggerFactory.getLogger(MapboxIsochroneClient.class);
    private static final int WGS84_SRID = 4326;

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final GeometryFactory geometryFactory;
    private final String baseUrl;
    private final String accessToken;

    public MapboxIsochroneClient(
            RestClient restClient,
            ObjectMapper mapper,
            @Value("${mapbox.isochrone.base-url}") String baseUrl,
            @Value("${mapbox.access-token}") String accessToken) {
        this.restClient = restClient;
        this.mapper = mapper;
        // SRID 4326 = WGS-84. JTS doesn't perform geodesic math; this is metadata.
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), WGS84_SRID);
        this.baseUrl = baseUrl;
        this.accessToken = accessToken;
    }

    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }

    /**
     * Fetch isochrones for a single point and travel profile.
     *
     * @param latitude  WGS-84 latitude (decimal degrees)
     * @param longitude WGS-84 longitude (decimal degrees)
     * @param profile   Mapbox routing profile: "walking", "cycling", or "driving"
     * @param minutes   1-4 contour values, e.g. List.of(5, 10, 15)
     * @return map of minutes -> JTS Geometry (Polygon or MultiPolygon)
     */
    public Map<Integer, Geometry> fetchIsochrones(
            double latitude, double longitude, String profile, List<Integer> minutes) {

        if (!isConfigured()) {
            throw new IsochroneFetchException("Mapbox access token not configured");
        }
        if (minutes == null || minutes.isEmpty() || minutes.size() > 4) {
            throw new IllegalArgumentException(
                "Mapbox supports 1-4 contours per request, got: " + (minutes == null ? 0 : minutes.size()));
        }

        String minutesParam = minutes.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        // Mapbox uses lng,lat ordering in the path
        String coordinates = longitude + "," + latitude;

        String url = UriComponentsBuilder.fromUriString(baseUrl)
            .pathSegment(profile, coordinates)
            .queryParam("contours_minutes", minutesParam)
            .queryParam("polygons", "true")
            .queryParam("access_token", accessToken)
            .encode()
            .toUriString();

        log.debug("Mapbox isochrone request: profile={}, lat={}, lng={}, minutes={}",
            profile, latitude, longitude, minutesParam);

        String responseBody;
        try {
            responseBody = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            throw new IsochroneFetchException(
                "Mapbox isochrone request failed for (" + latitude + "," + longitude
                + ", " + profile + ")", e);
        }

        return parseFeatureCollection(responseBody);
    }

    private Map<Integer, Geometry> parseFeatureCollection(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) {
                throw new IsochroneFetchException("No features array in Mapbox response");
            }

            // LinkedHashMap preserves the order Mapbox returns (typically largest contour first)
            Map<Integer, Geometry> result = new LinkedHashMap<>();
            for (JsonNode feature : features) {
                int contour = feature.path("properties").path("contour").asInt();
                Geometry geom = parseGeometry(feature.path("geometry"));
                if (geom != null) {
                    result.put(contour, geom);
                }
            }
            return result;
        } catch (IsochroneFetchException e) {
            throw e;
        } catch (Exception e) {
            throw new IsochroneFetchException("Failed to parse Mapbox response", e);
        }
    }

    private Geometry parseGeometry(JsonNode geometryNode) {
        String type = geometryNode.path("type").asText();
        JsonNode coords = geometryNode.path("coordinates");

        if ("Polygon".equals(type)) {
            return parsePolygon(coords);
        } else if ("MultiPolygon".equals(type)) {
            // MultiPolygon coordinates: array of polygon coordinate arrays
            Polygon[] polygons = new Polygon[coords.size()];
            for (int i = 0; i < coords.size(); i++) {
                polygons[i] = parsePolygon(coords.get(i));
            }
            return geometryFactory.createMultiPolygon(polygons);
        }
        log.warn("Unknown Mapbox geometry type: {}", type);
        return null;
    }

    private Polygon parsePolygon(JsonNode polygonCoords) {
        // First ring is the outer shell; remaining rings are holes.
        LinearRing shell = parseLinearRing(polygonCoords.get(0));
        LinearRing[] holes = new LinearRing[polygonCoords.size() - 1];
        for (int i = 1; i < polygonCoords.size(); i++) {
            holes[i - 1] = parseLinearRing(polygonCoords.get(i));
        }
        return geometryFactory.createPolygon(shell, holes);
    }

    private LinearRing parseLinearRing(JsonNode ringCoords) {
        Coordinate[] coords = new Coordinate[ringCoords.size()];
        for (int i = 0; i < ringCoords.size(); i++) {
            JsonNode pt = ringCoords.get(i);
            // GeoJSON: [lng, lat] -> JTS Coordinate(x=lng, y=lat)
            double lng = pt.get(0).asDouble();
            double lat = pt.get(1).asDouble();
            coords[i] = new Coordinate(lng, lat);
        }
        return geometryFactory.createLinearRing(coords);
    }
}