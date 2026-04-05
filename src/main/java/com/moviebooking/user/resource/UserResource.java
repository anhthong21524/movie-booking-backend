package com.moviebooking.user.resource;

import com.moviebooking.booking.entity.BookingStatus;
import com.moviebooking.booking.service.BookingService;
import com.moviebooking.user.dto.AvatarUpdateRequest;
import com.moviebooking.user.service.UserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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

    @PUT
    @Path("/me/avatar")
    @RolesAllowed({"USER", "ADMIN"})
    public Response updateAvatar(@Valid AvatarUpdateRequest request) {
        Long userId = Long.parseLong(jwt.getSubject());
        return Response.ok(userService.updateAvatar(userId, request.getAvatar())).build();
    }

    @DELETE
    @Path("/me/avatar")
    @RolesAllowed({"USER", "ADMIN"})
    public Response removeAvatar() {
        Long userId = Long.parseLong(jwt.getSubject());
        return Response.ok(userService.removeAvatar(userId)).build();
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
