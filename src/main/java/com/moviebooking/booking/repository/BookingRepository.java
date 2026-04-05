package com.moviebooking.booking.repository;

import com.moviebooking.booking.entity.Booking;
import com.moviebooking.booking.entity.BookingStatus;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class BookingRepository implements PanacheRepository<Booking> {

    public Optional<Booking> findByIdempotencyKey(String idempotencyKey) {
        return find("idempotencyKey", idempotencyKey).firstResultOptional();
    }

    public PanacheQuery<Booking> findByUserId(Long userId, BookingStatus status, Sort sort) {
        StringBuilder query = new StringBuilder("user.id = :userId");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        if (status != null) {
            query.append(" and status = :status");
            params.put("status", status);
        }

        return find(query.toString(), sort, params);
    }
}
