package com.jimrealty.listingagent.repository;

import com.jimrealty.listingagent.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Collection;
import java.util.List;

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
}