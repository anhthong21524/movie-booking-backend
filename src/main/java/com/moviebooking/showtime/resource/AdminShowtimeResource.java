package com.moviebooking.showtime.resource;

import com.moviebooking.showtime.dto.ShowtimeCreateRequest;
import com.moviebooking.showtime.dto.ShowtimeUpdateRequest;
import com.moviebooking.showtime.service.ShowtimeService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin/showtimes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AdminShowtimeResource {

    @Inject
    ShowtimeService showtimeService;

    @GET
    public Response adminListShowtimes(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") String sort,
            @QueryParam("movieId") Long movieId,
            @QueryParam("fromTime") String fromTime,
            @QueryParam("toTime") String toTime) {
        return Response.ok(
                showtimeService.adminListShowtimes(page, size, sort, movieId, fromTime, toTime)).build();
    }

    @POST
    public Response createShowtime(
            @HeaderParam("Idempotency-Key") String idempotencyKey,
            @Valid ShowtimeCreateRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(showtimeService.create(request))
                .build();
    }

    @PUT
    @Path("/{showtimeId}")
    public Response updateShowtime(
            @PathParam("showtimeId") Long showtimeId,
            @HeaderParam("Idempotency-Key") String idempotencyKey,
            @Valid ShowtimeUpdateRequest request) {
        return Response.ok(showtimeService.update(showtimeId, request)).build();
    }

    @DELETE
    @Path("/{showtimeId}")
    public Response deleteShowtime(@PathParam("showtimeId") Long showtimeId) {
        showtimeService.delete(showtimeId);
        return Response.noContent().build();
    }
}
