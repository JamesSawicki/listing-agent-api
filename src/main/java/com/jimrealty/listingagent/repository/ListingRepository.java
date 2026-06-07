package com.jimrealty.listingagent.repository;

import com.jimrealty.listingagent.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.Optional;
/**
 * ListingRepository — Spring Data JPA repository for Listing entities.
 *
 * Extends two interfaces:
 *
 * JpaRepository<Listing, Long>
 *   → Provides the standard CRUD methods: findAll(), findById(), save(),
 *     deleteById(), count(), etc. The Long is the type of the @Id field.
 *
 * JpaSpecificationExecutor<Listing>
 *   → Adds findAll(Specification<T>, Pageable) and related overloads.
 *     This is what allows ListingSpecification to compose dynamic WHERE
 *     clauses and pass them directly to the database via Spring Data.
 *     Without this interface, Specifications compile but can't be executed.
 *
 * No method declarations needed — Spring Data generates all implementations
 * at startup by reading the interface and creating a proxy class.
 */
public interface ListingRepository
        extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

        List<Listing> findByStatusIn(Collection<String> statuses);

        // Upsert lookup — the entire delta sync strategy relies on this
        Optional<Listing> findByListingKey(String listingKey);
        
        // Returns the greatest ModificationTimestamp in the DB.
        // If null (empty DB), deltaSync falls back to a full historical pull.
        @Query("SELECT MAX(l.modificationTimestamp) FROM Listing l WHERE l.modificationTimestamp IS NOT NULL")
        Optional<Instant> findMaxModificationTimestamp();

        /**
         * Media-ingestion backfill candidates: any listing whose media has not
         * been fully ingested to Cloudflare Images. Caller further filters to
         * only those with mediaJson present.
         *
         * Status values: NULL = never attempted, PENDING/IN_PROGRESS = stuck or
         * mid-flight, FAILED = retry candidate, COMPLETE = skip.
         */
        @Query("SELECT l FROM Listing l " +
               "WHERE l.mediaIngestionStatus IS NULL " +
               "   OR l.mediaIngestionStatus <> 'COMPLETE' " +
               "ORDER BY l.id ASC")
        List<Listing> findMediaIngestionPending();
}