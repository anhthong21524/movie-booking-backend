package com.moviebooking.common.exception;

import com.moviebooking.common.response.FieldError;
import com.moviebooking.common.response.ProblemDetail;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof AppException) {
            AppException e = (AppException) exception;
            ProblemDetail problem = new ProblemDetail(e.getCode(), e.getMessage(), e.getErrors());
            return Response.status(e.getStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(problem)
                    .build();
        }

        if (exception instanceof ConstraintViolationException) {
            ConstraintViolationException cve = (ConstraintViolationException) exception;
            List<FieldError> fieldErrors = new ArrayList<>();
            for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
                String path = cv.getPropertyPath().toString();
                int dot = path.lastIndexOf('.');
                String field = dot >= 0 ? path.substring(dot + 1) : path;
                fieldErrors.add(new FieldError(field, cv.getMessage()));
            }
            ProblemDetail problem = new ProblemDetail("VALIDATION_ERROR", "Request validation failed.", fieldErrors);
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(problem)
                    .build();
        }

        if (exception instanceof io.quarkus.security.UnauthorizedException) {
            ProblemDetail problem = new ProblemDetail("UNAUTHORIZED", "Authentication is required.");
            return Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(problem)
                    .build();
        }

        if (exception instanceof io.quarkus.security.ForbiddenException) {
            ProblemDetail problem = new ProblemDetail("FORBIDDEN", "You do not have permission to access this resource.");
            return Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(problem)
                    .build();
        }

        if (exception instanceof DateTimeParseException) {
            ProblemDetail problem = new ProblemDetail("VALIDATION_ERROR", "Invalid date-time format. Expected UTC ISO-8601 (e.g. 2026-04-05T10:00:00Z).");
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(problem)
                    .build();
        }

        ProblemDetail problem = new ProblemDetail("INTERNAL_ERROR", "An unexpected error occurred.");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(problem)
                .build();
    }
}
