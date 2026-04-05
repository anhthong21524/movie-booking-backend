package com.moviebooking.showtime.dto;

import com.moviebooking.showtime.entity.SeatStatus;

public class SeatInfo {

    private String id;
    private String label;
    private String row;
    private int number;
    private SeatStatus status;

    public SeatInfo() {}

    public SeatInfo(String id, String label, String row, int number, SeatStatus status) {
        this.id = id;
        this.label = label;
        this.row = row;
        this.number = number;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }
}
