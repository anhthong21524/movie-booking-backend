package com.moviebooking.showtime.repository;

import com.moviebooking.showtime.entity.Seat;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SeatRepository implements PanacheRepository<Seat> {

    public List<Seat> findByShowtimeId(Long showtimeId) {
        return list("showtime.id = ?1 ORDER BY row, number", showtimeId);
    }
}
