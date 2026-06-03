package com.jimrealty.listingagent.service;

import com.jimrealty.listingagent.model.Listing;
import com.jimrealty.listingagent.model.ListingSearchParams;
import com.jimrealty.listingagent.repository.ListingRepository;
import com.jimrealty.listingagent.spec.ListingSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ListingService — business logic layer between the controller and repository.
 *
 * @RequiredArgsConstructor (Lombok) generates a constructor that injects
 * all final fields. Spring sees one constructor and uses it for dependency
 * injection automatically — no @Autowired needed.
 */
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;

    // ------------------------------------------------------------------
    // Existing methods (unchanged)
    // ------------------------------------------------------------------

    public List<Listing> getAllListings() {
        return listingRepository.findAll();
    }

    public Listing getListingById(@NonNull Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found: " + id));
    }

    public Listing createListing(@NonNull Listing listing) {
        return listingRepository.save(listing);
    }

    public Listing updateListing(@NonNull Long id, Listing updated) {
        Listing existing = getListingById(id);
        updated.setId(existing.getId());
        return listingRepository.save(updated);
    }

    public void deleteListing(@NonNull Long id) {
        listingRepository.deleteById(id);
    }

    // ------------------------------------------------------------------
    // Search — new
    // ------------------------------------------------------------------

    /**
     * Executes a dynamic filtered search using the Specification pattern.
     *
     * Returns a Page<Listing> rather than List<Listing> for two reasons:
     * 1. Pagination — the frontend asks for page 0 size 24, page 1 size 24, etc.
     *    Without pagination, a live MLS feed with 15,000 listings returns all of
     *    them on every request, which is both slow and wasteful.
     * 2. Metadata — Page carries totalElements and totalPages so the frontend
     *    can render "Showing 1-24 of 347 results" and a page navigator.
     *
     * PageRequest.of(page, size) builds the Pageable Spring Data needs.
     * Default: page 0, 24 results per page.
     *
     * @param params  Search filter parameters from the HTTP query string
     * @return        Paginated, filtered, sorted listing results
     */
    @SuppressWarnings("null")
    public Page<Listing> searchListings(@NonNull ListingSearchParams params) {
        int page = params.getPage() != null ? params.getPage() : 0;
        int size = params.getSize() != null ? params.getSize() : 24;

        Pageable pageable = PageRequest.of(page, size);

        return listingRepository.findAll(
                ListingSpecification.fromParams(params),
                pageable
        );
    }
}