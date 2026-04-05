package com.moviebooking.movie.repository;

import com.moviebooking.movie.entity.Movie;
import com.moviebooking.movie.entity.MovieStatus;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class MovieRepository implements PanacheRepository<Movie> {

    public PanacheQuery<Movie> search(MovieStatus status, String keyword, Sort sort) {
        StringBuilder query = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        if (status != null) {
            query.append("status = :status");
            params.put("status", status);
        }

        if (keyword != null && !keyword.isBlank()) {
            if (query.length() > 0) {
                query.append(" and ");
            }
            query.append("lower(title) like lower(:keyword)");
            params.put("keyword", "%" + keyword.toLowerCase() + "%");
        }

        if (query.length() == 0) {
            return findAll(sort);
        }

        return find(query.toString(), sort, params);
    }
}
