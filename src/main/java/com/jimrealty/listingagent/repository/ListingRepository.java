package com.jimrealty.listingagent.repository;

import com.jimrealty.listingagent.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// JpaRepository<Listing, Long> gives us all standard CRUD operations for free:
//   save(), findById(), findAll(), deleteById() etc.
// Spring Data generates the actual SQL implementation at runtime —
// we never write a query for standard operations.
@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    // Spring Data parses method names and generates SQL automatically.
    // "findByAgentName" becomes: SELECT * FROM listings WHERE agent_name = ?
    // No SQL, no @Query annotation needed — just the right method name.
    List<Listing> findByAgentName(String agentName);

    // findByNeighborhoodContainingIgnoreCase becomes:
    // SELECT * FROM listings WHERE LOWER(neighborhood) LIKE LOWER(CONCAT('%', ?, '%'))
    List<Listing> findByNeighborhoodContainingIgnoreCase(String neighborhood);
}
