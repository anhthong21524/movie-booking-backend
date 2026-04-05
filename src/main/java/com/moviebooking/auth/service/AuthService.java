package com.moviebooking.auth.service;

import com.moviebooking.auth.dto.AuthResponse;
import com.moviebooking.auth.dto.LoginRequest;
import com.moviebooking.auth.dto.RefreshRequest;
import com.moviebooking.auth.dto.RegisterRequest;
import com.moviebooking.common.exception.AppException;
import com.moviebooking.common.response.FieldError;
import com.moviebooking.user.dto.UserResponse;
import com.moviebooking.user.entity.User;
import com.moviebooking.user.entity.UserRole;
import com.moviebooking.user.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    JWTParser jwtParser;

    @ConfigProperty(name = "jwt.issuer")
    String jwtIssuer;

    @ConfigProperty(name = "jwt.access-token.expiry")
    long accessTokenExpiry;

    @ConfigProperty(name = "jwt.refresh-token.expiry")
    long refreshTokenExpiry;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            List<FieldError> errors = Collections.singletonList(
                    new FieldError("email", "already registered"));
            throw new AppException(Response.Status.CONFLICT, "EMAIL_ALREADY_EXISTS",
                    "The email address is already registered.", errors);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(BcryptUtil.bcryptHash(request.getPassword()));
        user.setRole(UserRole.USER);
        userRepository.persist(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(
                        Response.Status.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password."));

        if (!BcryptUtil.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(Response.Status.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password.");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        try {
            JsonWebToken token = jwtParser.parse(request.getRefreshToken());

            String type = token.getClaim("type");
            if (!"refresh".equals(type)) {
                throw new AppException(Response.Status.UNAUTHORIZED, "UNAUTHORIZED", "Invalid refresh token.");
            }

            Long userId = Long.parseLong(token.getSubject());
            User user = userRepository.findByIdOptional(userId)
                    .orElseThrow(() -> new AppException(
                            Response.Status.UNAUTHORIZED, "UNAUTHORIZED", "Invalid refresh token."));

            return buildAuthResponse(user);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(Response.Status.UNAUTHORIZED, "UNAUTHORIZED", "Invalid or expired refresh token.");
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = Jwt.issuer(jwtIssuer)
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .groups(Collections.singleton(user.getRole().name()))
                .expiresIn(Duration.ofSeconds(accessTokenExpiry))
                .sign();

        String refreshToken = Jwt.issuer(jwtIssuer)
                .subject(String.valueOf(user.getId()))
                .claim("type", "refresh")
                .expiresIn(Duration.ofSeconds(refreshTokenExpiry))
                .sign();

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setRole(user.getRole());
        userResponse.setCreatedAt(user.getCreatedAt());

        return new AuthResponse(accessToken, refreshToken, "Bearer", (int) accessTokenExpiry, userResponse);
    }
}
