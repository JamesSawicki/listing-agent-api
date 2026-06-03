package com.jimrealty.listingagent;

import com.jimrealty.listingagent.model.Listing;
import com.jimrealty.listingagent.model.ListingSearchParams;
import com.jimrealty.listingagent.repository.ListingRepository;
import com.jimrealty.listingagent.service.ListingService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ListingServiceTest — unit tests for the service layer.
 *
 * Uses Mockito to mock the ListingRepository so tests don't hit a real database.
 * This is a UNIT test, not an integration test — we verify that the service
 * calls the repository correctly and handles results properly, NOT that the
 * Specification produces correct SQL (that's a separate integration concern).
 *
 * @ExtendWith(MockitoExtension.class) enables the @Mock and @InjectMocks
 * annotations on JUnit 5. The extension automatically creates mocks before
 * each test and injects them into the service.
 *
 * Lombok's @Builder is used to construct test listings with only the fields
 * each test cares about — every other field defaults to null.
 */
@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private ListingService listingService;

    private Listing sampleListing;

    @BeforeEach
    void setUp() {
        // A minimal but realistic listing used across multiple tests.
        // Builder pattern means we set only the fields we care about.
        sampleListing = Listing.builder()
                .id(1L)
                .mlsId("7001001")
                .status("Active")
                .listPrice(875000L)
                .address("5824 Interlachen Blvd")
                .city("Edina")
                .zipCode("55436")
                .beds(5)
                .bathsFull(3)
                .bathsThreeQuarter(1)
                .bathsHalf(1)
                .bathsQuarter(0)
                .sqftTotal(4050)
                .yearBuilt(1987)
                .propertyType("Single Family")
                .build();
    }

    // ----------------------------------------------------------------------
    // getAllListings
    // ----------------------------------------------------------------------

    @Test
    void getAllListings_returnsRepositoryResults() {
        when(listingRepository.findAll()).thenReturn(List.of(sampleListing));

        List<Listing> result = listingService.getAllListings();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAddress()).isEqualTo("5824 Interlachen Blvd");
        verify(listingRepository).findAll();
    }

    // ----------------------------------------------------------------------
    // getListingById
    // ----------------------------------------------------------------------

    @Test
    void getListingById_whenExists_returnsListing() {
        when(listingRepository.findById(1L)).thenReturn(Optional.of(sampleListing));

        Listing result = listingService.getListingById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMlsId()).isEqualTo("7001001");
    }

    @Test
    void getListingById_whenMissing_throwsEntityNotFoundException() {
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.getListingById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ----------------------------------------------------------------------
    // createListing
    // ----------------------------------------------------------------------

    @SuppressWarnings("null")
    @Test
    void createListing_savesAndReturnsListing() {
        when(listingRepository.save(any(Listing.class))).thenReturn(sampleListing);

        @SuppressWarnings("null")
        Listing result = listingService.createListing(sampleListing);

        assertThat(result.getId()).isEqualTo(1L);
        verify(listingRepository).save(sampleListing);
    }

    // ----------------------------------------------------------------------
    // updateListing
    // ----------------------------------------------------------------------

    @SuppressWarnings("null")
    @Test
    void updateListing_whenExists_savesUpdatedListing() {
        Listing update = Listing.builder()
                .address("New Address")
                .city("Minneapolis")
                .listPrice(900000L)
                .build();

        when(listingRepository.findById(1L)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.updateListing(1L, update);

        // The service sets the id from the existing record onto the updated payload
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAddress()).isEqualTo("New Address");
        assertThat(result.getListPrice()).isEqualTo(900000L);
    }

    @Test
    void updateListing_whenMissing_throwsEntityNotFoundException() {
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.updateListing(999L, sampleListing))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ----------------------------------------------------------------------
    // deleteListing
    // ----------------------------------------------------------------------

    @Test
    void deleteListing_callsRepositoryDelete() {
        listingService.deleteListing(1L);

        verify(listingRepository).deleteById(1L);
    }

    // ----------------------------------------------------------------------
    // searchListings — verifies the service builds a Specification and
    // passes the right pagination defaults to the repository.
    //
    // We do NOT verify the Specification's actual SQL behavior here — that's
    // an integration concern that requires a real (or @DataJpaTest) database.
    // ----------------------------------------------------------------------

    @SuppressWarnings({ "unchecked", "null" })
    @Test
    void searchListings_withDefaultParams_callsRepositoryWithSpec() {
        ListingSearchParams params = new ListingSearchParams();

        @SuppressWarnings("null")
        Page<Listing> mockPage = new PageImpl<>(List.of(sampleListing));
        when(listingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<Listing> result = listingService.searchListings(params);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMlsId()).isEqualTo("7001001");
        verify(listingRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @SuppressWarnings({ "unchecked", "null" })
    @Test
    void searchListings_withCustomPageSize_passesThroughPageable() {
        ListingSearchParams params = new ListingSearchParams();
        params.setPage(2);
        params.setSize(12);

        when(listingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        listingService.searchListings(params);

        // Captures the actual Pageable argument to verify the page/size we passed
        verify(listingRepository).findAll(
                any(Specification.class),
                org.mockito.ArgumentMatchers.argThat((Pageable p) ->
                        p.getPageNumber() == 2 && p.getPageSize() == 12)
        );
    }
}