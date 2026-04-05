package com.moviebooking.showtime.repository;

import com.moviebooking.showtime.entity.Showtime;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ShowtimeRepository implements PanacheRepository<Showtime> {

    public boolean hasOverlap(String room, Instant startTime, Instant endTime, Long excludeId) {
        StringBuilder query = new StringBuilder(
                "room = :room and startTime < :endTime and endTime > :startTime");
        Map<String, Object> params = new HashMap<>();
        params.put("room", room);
        params.put("startTime", startTime);
        params.put("endTime", endTime);

        if (excludeId != null) {
            query.append(" and id != :excludeId");
            params.put("excludeId", excludeId);
        }

        return count(query.toString(), params) > 0;
    }

    public List<Showtime> findByMovieAndTimeRange(Long movieId, Instant fromTime, Instant toTime) {
        StringBuilder query = new StringBuilder("movie.id = :movieId");
        Map<String, Object> params = new HashMap<>();
        params.put("movieId", movieId);

        if (fromTime != null) {
            query.append(" and startTime >= :fromTime");
            params.put("fromTime", fromTime);
        }

        if (toTime != null) {
            query.append(" and startTime <= :toTime");
            params.put("toTime", toTime);
        }

        return find(query.toString(), Sort.by("startTime"), params).list();
    }

    public PanacheQuery<Showtime> adminSearch(Long movieId, Instant fromTime, Instant toTime, Sort sort) {
        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (movieId != null) {
            query.append(" and movie.id = :movieId");
            params.put("movieId", movieId);
        }

        if (fromTime != null) {
            query.append(" and startTime >= :fromTime");
            params.put("fromTime", fromTime);
        }

        if (toTime != null) {
            query.append(" and startTime <= :toTime");
            params.put("toTime", toTime);
        }

        return find(query.toString(), sort, params);
    }
}
