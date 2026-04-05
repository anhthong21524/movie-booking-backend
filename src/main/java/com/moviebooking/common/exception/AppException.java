package com.moviebooking.common.exception;

import com.moviebooking.common.response.FieldError;
import jakarta.ws.rs.core.Response;

import java.util.List;

public class AppException extends RuntimeException {

    private final Response.Status status;
    private final String code;
    private final List<FieldError> errors;

    public AppException(Response.Status status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = null;
    }

    public AppException(Response.Status status, String code, String message, List<FieldError> errors) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = errors;
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<FieldError> getErrors() {
        return errors;
    }
}
