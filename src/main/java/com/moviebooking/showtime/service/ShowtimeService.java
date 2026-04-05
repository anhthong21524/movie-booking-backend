package com.moviebooking.showtime.service;

import com.moviebooking.booking.entity.BookingSeat;
import com.moviebooking.booking.repository.BookingSeatRepository;
import com.moviebooking.common.exception.AppException;
import com.moviebooking.common.response.FieldError;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.common.util.SortUtil;
import com.moviebooking.movie.entity.Movie;
import com.moviebooking.movie.repository.MovieRepository;
import com.moviebooking.showtime.dto.SeatAvailabilityResponse;
import com.moviebooking.showtime.dto.SeatInfo;
import com.moviebooking.showtime.dto.ShowtimeCreateRequest;
import com.moviebooking.showtime.dto.ShowtimeResponse;
import com.moviebooking.showtime.dto.ShowtimeUpdateRequest;
import com.moviebooking.showtime.entity.Seat;
import com.moviebooking.showtime.entity.SeatStatus;
import com.moviebooking.showtime.entity.Showtime;
import com.moviebooking.showtime.repository.SeatRepository;
import com.moviebooking.showtime.repository.ShowtimeRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ShowtimeService {

    @Inject
    ShowtimeRepository showtimeRepository;

    @Inject
    MovieRepository movieRepository;

    @Inject
    BookingSeatRepository bookingSeatRepository;

    @Inject
    SeatRepository seatRepository;

    public List<ShowtimeResponse> listShowtimes(Long movieId, String fromTime, String toTime) {
        movieRepository.findByIdOptional(movieId)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "MOVIE_NOT_FOUND", "No movie found with the given ID."));

        Instant from = fromTime != null ? Instant.parse(fromTime) : null;
        Instant to = toTime != null ? Instant.parse(toTime) : null;

        return showtimeRepository.findByMovieAndTimeRange(movieId, from, to)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SeatAvailabilityResponse getSeats(Long showtimeId) {
        Showtime showtime = showtimeRepository.findByIdOptional(showtimeId)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "SHOWTIME_NOT_FOUND", "No showtime found with the given ID."));

        List<Seat> seatEntities = seatRepository.findByShowtimeId(showtimeId);

        List<SeatInfo> seats = seatEntities.stream()
                .map(s -> new SeatInfo(
                        String.valueOf(s.getId()),
                        s.getLabel(),
                        s.getRow(),
                        s.getNumber(),
                        s.getStatus()))
                .collect(Collectors.toList());

        SeatAvailabilityResponse response = new SeatAvailabilityResponse();
        response.setShowtimeId(String.valueOf(showtimeId));
        response.setCapacity(showtime.getTotalSeats());
        response.setRemainingSeats(showtime.getAvailableSeats());
        response.setSeats(seats);
        return response;
    }

    public PageResponse<ShowtimeResponse> adminListShowtimes(int page, int size, String sortParam,
            Long movieId, String fromTime, String toTime) {
        Sort sort = SortUtil.parse(sortParam, "startTime");
        Instant from = fromTime != null ? Instant.parse(fromTime) : null;
        Instant to = toTime != null ? Instant.parse(toTime) : null;

        PanacheQuery<Showtime> query = showtimeRepository.adminSearch(movieId, from, to, sort);
        query.page(page, size);
        long totalItems = query.count();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        List<ShowtimeResponse> items = query.list().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(items, page, size, totalItems, totalPages);
    }

    @Transactional
    public ShowtimeResponse create(ShowtimeCreateRequest request) {
        Movie movie = movieRepository.findByIdOptional(request.getMovieId())
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "MOVIE_NOT_FOUND", "No movie found with the given ID."));

        Instant startTime = Instant.parse(request.getStartTime());
        Instant endTime = Instant.parse(request.getEndTime());

        validateTimeRange(startTime, endTime);

        if (showtimeRepository.hasOverlap(request.getRoom(), startTime, endTime, null)) {
            throw new AppException(Response.Status.CONFLICT, "SHOWTIME_OVERLAP",
                    "The requested time slot overlaps an existing showtime in room " + request.getRoom() + ".",
                    Collections.singletonList(new FieldError("room", "overlapping schedule")));
        }

        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(request.getRoom());
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtime.setTotalSeats(request.getTotalSeats());
        showtime.setAvailableSeats(request.getTotalSeats());
        showtimeRepository.persist(showtime);
        return mapToResponse(showtime);
    }

    @Transactional
    public ShowtimeResponse update(Long id, ShowtimeUpdateRequest request) {
        Showtime showtime = showtimeRepository.findByIdOptional(id)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "SHOWTIME_NOT_FOUND", "No showtime found with the given ID."));

        Instant startTime = Instant.parse(request.getStartTime());
        Instant endTime = Instant.parse(request.getEndTime());

        validateTimeRange(startTime, endTime);

        if (showtimeRepository.hasOverlap(request.getRoom(), startTime, endTime, id)) {
            throw new AppException(Response.Status.CONFLICT, "SHOWTIME_OVERLAP",
                    "The requested time slot overlaps an existing showtime in room " + request.getRoom() + ".",
                    Collections.singletonList(new FieldError("room", "overlapping schedule")));
        }

        showtime.setRoom(request.getRoom());
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtime.setTotalSeats(request.getTotalSeats());
        return mapToResponse(showtime);
    }

    @Transactional
    public void delete(Long id) {
        Showtime showtime = showtimeRepository.findByIdOptional(id)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "SHOWTIME_NOT_FOUND", "No showtime found with the given ID."));
        showtimeRepository.delete(showtime);
    }

    private void validateTimeRange(Instant startTime, Instant endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new AppException(Response.Status.BAD_REQUEST, "VALIDATION_ERROR",
                    "End time must be after start time.",
                    Collections.singletonList(new FieldError("endTime", "must be after startTime")));
        }
    }

    public ShowtimeResponse getShowtime(Long id) {
        Showtime showtime = showtimeRepository.findByIdOptional(id)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "SHOWTIME_NOT_FOUND", "No showtime found with the given ID."));
        return mapToResponse(showtime);
    }

    public ShowtimeResponse mapToResponse(Showtime showtime) {
        ShowtimeResponse response = new ShowtimeResponse();
        response.setId(String.valueOf(showtime.getId()));
        response.setMovieId(String.valueOf(showtime.getMovie().getId()));
        response.setRoomName(showtime.getRoom());
        response.setStartsAt(showtime.getStartTime().toString());
        response.setEndsAt(showtime.getEndTime().toString());
        response.setCapacity(showtime.getTotalSeats());
        response.setRemainingSeats(showtime.getAvailableSeats());
        response.setPrice(showtime.getPrice());
        return response;
    }
}
