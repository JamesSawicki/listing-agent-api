package com.jimrealty.listingagent.controller;

import com.jimrealty.listingagent.model.Listing;
import com.jimrealty.listingagent.service.ListingService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RestController = @Controller + @ResponseBody
// Every method return value is automatically serialized to JSON.
// @RequestMapping sets the base URL for all endpoints in this class.
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    // GET /api/listings
    // Returns all listings. The frontend uses this to populate a listing picker.
    @GetMapping
    public ResponseEntity<List<Listing>> getAllListings() {
        return ResponseEntity.ok(listingService.getAllListings());
    }

    // GET /api/listings/{id}
    // {id} is a path variable — /api/listings/1 sets id=1.
    // @PathVariable binds it to the method parameter.
    @GetMapping("/{id}")
    public ResponseEntity<Listing> getListingById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(listingService.getListingById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404
        }
    }

    // POST /api/listings
    // @RequestBody deserializes the JSON request body into a Listing object.
    // @Valid triggers the validation annotations on the model (@NotBlank etc.)
    @PostMapping
    public ResponseEntity<Listing> createListing(@Valid @RequestBody Listing listing) {
        Listing created = listingService.createListing(listing);
        return ResponseEntity.status(HttpStatus.CREATED).body(created); // 201
    }

    // PUT /api/listings/{id}
    // Full update — replaces all fields. Use PATCH for partial updates.
    @PutMapping("/{id}")
    public ResponseEntity<Listing> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody Listing listing) {
        try {
            return ResponseEntity.ok(listingService.updateListing(id, listing));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/listings/{id}
    // 204 No Content is the correct status for a successful delete with no body.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        try {
            listingService.deleteListing(id);
            return ResponseEntity.noContent().build(); // 204
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
