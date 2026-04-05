package com.moviebooking.booking.resource;

import com.moviebooking.booking.dto.BookingCancelRequest;
import com.moviebooking.booking.dto.BookingCreateRequest;
import com.moviebooking.booking.service.BookingService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
public class BookingResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    BookingService bookingService;

    @POST
    public Response createBooking(
            @HeaderParam("Idempotency-Key") String idempotencyKey,
            @Valid BookingCreateRequest request) {
        Long userId = Long.parseLong(jwt.getSubject());
        return Response.status(Response.Status.CREATED)
                .entity(bookingService.createBooking(userId, request, idempotencyKey))
                .build();
    }

    @PATCH
    @Path("/{bookingId}")
    public Response cancelBooking(
            @PathParam("bookingId") Long bookingId,
            @Valid BookingCancelRequest request) {
        Long userId = Long.parseLong(jwt.getSubject());
        return Response.ok(bookingService.cancelBooking(bookingId, userId)).build();
    }
}
