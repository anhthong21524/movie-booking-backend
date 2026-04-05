package com.moviebooking.user.resource;

import com.moviebooking.booking.entity.BookingStatus;
import com.moviebooking.booking.service.BookingService;
import com.moviebooking.user.service.UserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    UserService userService;

    @Inject
    BookingService bookingService;

    @GET
    @Path("/me")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getMyProfile() {
        Long userId = Long.parseLong(jwt.getSubject());
        return Response.ok(userService.findById(userId)).build();
    }

    @GET
    @Path("/me/bookings")
    @RolesAllowed({"USER", "ADMIN"})
    public Response listMyBookings(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") String sort,
            @QueryParam("status") BookingStatus status) {
        Long userId = Long.parseLong(jwt.getSubject());
        return Response.ok(bookingService.listMyBookings(userId, page, size, sort, status)).build();
    }
}
