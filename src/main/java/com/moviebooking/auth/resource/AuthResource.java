package com.moviebooking.auth.resource;

import com.moviebooking.auth.dto.AuthResponse;
import com.moviebooking.auth.dto.LoginRequest;
import com.moviebooking.auth.dto.OAuthLoginRequest;
import com.moviebooking.auth.dto.RefreshRequest;
import com.moviebooking.auth.dto.RegisterRequest;
import com.moviebooking.auth.service.AuthService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/oauth")
    @PermitAll
    public Response oauthLogin(@Valid OAuthLoginRequest request) {
        AuthResponse response = authService.oauthLogin(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest request) {
        AuthResponse response = authService.login(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/refresh")
    @PermitAll
    public Response refresh(@Valid RefreshRequest request) {
        AuthResponse response = authService.refresh(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/logout")
    @RolesAllowed({"USER", "ADMIN"})
    public Response logout() {
        return Response.noContent().build();
    }
}
