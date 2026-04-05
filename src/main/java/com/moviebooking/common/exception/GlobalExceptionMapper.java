package com.moviebooking.common.exception;

import com.moviebooking.common.response.FieldError;
import com.moviebooking.common.response.ProblemDetail;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class GlobalExceptionMapper {

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> handleAppException(AppException e) {
        ProblemDetail problem = new ProblemDetail(e.getCode(), e.getMessage(), e.getErrors());
        return RestResponse.status(Response.Status.fromStatusCode(e.getStatus().getStatusCode()), problem);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> handleConstraintViolation(ConstraintViolationException e) {
        List<FieldError> fieldErrors = new ArrayList<>();
        for (ConstraintViolation<?> cv : e.getConstraintViolations()) {
            String path = cv.getPropertyPath().toString();
            int dot = path.lastIndexOf('.');
            String field = dot >= 0 ? path.substring(dot + 1) : path;
            fieldErrors.add(new FieldError(field, cv.getMessage()));
        }
        ProblemDetail problem = new ProblemDetail("VALIDATION_ERROR", "Request validation failed.", fieldErrors);
        return RestResponse.status(Response.Status.BAD_REQUEST, problem);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> handleValidationException(jakarta.validation.ValidationException e) {
        // Catches ResteasyViolationException (RESTEasy Reactive's wrapper for @Valid failures)
        if (e instanceof ConstraintViolationException) {
            return handleConstraintViolation((ConstraintViolationException) e);
        }
        // Fallback for other ValidationException subtypes
        List<FieldError> fieldErrors = new ArrayList<>();
        String msg = e.getMessage();
        // RESTEasy Reactive encodes violations in the message; parse them out
        if (msg != null && msg.contains(": ")) {
            for (String part : msg.split(", ")) {
                int colon = part.lastIndexOf(": ");
                if (colon > 0) {
                    String field = part.substring(0, colon).replaceAll(".*\\.", "");
                    String message = part.substring(colon + 2);
                    fieldErrors.add(new FieldError(field, message));
                }
            }
        }
        ProblemDetail problem = new ProblemDetail("VALIDATION_ERROR", "Request validation failed.",
                fieldErrors.isEmpty() ? null : fieldErrors);
        return RestResponse.status(Response.Status.BAD_REQUEST, problem);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> handleUnauthorized(io.quarkus.security.UnauthorizedException e) {
        ProblemDetail problem = new ProblemDetail("UNAUTHORIZED", "Authentication is required.");
        return RestResponse.status(Response.Status.UNAUTHORIZED, problem);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> handleForbidden(io.quarkus.security.ForbiddenException e) {
        ProblemDetail problem = new ProblemDetail("FORBIDDEN", "You do not have permission to access this resource.");
        return RestResponse.status(Response.Status.FORBIDDEN, problem);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> handleDateTimeParse(DateTimeParseException e) {
        ProblemDetail problem = new ProblemDetail("VALIDATION_ERROR",
                "Invalid date-time format. Expected UTC ISO-8601 (e.g. 2026-04-05T10:00:00Z).");
        return RestResponse.status(Response.Status.BAD_REQUEST, problem);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetail> handleGeneral(Exception e) {
        ProblemDetail problem = new ProblemDetail("INTERNAL_ERROR", "An unexpected error occurred.");
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, problem);
    }
}
