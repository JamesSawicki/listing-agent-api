package com.jimrealty.listingagent.service;

import com.jimrealty.listingagent.model.Listing;
import com.jimrealty.listingagent.repository.ListingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// @RequiredArgsConstructor generates a constructor for all final fields.
// Spring sees that constructor and injects the ListingRepository automatically.
// This is constructor injection — preferred over @Autowired field injection
// because it makes dependencies explicit and easier to test.
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;

    // Get all listings
    public List<Listing> getAllListings() {
        return listingRepository.findAll();
    }

    // Get a single listing by ID — throws a meaningful exception if not found.
    // The controller will catch this and return a 404.
    public Listing getListingById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found with id: " + id));
    }

    // Create a new listing
    public Listing createListing(Listing listing) {
        return listingRepository.save(listing);
    }

    // Update an existing listing.
    // We fetch first to confirm it exists, then apply changes from the request.
    // This is safer than blindly saving — it returns a 404 if the ID is wrong.
    public Listing updateListing(Long id, Listing updatedListing) {
        Listing existing = getListingById(id); // throws 404 if not found

        // Apply updates — in a larger app you might use a mapper library (MapStruct)
        // to do this more elegantly, but explicit assignment is fine here.
        existing.setAddress(updatedListing.getAddress());
        existing.setCityStateZip(updatedListing.getCityStateZip());
        existing.setNeighborhood(updatedListing.getNeighborhood());
        existing.setPrice(updatedListing.getPrice());
        existing.setStyle(updatedListing.getStyle());
        existing.setYearBuilt(updatedListing.getYearBuilt());
        existing.setBeds(updatedListing.getBeds());
        existing.setBaths(updatedListing.getBaths());
        existing.setSqft(updatedListing.getSqft());
        existing.setLotSqft(updatedListing.getLotSqft());
        existing.setGarage(updatedListing.getGarage());
        existing.setTaxes(updatedListing.getTaxes());
        existing.setEstimatedPayment(updatedListing.getEstimatedPayment());
        existing.setAgentName(updatedListing.getAgentName());
        existing.setDescription(updatedListing.getDescription());
        existing.setFeatures(updatedListing.getFeatures());
        existing.setLocation(updatedListing.getLocation());

        return listingRepository.save(existing);
    }

    // Delete
    public void deleteListing(Long id) {
        getListingById(id); // confirms existence before deleting
        listingRepository.deleteById(id);
    }
}
