package com.moviebooking.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BookingCreateRequest {

    @NotNull
    @Min(1)
    private Long showtimeId;

    @NotEmpty
    @Size(min = 1, max = 20)
    private List<@NotNull @Pattern(regexp = "^[A-Z][0-9]{1,2}$") String> seatNumbers;

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
    }

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(List<String> seatNumbers) {
        this.seatNumbers = seatNumbers;
    }
}
