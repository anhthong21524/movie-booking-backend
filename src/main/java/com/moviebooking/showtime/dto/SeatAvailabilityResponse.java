package com.moviebooking.showtime.dto;

import java.util.List;

public class SeatAvailabilityResponse {

    private Long showtimeId;
    private int totalSeats;
    private int availableSeats;
    private List<SeatInfo> seats;

    public SeatAvailabilityResponse() {}

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public List<SeatInfo> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatInfo> seats) {
        this.seats = seats;
    }
}
