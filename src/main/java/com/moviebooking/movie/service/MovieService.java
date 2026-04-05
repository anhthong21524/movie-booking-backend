package com.moviebooking.movie.service;

import com.moviebooking.common.exception.AppException;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.common.util.SortUtil;
import com.moviebooking.movie.dto.MovieCreateRequest;
import com.moviebooking.movie.dto.MovieResponse;
import com.moviebooking.movie.dto.MovieUpdateRequest;
import com.moviebooking.movie.entity.Movie;
import com.moviebooking.movie.entity.MovieStatus;
import com.moviebooking.movie.repository.MovieRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class MovieService {

    @Inject
    MovieRepository movieRepository;

    public PageResponse<MovieResponse> listMovies(int page, int size, String sortParam,
            MovieStatus status, String keyword) {
        Sort sort = SortUtil.parse(sortParam, "createdAt");
        PanacheQuery<Movie> query = movieRepository.search(status, keyword, sort);
        query.page(page, size);
        long totalItems = query.count();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        List<MovieResponse> items = query.list().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(items, page, size, totalItems, totalPages);
    }

    public MovieResponse findById(Long id) {
        Movie movie = movieRepository.findByIdOptional(id)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "MOVIE_NOT_FOUND", "No movie found with the given ID."));
        return mapToResponse(movie);
    }

    @Transactional
    public MovieResponse create(MovieCreateRequest request) {
        Movie movie = new Movie();
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setPosterUrl(request.getPosterUrl());
        movie.setStatus(request.getStatus());
        movie.setGenre(request.getGenre());
        movie.setRating(request.getRating());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setBasePrice(request.getBasePrice());
        movieRepository.persist(movie);
        return mapToResponse(movie);
    }

    @Transactional
    public MovieResponse update(Long id, MovieUpdateRequest request) {
        Movie movie = movieRepository.findByIdOptional(id)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "MOVIE_NOT_FOUND", "No movie found with the given ID."));
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setPosterUrl(request.getPosterUrl());
        movie.setStatus(request.getStatus());
        movie.setGenre(request.getGenre());
        movie.setRating(request.getRating());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setBasePrice(request.getBasePrice());
        return mapToResponse(movie);
    }

    @Transactional
    public void delete(Long id) {
        Movie movie = movieRepository.findByIdOptional(id)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "MOVIE_NOT_FOUND", "No movie found with the given ID."));
        movieRepository.delete(movie);
    }

    public MovieResponse mapToResponse(Movie movie) {
        MovieResponse response = new MovieResponse();
        response.setId(String.valueOf(movie.getId()));
        response.setTitle(movie.getTitle());
        response.setDescription(movie.getDescription());
        response.setDurationMinutes(movie.getDurationMinutes());
        response.setPosterUrl(movie.getPosterUrl());
        response.setStatus(movie.getStatus());
        response.setGenre(movie.getGenre());
        response.setRating(movie.getRating());
        response.setReleaseDate(movie.getReleaseDate() != null ? movie.getReleaseDate().toString() : null);
        response.setBasePrice(movie.getBasePrice());
        return response;
    }
}
