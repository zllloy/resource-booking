package com.ramil.booking.resource_booking.api.graphql.booking;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.ramil.booking.resource_booking.domain.booking.dto.BookingView;
import com.ramil.booking.resource_booking.domain.booking.service.BookingService;

@Controller
public class BookingQuery {

    private final BookingService bookingService;

    public BookingQuery(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BookingView booking(@Argument UUID id) {
        return bookingService.getById(id);
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<BookingView> myBookings(@Argument UUID userId) {
        return bookingService.listForUser(userId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingView> allBookings() {
        return bookingService.listAll();
    }
}

