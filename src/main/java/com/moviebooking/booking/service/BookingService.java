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
import com.moviebooking.showtime.dto.SeatInfo;
import com.moviebooking.showtime.entity.Seat;
import com.moviebooking.showtime.entity.SeatStatus;
import com.moviebooking.showtime.entity.Showtime;
import com.moviebooking.showtime.repository.SeatRepository;
import com.moviebooking.showtime.repository.ShowtimeRepository;
import com.moviebooking.user.entity.User;
import com.moviebooking.user.repository.UserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.Instant;
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
    SeatRepository seatRepository;

    @Inject
    UserRepository userRepository;

    @Transactional
    public BookingResponse createBooking(Long userId, BookingCreateRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Booking> existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return mapToBookingResponse(existing.get());
            }
        }

        List<Long> seatIds = request.getSeatIds();
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new AppException(Response.Status.BAD_REQUEST, "VALIDATION_ERROR",
                    "Seat IDs must be unique.",
                    Collections.singletonList(new FieldError("seatIds", "must contain unique items")));
        }

        Showtime showtime = showtimeRepository.findByIdOptional(request.getShowtimeId())
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "SHOWTIME_NOT_FOUND", "No showtime found with the given ID."));

        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new AppException(
                        Response.Status.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required."));

        List<Seat> seats = seatRepository.findByShowtimeId(showtime.getId()).stream()
                .filter(s -> seatIds.contains(s.getId()))
                .collect(Collectors.toList());

        if (seats.size() != seatIds.size()) {
            throw new AppException(Response.Status.BAD_REQUEST, "VALIDATION_ERROR",
                    "One or more seat IDs are invalid for this showtime.",
                    Collections.singletonList(new FieldError("seatIds", "invalid seat IDs")));
        }

        List<Seat> unavailable = seats.stream()
                .filter(s -> s.getStatus() != SeatStatus.AVAILABLE)
                .collect(Collectors.toList());

        if (!unavailable.isEmpty()) {
            throw new AppException(Response.Status.CONFLICT, "SEAT_ALREADY_RESERVED",
                    "One or more selected seats are already reserved.",
                    Collections.singletonList(new FieldError("seatIds", "already reserved")));
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setIdempotencyKey(idempotencyKey);
        booking.setConfirmedAt(Instant.now());
        bookingRepository.persist(booking);

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);

            BookingSeat bs = new BookingSeat();
            bs.setBooking(booking);
            bs.setShowtime(showtime);
            bs.setSeatNumber(seat.getLabel());
            bookingSeatRepository.persist(bs);
        }

        showtime.setAvailableSeats(showtime.getAvailableSeats() - seats.size());

        BigDecimal unitPrice = showtime.getPrice() != null ? showtime.getPrice() : BigDecimal.ZERO;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(seats.size()));

        return mapToBookingResponse(booking, seats, unitPrice, totalAmount);
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

        booking.setStatus(BookingStatus.CANCELLED);

        Showtime showtime = booking.getShowtime();
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
        showtime.setAvailableSeats(showtime.getAvailableSeats() + bookingSeats.size());

        List<Seat> seats = seatRepository.findByShowtimeId(showtime.getId()).stream()
                .filter(s -> bookingSeats.stream()
                        .anyMatch(bs -> bs.getSeatNumber().equals(s.getLabel())))
                .collect(Collectors.toList());

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
        }

        return mapToBookingResponse(booking);
    }

    public BookingResponse getBookingById(Long bookingId, Long currentUserId) {
        Booking booking = bookingRepository.findByIdOptional(bookingId)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "BOOKING_NOT_FOUND", "No booking found with the given ID."));

        if (!booking.getUser().getId().equals(currentUserId)) {
            throw new AppException(Response.Status.FORBIDDEN, "FORBIDDEN",
                    "You do not have permission to access this resource.");
        }

        return mapToBookingResponse(booking);
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
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
        List<Seat> seats = bookingSeats.stream()
                .flatMap(bs -> seatRepository.findByShowtimeId(booking.getShowtime().getId()).stream()
                        .filter(s -> s.getLabel().equals(bs.getSeatNumber())))
                .collect(Collectors.toList());
        BigDecimal unitPrice = booking.getShowtime().getPrice() != null
                ? booking.getShowtime().getPrice() : BigDecimal.ZERO;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(seats.size()));
        return mapToBookingResponse(booking, seats, unitPrice, totalAmount);
    }

    private BookingResponse mapToBookingResponse(Booking booking, List<Seat> seats,
            BigDecimal unitPrice, BigDecimal totalAmount) {
        BookingResponse response = new BookingResponse();
        response.setId(String.valueOf(booking.getId()));
        response.setShowtimeId(String.valueOf(booking.getShowtime().getId()));
        response.setSeatIds(seats.stream().map(s -> String.valueOf(s.getId())).collect(Collectors.toList()));
        response.setSeats(seats.stream()
                .map(s -> new SeatInfo(String.valueOf(s.getId()), s.getLabel(), s.getRow(), s.getNumber(), s.getStatus()))
                .collect(Collectors.toList()));
        response.setUnitPrice(unitPrice);
        response.setTotalAmount(totalAmount);
        response.setStatus(booking.getStatus());
        response.setCreatedAt(booking.getCreatedAt().toString());
        if (booking.getConfirmedAt() != null) {
            response.setConfirmedAt(booking.getConfirmedAt().toString());
        }
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
