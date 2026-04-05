package com.moviebooking.booking.service;

import com.moviebooking.booking.dto.BookingCreateRequest;
import com.moviebooking.booking.dto.BookingResponse;
import com.moviebooking.booking.dto.BookingSummaryResponse;
import com.moviebooking.booking.entity.Booking;
import com.moviebooking.booking.entity.BookingSeat;
import com.moviebooking.booking.entity.BookingStatus;
import com.moviebooking.booking.repository.BookingRepository;
import com.moviebooking.booking.repository.BookingSeatRepository;
import com.moviebooking.common.exception.AppException;
import com.moviebooking.common.response.FieldError;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.common.util.SortUtil;
import com.moviebooking.showtime.entity.Showtime;
import com.moviebooking.showtime.repository.ShowtimeRepository;
import com.moviebooking.user.entity.User;
import com.moviebooking.user.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class BookingService {

    @Inject
    BookingRepository bookingRepository;

    @Inject
    BookingSeatRepository bookingSeatRepository;

    @Inject
    ShowtimeRepository showtimeRepository;

    @Inject
    UserRepository userRepository;

    @Transactional
    public BookingResponse createBooking(Long userId, BookingCreateRequest request, String idempotencyKey) {
        // Idempotency check
        if (idempotencyKey != null) {
            Optional<Booking> existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return mapToBookingResponse(existing.get());
            }
        }

        // Validate unique seat numbers in request
        List<String> seatNumbers = request.getSeatNumbers();
        if (new HashSet<>(seatNumbers).size() != seatNumbers.size()) {
            throw new AppException(Response.Status.BAD_REQUEST, "VALIDATION_ERROR",
                    "Seat numbers must be unique.",
                    Collections.singletonList(new FieldError("seatNumbers", "must contain unique items")));
        }

        Showtime showtime = showtimeRepository.findByIdOptional(request.getShowtimeId())
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "SHOWTIME_NOT_FOUND", "No showtime found with the given ID."));

        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new AppException(
                        Response.Status.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required."));

        if (bookingSeatRepository.anyActiveForSeats(showtime.getId(), seatNumbers)) {
            throw new AppException(Response.Status.CONFLICT, "SEAT_ALREADY_RESERVED",
                    "One or more selected seats are already reserved.",
                    Collections.singletonList(new FieldError("seatNumbers", "already reserved")));
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setIdempotencyKey(idempotencyKey);
        bookingRepository.persist(booking);

        for (String seatNumber : seatNumbers) {
            BookingSeat bs = new BookingSeat();
            bs.setBooking(booking);
            bs.setShowtime(showtime);
            bs.setSeatNumber(seatNumber);
            bookingSeatRepository.persist(bs);
        }

        showtime.setAvailableSeats(showtime.getAvailableSeats() - seatNumbers.size());

        return mapToBookingResponse(booking, seatNumbers);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long currentUserId) {
        Booking booking = bookingRepository.findByIdOptional(bookingId)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "BOOKING_NOT_FOUND", "No booking found with the given ID."));

        if (!booking.getUser().getId().equals(currentUserId)) {
            throw new AppException(Response.Status.FORBIDDEN, "FORBIDDEN",
                    "You do not have permission to access this resource.");
        }

        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new AppException(Response.Status.CONFLICT, "BOOKING_ALREADY_CANCELLED",
                    "This booking has already been cancelled.");
        }

        List<BookingSeat> seats = bookingSeatRepository.findByBookingId(booking.getId());
        List<String> seatNumbers = seats.stream()
                .map(BookingSeat::getSeatNumber)
                .collect(Collectors.toList());

        booking.setStatus(BookingStatus.CANCELLED);

        Showtime showtime = booking.getShowtime();
        showtime.setAvailableSeats(showtime.getAvailableSeats() + seats.size());

        return mapToBookingResponse(booking, seatNumbers);
    }

    public PageResponse<BookingSummaryResponse> listMyBookings(Long userId, int page, int size,
            String sortParam, BookingStatus status) {
        Sort sort = SortUtil.parse(sortParam, "createdAt");
        PanacheQuery<Booking> query = bookingRepository.findByUserId(userId, status, sort);
        query.page(page, size);
        long totalItems = query.count();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        List<BookingSummaryResponse> items = query.list().stream()
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(items, page, size, totalItems, totalPages);
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        List<String> seatNumbers = bookingSeatRepository.findByBookingId(booking.getId())
                .stream()
                .map(BookingSeat::getSeatNumber)
                .collect(Collectors.toList());
        return mapToBookingResponse(booking, seatNumbers);
    }

    private BookingResponse mapToBookingResponse(Booking booking, List<String> seatNumbers) {
        BookingResponse response = new BookingResponse();
        response.setBookingId(booking.getId());
        response.setUserId(booking.getUser().getId());
        response.setShowtimeId(booking.getShowtime().getId());
        response.setSeatNumbers(seatNumbers);
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        return response;
    }

    private BookingSummaryResponse mapToSummaryResponse(Booking booking) {
        List<String> seatNumbers = bookingSeatRepository.findByBookingId(booking.getId())
                .stream()
                .map(BookingSeat::getSeatNumber)
                .collect(Collectors.toList());
        BookingSummaryResponse response = new BookingSummaryResponse();
        response.setBookingId(booking.getId());
        response.setShowtimeId(booking.getShowtime().getId());
        response.setSeatNumbers(seatNumbers);
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        return response;
    }
}
