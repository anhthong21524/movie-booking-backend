package com.moviebooking.user.service;

import com.moviebooking.common.exception.AppException;
import com.moviebooking.user.dto.UserResponse;
import com.moviebooking.user.entity.User;
import com.moviebooking.user.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    public UserResponse findById(Long id) {
        User user = userRepository.findByIdOptional(id)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "USER_NOT_FOUND", "No user found with the given ID."));
        return mapToResponse(user);
    }

    public UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
