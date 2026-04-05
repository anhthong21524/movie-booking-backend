package com.moviebooking.showtime.resource;

import com.moviebooking.showtime.service.ShowtimeService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/showtimes")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class ShowtimeResource {

    @Inject
    ShowtimeService showtimeService;

    @GET
    public Response listShowtimes(
            @QueryParam("movieId") Long movieId,
            @QueryParam("fromTime") String fromTime,
            @QueryParam("toTime") String toTime) {
        return Response.ok(Map.of("items", showtimeService.listShowtimes(movieId, fromTime, toTime))).build();
    }

    @GET
    @Path("/{showtimeId}")
    public Response getShowtime(@PathParam("showtimeId") Long showtimeId) {
        return Response.ok(showtimeService.getShowtime(showtimeId)).build();
    }

    @GET
    @Path("/{showtimeId}/seats")
    public Response listShowtimeSeats(@PathParam("showtimeId") Long showtimeId) {
        return Response.ok(showtimeService.getSeats(showtimeId)).build();
    }
}
