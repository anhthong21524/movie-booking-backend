package com.moviebooking.showtime.dto;

import com.moviebooking.showtime.entity.SeatStatus;

public class SeatInfo {

    private String seatNumber;
    private SeatStatus status;

    public SeatInfo() {}

    public SeatInfo(String seatNumber, SeatStatus status) {
        this.seatNumber = seatNumber;
        this.status = status;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }
}
