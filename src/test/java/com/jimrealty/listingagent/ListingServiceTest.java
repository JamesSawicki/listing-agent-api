package com.jimrealty.listingagent;

import com.jimrealty.listingagent.model.Listing;
import com.jimrealty.listingagent.repository.ListingRepository;
import com.jimrealty.listingagent.service.ListingService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) wires up Mockito without needing Spring context.
// Unit tests should run fast — no database, no HTTP, just logic.
@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    // @Mock creates a fake ListingRepository — no real DB calls
    @Mock
    private ListingRepository listingRepository;

    // @InjectMocks creates a real ListingService, injecting the mock repository
    @InjectMocks
    private ListingService listingService;

    private Listing sampleListing;

    @BeforeEach
    void setUp() {
        sampleListing = Listing.builder()
                .id(1L)
                .address("1847 Goodrich Ave")
                .cityStateZip("Saint Paul, MN 55105")
                .beds("4")
                .price("$685,000")
                .agentName("Jim")
                .build();
    }

    @Test
    void getListingById_whenFound_returnsListing() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(sampleListing));

        Listing result = listingService.getListingById(1L);

        assertEquals("1847 Goodrich Ave", result.getAddress());
        verify(listingRepository, times(1)).findById(1L);
    }

    @Test
    void getListingById_whenNotFound_throwsEntityNotFoundException() {
        when(listingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> listingService.getListingById(99L));
    }

    @Test
    void createListing_savesAndReturnsListing() {
        when(listingRepository.save(sampleListing)).thenReturn(sampleListing);

        Listing result = listingService.createListing(sampleListing);

        assertEquals(sampleListing.getId(), result.getId());
        verify(listingRepository, times(1)).save(sampleListing);
    }
}
