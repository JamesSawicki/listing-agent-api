package com.jimrealty.listingagent.amenity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Queries the OpenStreetMap Overpass API for points of interest within a bounding box.
 *
 * Overpass QL bbox order: (south, west, north, east) = (minLat, minLng, maxLat, maxLng).
 * This is DIFFERENT from GeoJSON and Mapbox which are lng-first. Don't confuse them.
 *
 * Public instance etiquette: optimize queries, limit simultaneous requests, retry on 429/504
 * with backoff. Retry logic lives in the service layer, not here.
 */
@Component
public class OverpassClient {

    private static final Logger log = LoggerFactory.getLogger(OverpassClient.class);

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final int timeoutSeconds;

    public OverpassClient(
            RestClient restClient,
            ObjectMapper mapper,
            @Value("${overpass.base-url}") String baseUrl,
            @Value("${overpass.timeout-seconds}") int timeoutSeconds) {
        this.restClient = restClient;
        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Fetch all POIs within the bounding box matching any AmenityCategory tag.
     *
     * @param minLat south edge (decimal degrees)
     * @param minLng west edge  (decimal degrees)
     * @param maxLat north edge (decimal degrees)
     * @param maxLng east edge  (decimal degrees)
     * @return POIs, possibly empty, never null
     */
    public List<PointOfInterest> fetchPois(double minLat, double minLng, double maxLat, double maxLng) {
        // Overpass bbox: south, west, north, east
        String bbox = minLat + "," + minLng + "," + maxLat + "," + maxLng;
        String query = buildQuery(bbox);
        String formBody = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        log.debug("Overpass query for bbox {}", bbox);

        String responseBody;
        try {
            responseBody = restClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formBody)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            throw new OverpassFetchException("Overpass query failed for bbox " + bbox, e);
        }

        return parseResponse(responseBody);
    }

    /**
     * Construct an Overpass QL query that unions one filter per (key, value) pair
     * across all AmenityCategory.osmTags. nwr = node, way, relation (all three).
     * "out center;" emits centroid for ways/relations instead of full geometry.
     */
    String buildQuery(String bbox) {
        StringBuilder sb = new StringBuilder();
        sb.append("[out:json][timeout:").append(timeoutSeconds).append("];\n");
        sb.append("(\n");
        for (AmenityCategory cat : AmenityCategory.values()) {
            for (AmenityCategory.OsmTag tag : cat.osmTags()) {
                sb.append("  nwr[\"")
                  .append(tag.key())
                  .append("\"=\"")
                  .append(tag.value())
                  .append("\"](")
                  .append(bbox)
                  .append(");\n");
            }
        }
        sb.append(");\n");
        sb.append("out center;");
        return sb.toString();
    }

    private List<PointOfInterest> parseResponse(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            JsonNode elements = root.path("elements");
            if (!elements.isArray()) {
                log.warn("Overpass response has no elements array");
                return Collections.emptyList();
            }

            List<PointOfInterest> pois = new ArrayList<>(elements.size());
            for (JsonNode element : elements) {
                PointOfInterest poi = parseElement(element);
                if (poi != null) pois.add(poi);
            }
            log.debug("Overpass returned {} elements, parsed {} POIs", elements.size(), pois.size());
            return pois;
        } catch (Exception e) {
            throw new OverpassFetchException("Failed to parse Overpass response", e);
        }
    }

    private PointOfInterest parseElement(JsonNode element) {
        String type = element.path("type").asText();
        long id = element.path("id").asLong();

        double lat;
        double lng;
        if ("node".equals(type)) {
            lat = element.path("lat").asDouble();
            lng = element.path("lon").asDouble();
        } else {
            // way or relation: use the center coordinate emitted by "out center;"
            JsonNode center = element.path("center");
            if (center.isMissingNode()) return null;
            lat = center.path("lat").asDouble();
            lng = center.path("lon").asDouble();
        }

        JsonNode tagsNode = element.path("tags");
        if (tagsNode.isMissingNode() || tagsNode.isEmpty()) return null;

        Map<String, String> tags = jsonNodeToTagMap(tagsNode);

        AmenityCategory category = AmenityCategory.classify(tags);
        if (category == null) {
            // Element matched our query but no category — shouldn't happen, but defensive
            return null;
        }
        String subtype = category.subtypeOf(tags);

        String name = tags.getOrDefault("name", "Unnamed");
        if (name.isBlank()) name = "Unnamed";

        return new PointOfInterest(id, name, lat, lng, category, subtype);
    }

    /**
     * Convert a Jackson ObjectNode of tag key/value pairs into a plain Map<String,String>.
     * Using the explicit Iterator avoids reliance on Jackson API methods that have
     * shifted across versions (fields() / properties()).
     */
    @SuppressWarnings("deprecation")
    private Map<String, String> jsonNodeToTagMap(JsonNode tagsNode) {
        Map<String, String> tags = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = tagsNode.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            tags.put(entry.getKey(), entry.getValue().asText());
        }
        return tags;
    }
}