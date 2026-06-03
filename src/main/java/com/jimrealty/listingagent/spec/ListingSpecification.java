package com.jimrealty.listingagent.spec;

import com.jimrealty.listingagent.model.Listing;
import com.jimrealty.listingagent.model.ListingSearchParams;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ListingSpecification — dynamic query builder using the JPA Criteria API.
 *
 * Builds the WHERE clause at runtime by composing predicates conditionally.
 * Fields not present in the request (null params) are simply skipped.
 *
 * Root<T>         → the FROM clause. root.get("fieldName") gives you a column.
 * CriteriaBuilder → factory for predicates: equal(), greaterThanOrEqualTo(), etc.
 * CriteriaQuery   → the full query object; used here only for ordering.
 *
 * Bounding box notes:
 * - All four params (minLat/maxLat/minLng/maxLng) must be non-null to activate.
 * - This prevents a partial bbox from accidentally filtering out all results
 *   when the map page hasn't fully initialized its bounds.
 * - Longitude is negative in the western hemisphere. "minLng" is the west
 *   edge, which is the more negative value (e.g., -93.65). The BETWEEN
 *   check (lng >= minLng AND lng <= maxLng) handles this correctly because
 *   -93.65 < -93.20 numerically.
 */
public class ListingSpecification {

    public static Specification<Listing> fromParams(ListingSearchParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // -----------------------------------------------------------------
            // STATUS
            // -----------------------------------------------------------------
            if (params.getStatus() == null || params.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), "Active"));
            } else {
                List<String> statuses = Arrays.asList(params.getStatus().split(","));
                predicates.add(root.get("status").in(statuses));
            }

            // -----------------------------------------------------------------
            // PRICE RANGE
            // -----------------------------------------------------------------
            if (params.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("listPrice"), params.getMinPrice()));
            }
            if (params.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("listPrice"), params.getMaxPrice()));
            }

            // -----------------------------------------------------------------
            // BEDS
            // -----------------------------------------------------------------
            if (params.getMinBeds() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("beds"), params.getMinBeds()));
            }

            // -----------------------------------------------------------------
            // BATHS — derived total minimum
            // SQL expression: (bathsFull * 1.0 + bathsThreeQuarter * 0.75
            //                  + bathsHalf * 0.5 + bathsQuarter * 0.25) >= minBaths
            // -----------------------------------------------------------------
            if (params.getMinBaths() != null) {
                Expression<Double> fullBaths = cb.prod(
                        root.<Integer>get("bathsFull").as(Double.class),
                        cb.literal(1.0)
                );
                Expression<Double> threeQuarterBaths = cb.prod(
                        root.<Integer>get("bathsThreeQuarter").as(Double.class),
                        cb.literal(0.75)
                );
                Expression<Double> halfBaths = cb.prod(
                        root.<Integer>get("bathsHalf").as(Double.class),
                        cb.literal(0.5)
                );
                Expression<Double> quarterBaths = cb.prod(
                        root.<Integer>get("bathsQuarter").as(Double.class),
                        cb.literal(0.25)
                );
                Expression<Double> bathTotal = cb.sum(
                        cb.sum(cb.sum(fullBaths, threeQuarterBaths), halfBaths),
                        quarterBaths
                );
                predicates.add(cb.greaterThanOrEqualTo(bathTotal, params.getMinBaths()));
            }

            // -----------------------------------------------------------------
            // SQUARE FOOTAGE
            // -----------------------------------------------------------------
            if (params.getMinSqft() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sqftTotal"), params.getMinSqft()));
            }

            // -----------------------------------------------------------------
            // LOCATION
            // -----------------------------------------------------------------
            if (params.getCity() != null && !params.getCity().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("city")),
                        "%" + params.getCity().toLowerCase() + "%"
                ));
            }
            if (params.getZipCode() != null && !params.getZipCode().isBlank()) {
                predicates.add(cb.equal(root.get("zipCode"), params.getZipCode()));
            }
            if (params.getCounty() != null && !params.getCounty().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("county")),
                        "%" + params.getCounty().toLowerCase() + "%"
                ));
            }
            if (params.getNeighborhood() != null && !params.getNeighborhood().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("neighborhood")),
                        "%" + params.getNeighborhood().toLowerCase() + "%"
                ));
            }

            // -----------------------------------------------------------------
            // PROPERTY TYPE
            // -----------------------------------------------------------------
            if (params.getPropertyType() != null && !params.getPropertyType().isBlank()) {
                predicates.add(cb.equal(root.get("propertyType"), params.getPropertyType()));
            }

            // -----------------------------------------------------------------
            // WATERFRONT
            // -----------------------------------------------------------------
            if (Boolean.TRUE.equals(params.getWaterfront())) {
                predicates.add(cb.greaterThan(root.get("waterfrontFeet"), 0));
            }

            // -----------------------------------------------------------------
            // POOL
            // -----------------------------------------------------------------
            if (Boolean.TRUE.equals(params.getPool())) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("pool")),
                        cb.notEqual(root.get("pool"), "None")
                ));
            }

            // -----------------------------------------------------------------
            // GARAGE
            // -----------------------------------------------------------------
            if (params.getMinGarageStalls() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("garageStalls"), params.getMinGarageStalls()));
            }

            // -----------------------------------------------------------------
            // YEAR BUILT
            // -----------------------------------------------------------------
            if (params.getMinYearBuilt() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("yearBuilt"), params.getMinYearBuilt()));
            }
            if (params.getMaxYearBuilt() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("yearBuilt"), params.getMaxYearBuilt()));
            }

            // -----------------------------------------------------------------
            // BOUNDING BOX — map search (Phase 1)
            //
            // All four params required. A partial bbox is silently ignored so
            // the user sees all results rather than a confusing empty state
            // during a map page that hasn't fully initialized its bounds.
            //
            // Longitude sign: west hemisphere values are negative.
            //   minLng (west edge) is more negative, e.g. -93.65
            //   maxLng (east edge) is less negative,  e.g. -93.20
            // The >= / <= comparison works correctly with negative doubles.
            // -----------------------------------------------------------------
            if (params.getMinLat() != null && params.getMaxLat() != null
                    && params.getMinLng() != null && params.getMaxLng() != null) {

                predicates.add(cb.greaterThanOrEqualTo(root.get("latitude"),  params.getMinLat()));
                predicates.add(cb.lessThanOrEqualTo(root.get("latitude"),     params.getMaxLat()));
                predicates.add(cb.greaterThanOrEqualTo(root.get("longitude"), params.getMinLng()));
                predicates.add(cb.lessThanOrEqualTo(root.get("longitude"),    params.getMaxLng()));
            }

            // -----------------------------------------------------------------
            // SORT ORDER
            // -----------------------------------------------------------------
            String sortBy = params.getSortBy() != null ? params.getSortBy() : "price_desc";
            switch (sortBy) {
                case "price_asc"  -> query.orderBy(cb.asc(root.get("listPrice")));
                case "newest"     -> query.orderBy(cb.desc(root.get("listDate")));
                case "dom_asc"    -> query.orderBy(cb.asc(root.get("daysOnMarket")));
                case "sqft_desc"  -> query.orderBy(cb.desc(root.get("sqftTotal")));
                default           -> query.orderBy(cb.desc(root.get("listPrice")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}