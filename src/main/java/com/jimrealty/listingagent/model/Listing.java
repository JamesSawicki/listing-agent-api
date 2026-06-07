package com.jimrealty.listingagent.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Listing entity — mirrors the NorthStar MLS / RESO Web API field structure.
 *
 * Field groupings match the NorthStar listing sheet exactly.
 *
 * Numeric types for search-filterable fields (price, beds, baths, sqft, lot).
 * All others are String.
 *
 * Bath total is DERIVED, never stored:
 *   total = (bathsFull * 1.0) + (bathsThreeQuarter * 0.75)
 *         + (bathsHalf * 0.5) + (bathsQuarter * 0.25)
 *
 * Lot acres is DERIVED from lotSqft: acres = lotSqft / 43560.0
 *
 * roomInfo stores a JSON array of room objects (sorted by sortOrder):
 *   [{"key":"NST75326957","room":"Living Room","level":"Main","dim":"15x21","sortOrder":1}, ...]
 *   "key" and "sortOrder" added after MLS Grid integration; "level" and "dim" may be null.
 *
 * mediaJson stores the full Media array from MLS Grid as JSON. primaryImageUrl
 * holds the Order=1 photo URL for fast thumbnail access without parsing mediaJson.
 *
 * amenityScores stores a per-property scorecard of nearby amenities,
 * computed from Mapbox isochrones + OSM POI data:
 *   {"walk_10min":{"grocery":{"count":2,"nearestName":"Cub Foods",...},...},...}
 *
 * latitude/longitude store WGS-84 decimal degrees from the MLS feed.
 * Used for map display and bbox search.
 *
 * --- MLS GRID SYNC FIELDS ---
 * listingKey   : System primary key (e.g. "NST2878774"). Used for API delta queries.
 *                Strip "NST" prefix before displaying publicly.
 * mlsId        : Human-readable MLS# (e.g. "NST4899371"), displayed as "MLS#" to users.
 *                Also prefixed by MLS Grid; strip before display.
 * mlgCanView   : MLS Grid deletion flag. When false, remove from DB immediately.
 * modificationTimestamp : Drives all delta sync queries (ModificationTimestamp gt [last_seen]).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "listings", indexes = {
    @Index(name = "idx_listing_lat_lng",        columnList = "latitude, longitude"),
    @Index(name = "idx_listing_status",         columnList = "status"),
    @Index(name = "idx_listing_key",            columnList = "listing_key"),
    @Index(name = "idx_listing_mod_timestamp",  columnList = "modification_timestamp")
})
public class Listing {

    // -------------------------------------------------------------------------
    // Primary Key
    // -------------------------------------------------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------------------------------------------------------------------------
    // MLS Grid Sync Metadata
    // Populated by MlsGridIngestionService. Not for public display.
    // -------------------------------------------------------------------------

    /**
     * System primary key from MLS Grid (e.g. "NST2878774").
     * Distinct from mlsId. Used for all delta sync API queries.
     * Must strip "NST" prefix before displaying externally.
     */
    @Column(name = "listing_key", unique = true)
    private String listingKey;

    /**
     * MLS Grid deletion flag. When this is false, the record must be
     * removed from the database. Checked on every ingestion pass.
     */
    private Boolean mlgCanView;

    /**
     * Last time MLS Grid modified this record (UTC).
     * The entire delta sync strategy pivots on this field:
     *   GET /v2/Property?$filter=...and ModificationTimestamp gt [greatest value in our DB]
     */
    @Column(name = "modification_timestamp")
    private Instant modificationTimestamp;

    /** When the listing first entered MLS Grid. Useful for analytics. */
    private Instant originalEntryTimestamp;

    /**
     * Allowed use cases from MLS Grid (IDX, VOW, BO, PT).
     * Stored as JSON string, e.g. '["IDX"]'.
     * Governs how this listing may be displayed or used.
     */
    private String mlgCanUse;

    // -------------------------------------------------------------------------
    // Identity & Status
    // -------------------------------------------------------------------------

    /**
     * Human-readable MLS number (e.g. "NST4899371"), displayed to users as "MLS#".
     * This is ListingId in the RESO feed — distinct from listingKey (the system key).
     * Strip "NST" prefix before displaying.
     */
    private String mlsId;

    /** StandardStatus: Active | ActiveUnderContract | Pending | Closed | Expired | Withdrawn | Hold | Cancelled | ComingSoon */
    private String status;

    private LocalDate listDate;
    private LocalDate pendingDate;
    private LocalDate closeDate;
    private LocalDate offMarketDate;   // when listing left active pool (MLS compliance)
    private Integer daysOnMarket;

    private Long listPrice;
    private Long originalListPrice;
    private Long closePrice;

    private String contingency;        // "None" | "Inspection" | "Financing" | etc.

    // -------------------------------------------------------------------------
    // Location
    // -------------------------------------------------------------------------

    @NotBlank
    private String address;

    @NotBlank
    private String city;               // MLS City (may be township, e.g. "Buckman Twp")
    private String postalCity;         // Mailing city — often differs from City in rural MN

    private String zipCode;
    private String county;
    private String neighborhood;
    private String complexSubdiv;

    private String parcelNumber;       // Tax parcel / APN
    private String zoningDescription;

    // -------------------------------------------------------------------------
    // Geographic Coordinates
    // WGS-84 decimal degrees. Populated directly from MLS feed.
    // Indexed for map bbox search.
    // -------------------------------------------------------------------------

    private Double latitude;
    private Double longitude;

    // -------------------------------------------------------------------------
    // Property Basics
    // -------------------------------------------------------------------------

    private String propertyType;       // PropertySubType: "Single Family Residence", "Condo", etc.
    private String style;              // Levels[0]: "Split Entry (Bi-Level)", "Two Story", etc.
    private Integer yearBuilt;
    private String stories;            // StoriesTotal if present
    private String constructionStatus;
    private Boolean newConstruction;   // NewConstructionYN

    private Integer beds;

    /**
     * All four bath types stored separately for RESO compliance.
     * Display total = (bathsFull * 1.0) + (bathsThreeQuarter * 0.75)
     *               + (bathsHalf * 0.5) + (bathsQuarter * 0.25)
     */
    private Integer bathsFull;
    private Integer bathsThreeQuarter;
    private Integer bathsHalf;
    private Integer bathsQuarter;      // RESO: BathroomsOneQuarter

    private Integer sqftAboveGrade;
    private Integer sqftBelowGrade;
    private Integer sqftTotal;         // RESO: LivingArea
    private Integer sqftMainLevel;

    /**
     * Lot size in square feet.
     * DERIVED in mapper: if LotSizeUnits == "Acres", multiply LotSizeArea * 43560.
     * Frontend derives lot acres from this: acres = lotSqft / 43560.0
     */
    private Long lotSqft;
    private String lotDimensions;

    private Integer garageStalls;
    private Integer garageSqft;

    private String pool;

    // -------------------------------------------------------------------------
    // Room Details
    // JSON array sorted by NST_RoomSortOrder:
    //   [{"key":"NST75326957","room":"Living Room","level":"Main","dim":"15x21","sortOrder":1},...]
    // "level" and "dim" may be absent for some room types.
    // -------------------------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String roomInfo;

    // -------------------------------------------------------------------------
    // Features
    // Array fields from RESO are serialized to JSON strings, e.g. '["Dryer","Range","Washer"]'
    // -------------------------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String appliances;         // RESO: Appliances[]

    private String basement;           // RESO: Basement[] → e.g. '["Full"]'
    private String heating;            // RESO: Heating[]
    private String airConditioning;    // RESO: Cooling[]
    private String fuelType;           // NST_Fuel
    private Integer fireplaces;        // RESO: FireplacesTotal (was String — fixed)
    private String fireplaceFeatures;  // RESO: FireplaceFeatures (if present)
    private String constructionMaterials; // RESO: ConstructionMaterials[]

    @Column(columnDefinition = "TEXT")
    private String exteriorFeatures;

    private String roof;               // RESO: Roof[]
    private String electric;
    private String sewer;              // RESO: Sewer[]
    private String waterSource;        // RESO: WaterSource[]
    private String fencing;

    @Column(columnDefinition = "TEXT")
    private String lotFeatures;

    private String diningRoomFeatures; // NST_DiningRoomDescription
    private String familyRoomFeatures;

    @Column(columnDefinition = "TEXT")
    private String amenities;          // NST_AmenitiesUnit

    private String parkingFeatures;    // RESO: ParkingFeatures[]
    private String laundryFeatures;    // NST_SpecialSearch (contains laundry location info)
    private String financing;

    // -------------------------------------------------------------------------
    // Waterfront / Lake
    // -------------------------------------------------------------------------

    private Integer waterfrontFeet;
    private String waterfrontView;
    private String waterBodyName;
    private String surfaceWaterType;
    private String dnrLakeClass;
    private String dnrLakeId;
    private String lakeAcres;
    private String lakeDepth;
    private String lakeBottomType;

    // -------------------------------------------------------------------------
    // HOA
    // -------------------------------------------------------------------------

    private Long associationFee;           // was String — RESO returns numeric (was breaking search)
    private String associationFeeFrequency;

    @Column(columnDefinition = "TEXT")
    private String associationFeeIncludes;

    private String associationMgmtName;
    private String associationMgmtPhone;

    // -------------------------------------------------------------------------
    // Financial
    // -------------------------------------------------------------------------

    private Long taxAmount;            // RESO: TaxAnnualAmount (was String — fixed)
    private Integer taxYear;           // RESO: TaxYear (was String — fixed)
    private String taxWithAssessments; // NST_TaxWithAssessments (kept String: "1069.0400" format)

    // -------------------------------------------------------------------------
    // Schools
    // -------------------------------------------------------------------------

    private String elementarySchool;
    private String middleSchool;
    private String highSchool;
    private String schoolDistrict;     // RESO: HighSchoolDistrict
    private String schoolDistrictNumber; // NST_SchoolDistrictNumber

    // -------------------------------------------------------------------------
    // Agent / Office
    // -------------------------------------------------------------------------

    private String listAgentName;
    private String listAgentPhone;
    private String listAgentMlsId;
    private String listOfficeName;
    private String listOfficePhone;
    private String listOfficeMlsId;    // RESO: ListOfficeKey (strip NST prefix for display)

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String publicRemarks;

    private Integer photosCount;
    private String directions;

    /**
     * URL of the Order=1 photo from MLS Grid Media expansion.
     * Fast path for thumbnail rendering — avoids parsing mediaJson.
     * Per MLS Grid rules, in production this must point to your own hosted copy,
     * not the MLS Grid / S3 URL directly.
     */
    private String primaryImageUrl;

    /**
     * Full Media array from MLS Grid as a JSON string.
     * Each element: {key, order, url, width, height, modificationTimestamp}
     * Used by the listing detail page photo carousel.
     * In production, URLs must be replaced with locally hosted paths after download.
     */
    @Column(columnDefinition = "TEXT")
    private String mediaJson;

    // -------------------------------------------------------------------------
    // Computed Scores (Phase 2 — populated by AmenityScoreService)
    // JSON blob — see comments at top of class for structure.
    // -------------------------------------------------------------------------

    @Column(name = "amenity_scores", columnDefinition = "TEXT")
    private String amenityScores;
}