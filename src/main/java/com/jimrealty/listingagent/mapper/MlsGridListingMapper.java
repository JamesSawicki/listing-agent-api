package com.jimrealty.listingagent.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimrealty.listingagent.model.Listing;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Maps a single RESO Property record (as a Jackson JsonNode) from the MLS Grid
 * API to our Listing entity.
 *
 * Design decisions:
 *   - Input is JsonNode, not a typed DTO. A typed DTO would require ~80 annotated
 *     fields mirroring RESO exactly. JsonNode is null-safe, flexible, and
 *     trivially handles sparse records (not every field appears on every property).
 *   - All helper methods return null for missing/null nodes rather than default
 *     values. This preserves the distinction between "not provided" and "zero".
 *   - Array fields (Appliances, Heating, etc.) are serialized to JSON strings
 *     for storage. The frontend already expects this format.
 *   - This class has zero I/O — it doesn't touch the database or the HTTP layer.
 *     It is a pure function: JsonNode in, Listing out. Easy to unit test.
 *
 * Field naming convention:
 *   RESO standard fields use their exact Data Dictionary name (ListPrice, BedroomsTotal).
 *   NorthstarMLS local fields use the NST_ prefix (NST_Fuel, NST_AmenitiesUnit).
 */
@Component
public class MlsGridListingMapper {

    private final ObjectMapper objectMapper;

    public MlsGridListingMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    /**
     * Maps a single RESO Property JsonNode to a Listing entity.
     *
     * The caller (MlsGridIngestionService) is responsible for:
     *   1. Checking mlgCanView — if false, delete the record, don't call this method.
     *   2. Upsert logic — look up existing Listing by listingKey before saving
     *      so we update in place rather than creating a duplicate.
     */
    public Listing toListing(JsonNode p) {
        return Listing.builder()

            // --- Sync metadata ---------------------------------------------------
            .listingKey(text(p, "ListingKey"))
            .mlsId(text(p, "ListingId"))         // human-readable MLS# for display
            .mlgCanView(bool(p, "MlgCanView"))
            .modificationTimestamp(instant(p, "ModificationTimestamp"))
            .originalEntryTimestamp(instant(p, "OriginalEntryTimestamp"))
            .mlgCanUse(toJsonString(p, "MlgCanUse"))

            // --- Identity & status -----------------------------------------------
            .status(text(p, "StandardStatus"))
            .listDate(localDate(p, "ListingContractDate"))
            .closeDate(localDate(p, "CloseDate"))
            .offMarketDate(localDate(p, "OffMarketDate"))
            .listPrice(longVal(p, "ListPrice"))
            .originalListPrice(longVal(p, "OriginalListPrice"))
            .closePrice(longVal(p, "ClosePrice"))
            .contingency(text(p, "Contingency"))

            // --- Location --------------------------------------------------------
            .address(assembleAddress(p))
            .city(text(p, "City"))
            .postalCity(text(p, "PostalCity"))
            .zipCode(text(p, "PostalCode"))
            .county(text(p, "CountyOrParish"))
            .neighborhood(text(p, "SubdivisionName"))
            .complexSubdiv(text(p, "SubdivisionName"))  // same source, different display context
            .latitude(doubleVal(p, "Latitude"))
            .longitude(doubleVal(p, "Longitude"))
            .parcelNumber(text(p, "ParcelNumber"))
            .zoningDescription(text(p, "ZoningDescription"))

            // --- Property basics -------------------------------------------------
            .propertyType(text(p, "PropertySubType"))   // "Single Family Residence" is more useful than "Residential"
            .style(firstArrayElement(p, "Levels"))       // "Split Entry (Bi-Level)", "Two Story", etc.
            .yearBuilt(intVal(p, "YearBuilt"))
            .newConstruction(bool(p, "NewConstructionYN"))
            .beds(intVal(p, "BedroomsTotal"))
            .bathsFull(intVal(p, "BathroomsFull"))
            .bathsThreeQuarter(intVal(p, "BathroomsThreeQuarter"))
            .bathsHalf(intVal(p, "BathroomsHalf"))
            .bathsQuarter(intVal(p, "BathroomsOneQuarter"))  // RESO name differs from our field name
            .sqftAboveGrade(intVal(p, "AboveGradeFinishedArea"))
            .sqftBelowGrade(intVal(p, "BelowGradeFinishedArea"))
            .sqftTotal(intVal(p, "LivingArea"))
            .lotSqft(calculateLotSqft(p))
            .lotDimensions(text(p, "LotSizeDimensions"))
            .garageStalls(intVal(p, "GarageSpaces"))

            // --- Features --------------------------------------------------------
            .appliances(toJsonString(p, "Appliances"))
            .basement(toJsonString(p, "Basement"))
            .heating(toJsonString(p, "Heating"))
            .airConditioning(toJsonString(p, "Cooling"))
            .fuelType(text(p, "NST_Fuel"))
            .fireplaces(intVal(p, "FireplacesTotal"))
            .constructionMaterials(toJsonString(p, "ConstructionMaterials"))
            .roof(toJsonString(p, "Roof"))
            .sewer(toJsonString(p, "Sewer"))
            .waterSource(toJsonString(p, "WaterSource"))
            .parkingFeatures(toJsonString(p, "ParkingFeatures"))
            .diningRoomFeatures(text(p, "NST_DiningRoomDescription"))
            .amenities(text(p, "NST_AmenitiesUnit"))
            .laundryFeatures(text(p, "NST_SpecialSearch"))

            // --- HOA -------------------------------------------------------------
            .associationFee(longVal(p, "AssociationFee"))
            .associationFeeFrequency(text(p, "AssociationFeeFrequency"))

            // --- Financial -------------------------------------------------------
            .taxAmount(longVal(p, "TaxAnnualAmount"))
            .taxYear(intVal(p, "TaxYear"))
            .taxWithAssessments(text(p, "NST_TaxWithAssessments"))

            // --- Schools ---------------------------------------------------------
            .schoolDistrict(text(p, "HighSchoolDistrict"))
            .schoolDistrictNumber(text(p, "NST_SchoolDistrictNumber"))

            // --- Agent / office --------------------------------------------------
            // Note: ListOfficeKey is the system key (e.g. "NST19846").
            // Full office name/phone require expanding the Office resource separately.
            .listOfficeMlsId(text(p, "ListOfficeKey"))

            // --- Display ---------------------------------------------------------
            .publicRemarks(text(p, "PublicRemarks"))
            .photosCount(intVal(p, "PhotosCount"))
            .directions(text(p, "Directions"))
            .primaryImageUrl(extractPrimaryImageUrl(p))
            .mediaJson(serializeMedia(p))

            // --- Rooms -----------------------------------------------------------
            .roomInfo(mapRooms(p))

            .build();
    }

    // -------------------------------------------------------------------------
    // Complex mapping methods
    // -------------------------------------------------------------------------

    /**
     * Assembles the street address from its component parts.
     *
     * Why components instead of UnparsedAddress? NorthstarMLS via MLS Grid
     * does not consistently provide UnparsedAddress. The RESO spec always
     * includes StreetNumber and StreetName for residential listings.
     *
     * Handles optional: StreetDirPrefix (N, S, E, W before street name),
     * StreetDirSuffix (directional after name), UnitNumber (#101 etc.)
     */
    private String assembleAddress(JsonNode p) {
        StringBuilder sb = new StringBuilder();

        appendIfPresent(sb, text(p, "StreetNumber"));
        appendIfPresent(sb, text(p, "StreetDirPrefix"));
        appendIfPresent(sb, text(p, "StreetName"));
        appendIfPresent(sb, text(p, "StreetSuffix"));
        appendIfPresent(sb, text(p, "StreetDirSuffix"));

        String unit = text(p, "UnitNumber");
        if (unit != null) {
            sb.append(sb.length() > 0 ? " #" : "#").append(unit);
        }

        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private void appendIfPresent(StringBuilder sb, String val) {
        if (val != null && !val.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(val);
        }
    }

    /**
     * Converts lot size to square feet.
     *
     * MLS Grid returns LotSizeArea + LotSizeUnits rather than a guaranteed
     * LotSizeSquareFeet. NorthstarMLS commonly uses Acres for rural properties.
     * If LotSizeSquareFeet is present, use it directly.
     *
     * Conversion: 1 Acre = 43,560 sq ft (exact, by US legal definition).
     */
    private Long calculateLotSqft(JsonNode p) {
        // Check for a direct square footage field first
        JsonNode sqftDirect = p.path("LotSizeSquareFeet");
        if (!sqftDirect.isMissingNode() && !sqftDirect.isNull()) {
            return sqftDirect.asLong();
        }

        JsonNode areaNode = p.path("LotSizeArea");
        if (areaNode.isMissingNode() || areaNode.isNull()) return null;

        double area = areaNode.asDouble();
        String units = p.path("LotSizeUnits").asText("").toLowerCase().trim();

        return switch (units) {
            case "acres"        -> (long) (area * 43_560);
            case "square feet"  -> (long) area;
            // Square meters, if ever encountered from international data
            case "square meters" -> (long) (area * 10.764);
            default             -> null;  // unknown unit — don't guess
        };
    }

    /**
     * Maps the Rooms expanded resource to our roomInfo JSON format.
     *
     * The RESO Rooms resource provides: RoomKey, RoomType, RoomLevel (nullable),
     * RoomDimensions (nullable), NST_RoomSortOrder.
     *
     * Output format (sorted by sortOrder ascending):
     *   [{"key":"NST75326957","room":"Living Room","level":"Main","dim":"15x21","sortOrder":1},...]
     *
     * "level" and "dim" are omitted from the object if null/missing — the
     * frontend must handle their absence.
     */
    private String mapRooms(JsonNode p) {
        JsonNode rooms = p.path("Rooms");
        if (!rooms.isArray() || rooms.isEmpty()) return null;

        List<Map<String, Object>> roomList = new ArrayList<>();

        for (JsonNode r : rooms) {
            Map<String, Object> room = new LinkedHashMap<>();

            String key = text(r, "RoomKey");
            if (key != null) room.put("key", key);

            room.put("room", text(r, "RoomType"));

            String level = text(r, "RoomLevel");
            if (level != null) room.put("level", level);

            String dim = text(r, "RoomDimensions");
            if (dim != null) room.put("dim", dim);

            // NST_RoomSortOrder comes as a String ("1", "2"...) — parse to Integer for sorting
            String sortStr = text(r, "NST_RoomSortOrder");
            if (sortStr != null) {
                try {
                    room.put("sortOrder", Integer.parseInt(sortStr));
                } catch (NumberFormatException ignored) {
                    // If the sort order is malformed, omit it; the room still maps
                }
            }

            roomList.add(room);
        }

        // Sort by sortOrder ascending; rooms without sortOrder go to the end
        roomList.sort(Comparator.comparingInt(
            m -> (Integer) m.getOrDefault("sortOrder", Integer.MAX_VALUE)
        ));

        try {
            return objectMapper.writeValueAsString(roomList);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Extracts the primary (Order=1) photo URL for fast thumbnail access.
     *
     * MLS Grid docs: "The URL is for the highest resolution photo that the MLS
     * provides." In production, you must download this and serve from your own
     * storage — do not serve MLS Grid URLs directly on your public site.
     */
    private String extractPrimaryImageUrl(JsonNode p) {
        JsonNode media = p.path("Media");
        if (!media.isArray() || media.isEmpty()) return null;

        // First pass: find the explicitly Order=1 record
        for (JsonNode m : media) {
            if (m.path("Order").asInt(-1) == 1) {
                return text(m, "MediaURL");
            }
        }

        // Fallback: if no Order=1 found (malformed data), use the first element
        return text(media.get(0), "MediaURL");
    }

    /**
     * Serializes the full Media array to a JSON string for storage.
     *
     * We store the full array — key, order, url, dimensions, timestamp — so the
     * detail page can render a full photo carousel without re-querying the API.
     *
     * The full JsonNode array is serialized as-is; no field trimming here.
     * If storage size becomes a concern, trim to {key, order, url} only.
     */
    private String serializeMedia(JsonNode p) {
        JsonNode media = p.path("Media");
        if (!media.isArray() || media.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(media);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Primitive extraction helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the text value of a field, or null if the field is missing or null.
     * Blank strings are normalized to null — we don't store empty strings.
     *
     * Why not node.asText("")? Because asText("") on a MissingNode returns ""
     * which we'd then store as an empty string column value. Null is more correct.
     */
    private String text(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        String val = n.asText();
        return val.isBlank() ? null : val;
    }

    private Boolean bool(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        return n.asBoolean();
    }

    private Integer intVal(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        return n.asInt();
    }

    private Long longVal(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        return n.asLong();
    }

    private Double doubleVal(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        return n.asDouble();
    }

    /**
     * Parses an ISO-8601 date string (e.g. "2018-01-08") to LocalDate.
     * Returns null on parse failure rather than throwing — sparse MLS data
     * sometimes has malformed dates and we should not fail the entire ingest.
     */
    private LocalDate localDate(JsonNode node, String field) {
        String val = text(node, field);
        if (val == null) return null;
        try {
            return LocalDate.parse(val);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Parses an ISO-8601 instant string (e.g. "2021-04-08T21:06:06.307Z") to Instant.
     * Returns null on failure.
     */
    private Instant instant(JsonNode node, String field) {
        String val = text(node, field);
        if (val == null) return null;
        try {
            return Instant.parse(val);
        } catch (DateTimeException e) {
            return null;
        }
    }

    /**
     * Serializes a JSON array field to a JSON string.
     *
     * RESO array fields (Appliances, Heating, Cooling, etc.) come as actual
     * JSON arrays in the API response. We store them as JSON strings in TEXT
     * columns. The frontend parses them back to arrays for display.
     *
     * Example: ["Dryer","Range","Washer"] → '["Dryer","Range","Washer"]'
     *
     * Returns null if the field is missing, null, or not an array.
     */
    private String toJsonString(JsonNode node, String field) {
        JsonNode array = node.path(field);
        if (array.isMissingNode() || array.isNull()) return null;
        if (!array.isArray() && !array.isObject()) return null;
        try {
            return objectMapper.writeValueAsString(array);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Returns the first element of a JSON array field as a plain string.
     *
     * Used for Levels, which is an array but typically has one value in practice:
     *   ["Split Entry (Bi-Level)"] → "Split Entry (Bi-Level)"
     *
     * If the array has multiple elements, only the first is returned.
     * Use toJsonString() instead if you need all elements.
     */
    private String firstArrayElement(JsonNode node, String field) {
        JsonNode array = node.path(field);
        if (!array.isArray() || array.isEmpty()) return null;
        String val = array.get(0).asText();
        return val.isBlank() ? null : val;
    }
}