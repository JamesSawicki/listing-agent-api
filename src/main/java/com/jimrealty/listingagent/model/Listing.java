package com.jimrealty.listingagent.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

// @Data generates: getters, setters, toString, equals, hashCode
// @Builder gives us the builder pattern: Listing.builder().address("...").build()
// @NoArgsConstructor and @AllArgsConstructor are required by JPA and Builder respectively
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String address;

    @NotBlank
    private String cityStateZip;

    private String neighborhood;
    private String price;
    private String style;
    private String yearBuilt;
    private String beds;
    private String baths;
    private String sqft;
    private String lotSqft;
    private String garage;
    private String taxes;
    private String estimatedPayment;
    private String agentName;

    // Longer text fields — stored as TEXT in DB rather than VARCHAR(255)
    @Column(columnDefinition = "TEXT")
    private String description;

    // Features stored as newline-separated text.
    // A more normalized design would put these in a child table,
    // but for this use case keeping them as text is simpler and
    // maps directly to the textarea in the frontend form.
    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(columnDefinition = "TEXT")
    private String location;
}
