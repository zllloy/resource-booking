package com.ramil.booking.resource_booking.api.graphql.booking;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.ramil.booking.resource_booking.domain.booking.dto.BookingView;
import com.ramil.booking.resource_booking.domain.booking.dto.CreateBookingCommand;
import com.ramil.booking.resource_booking.domain.booking.service.BookingService;

@Controller
public class BookingMutation {

    private final BookingService bookingService;

    public BookingMutation(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @MutationMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public BookingView createBookingDraft(@Argument CreateBookingInput input) {
        OffsetDateTime start = OffsetDateTime.parse(input.startTime());
        OffsetDateTime end = OffsetDateTime.parse(input.endTime());

        return bookingService.createDraft(new CreateBookingCommand(
                UUID.fromString(input.userId()),
                UUID.fromString(input.resourceId()),
                start,
                end
        ));
    }

    @MutationMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public BookingView cancelBooking(@Argument UUID id) {
        return bookingService.cancel(id);
    }

    public record CreateBookingInput(String userId, String resourceId, String startTime, String endTime) {}
}
