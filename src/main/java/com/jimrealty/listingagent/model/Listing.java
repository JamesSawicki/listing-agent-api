package com.jimrealty.listingagent.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
 * roomInfo stores a JSON array of room objects:
 *   [{"room":"Living Room","level":"Main","dim":"15x21"}, ...]
 *
 * amenityScores stores a per-property scorecard of nearby amenities,
 * computed from Mapbox isochrones + OSM POI data:
 *   {"walk_10min":{"grocery":{"count":2,"nearestName":"Cub Foods",...},...},...}
 *
 * latitude/longitude store WGS-84 decimal degrees from the MLS feed or
 * from geocoding at import time. Used for map display and bbox search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "listings")
public class Listing {

    // -------------------------------------------------------------------------
    // Primary Key
    // -------------------------------------------------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------------------------------------------------------------------------
    // Identity & Status
    // -------------------------------------------------------------------------

    private String mlsId;

    /** Active | Pending | Sold | TNAS | Withdrawn | Expired | Cancelled | Coming Soon */
    private String status;

    private String listDate;
    private String pendingDate;
    private String closeDate;
    private Integer daysOnMarket;

    private Long listPrice;
    private Long originalListPrice;
    private Long closePrice;

    // -------------------------------------------------------------------------
    // Location
    // -------------------------------------------------------------------------

    @NotBlank
    private String address;

    @NotBlank
    private String city;

    private String zipCode;
    private String county;
    private String neighborhood;
    private String complexSubdiv;

    // -------------------------------------------------------------------------
    // Geographic Coordinates
    // WGS-84 decimal degrees. Populated from MLS feed or geocoding at import.
    // Used for map display (Phase 1) and amenity score computation (Phase 2).
    // Indexed via @Index in production; H2 handles small datasets without it.
    // -------------------------------------------------------------------------

    private Double latitude;
    private Double longitude;

    // -------------------------------------------------------------------------
    // Property Basics
    // -------------------------------------------------------------------------

    private String propertyType;
    private String style;
    private Integer yearBuilt;
    private String stories;
    private String constructionStatus;

    private Integer beds;

    /**
     * All four bath types stored separately for RESO compliance.
     * Display total = (bathsFull * 1.0) + (bathsThreeQuarter * 0.75)
     *               + (bathsHalf * 0.5) + (bathsQuarter * 0.25)
     */
    private Integer bathsFull;
    private Integer bathsThreeQuarter;
    private Integer bathsHalf;
    private Integer bathsQuarter;

    private Integer sqftAboveGrade;
    private Integer sqftBelowGrade;
    private Integer sqftTotal;
    private Integer sqftMainLevel;

    private Long lotSqft;
    private String lotDimensions;

    private Integer garageStalls;
    private Integer garageSqft;

    private String pool;

    // -------------------------------------------------------------------------
    // Room Details
    // JSON: [{"room":"Living Room","level":"Main","dim":"15x21"},...]
    // Future: @OneToMany Room entity when RESO feed is live
    // -------------------------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String roomInfo;

    // -------------------------------------------------------------------------
    // Features
    // -------------------------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String appliances;

    private String basement;
    private String heating;
    private String airConditioning;
    private String fuelType;
    private String fireplaceFeatures;
    private String fireplaces;
    private String constructionMaterials;

    @Column(columnDefinition = "TEXT")
    private String exteriorFeatures;

    private String roof;
    private String electric;
    private String sewer;
    private String waterSource;
    private String fencing;

    @Column(columnDefinition = "TEXT")
    private String lotFeatures;

    private String diningRoomFeatures;
    private String familyRoomFeatures;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    private String parkingFeatures;
    private String laundryFeatures;
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

    private String associationFee;
    private String associationFeeFrequency;

    @Column(columnDefinition = "TEXT")
    private String associationFeeIncludes;

    private String associationMgmtName;
    private String associationMgmtPhone;

    // -------------------------------------------------------------------------
    // Financial
    // -------------------------------------------------------------------------

    private String taxAmount;
    private String taxYear;
    private String taxWithAssessments;

    // -------------------------------------------------------------------------
    // Schools
    // -------------------------------------------------------------------------

    private String elementarySchool;
    private String middleSchool;
    private String highSchool;
    private String schoolDistrict;

    // -------------------------------------------------------------------------
    // Agent / Office
    // -------------------------------------------------------------------------

    private String listAgentName;
    private String listAgentPhone;
    private String listAgentMlsId;
    private String listOfficeName;
    private String listOfficePhone;
    private String listOfficeMlsId;

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String publicRemarks;

    private Integer photosCount;
    private String directions;

    // -------------------------------------------------------------------------
    // Computed Scores (Phase 2 — populated by AmenityScoreService)
    // JSON blob — see comments at top of class for structure.
    // -------------------------------------------------------------------------

    @Column(columnDefinition = "TEXT")
    private String amenityScores;
}