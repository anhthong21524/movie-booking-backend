package com.moviebooking.user.service;

import com.moviebooking.common.exception.AppException;
import com.moviebooking.user.dto.UserResponse;
import com.moviebooking.user.entity.User;
import com.moviebooking.user.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

    @Transactional
    public UserResponse updateAvatar(Long userId, String avatar) {
        if (!avatar.startsWith("data:image/")) {
            throw new AppException(Response.Status.BAD_REQUEST, "INVALID_AVATAR",
                    "Avatar must be a valid image data URL.");
        }

        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "USER_NOT_FOUND", "No user found with the given ID."));

        user.setAvatar(avatar);
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse removeAvatar(Long userId) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new AppException(
                        Response.Status.NOT_FOUND, "USER_NOT_FOUND", "No user found with the given ID."));

        user.setAvatar(null);
        return mapToResponse(user);
    }

    public UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        response.setAvatar(user.getAvatar());
        return response;
    }
}
