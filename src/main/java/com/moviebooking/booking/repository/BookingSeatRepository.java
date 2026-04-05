package com.moviebooking.booking.repository;

import com.moviebooking.booking.entity.BookingSeat;
import com.moviebooking.booking.entity.BookingStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BookingSeatRepository implements PanacheRepository<BookingSeat> {

    public List<BookingSeat> findByBookingId(Long bookingId) {
        return find("booking.id", bookingId).list();
    }

    public List<BookingSeat> findActiveByShowtimeId(Long showtimeId) {
        return find("showtime.id = :sid and booking.status != :cancelled",
                Map.of("sid", showtimeId, "cancelled", BookingStatus.CANCELLED))
                .list();
    }

    public boolean anyActiveForSeats(Long showtimeId, List<String> seatNumbers) {
        Map<String, Object> params = new HashMap<>();
        params.put("sid", showtimeId);
        params.put("seats", seatNumbers);
        params.put("cancelled", BookingStatus.CANCELLED);
        return count(
                "showtime.id = :sid and seatNumber in :seats and booking.status != :cancelled",
                params) > 0;
    }
}
