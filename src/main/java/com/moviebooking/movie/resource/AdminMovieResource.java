package com.moviebooking.movie.resource;

import com.moviebooking.movie.dto.MovieCreateRequest;
import com.moviebooking.movie.dto.MovieUpdateRequest;
import com.moviebooking.movie.service.MovieService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin/movies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
public class AdminMovieResource {

    @Inject
    MovieService movieService;

    @POST
    public Response createMovie(
            @HeaderParam("Idempotency-Key") String idempotencyKey,
            @Valid MovieCreateRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(movieService.create(request))
                .build();
    }

    @PUT
    @Path("/{movieId}")
    public Response updateMovie(
            @PathParam("movieId") Long movieId,
            @HeaderParam("Idempotency-Key") String idempotencyKey,
            @Valid MovieUpdateRequest request) {
        return Response.ok(movieService.update(movieId, request)).build();
    }

    @DELETE
    @Path("/{movieId}")
    public Response deleteMovie(@PathParam("movieId") Long movieId) {
        movieService.delete(movieId);
        return Response.noContent().build();
    }
}
