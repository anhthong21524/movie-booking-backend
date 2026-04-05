package com.moviebooking.movie.resource;

import com.moviebooking.movie.entity.MovieStatus;
import com.moviebooking.movie.service.MovieService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/movies")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class MovieResource {

    @Inject
    MovieService movieService;

    @GET
    public Response listMovies(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") String sort,
            @QueryParam("status") MovieStatus status,
            @QueryParam("keyword") String keyword) {
        return Response.ok(movieService.listMovies(page, size, sort, status, keyword)).build();
    }

    @GET
    @Path("/{movieId}")
    public Response getMovieById(@PathParam("movieId") Long movieId) {
        return Response.ok(movieService.findById(movieId)).build();
    }
}
