package com.moviebooking.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AvatarUpdateRequest {

    @NotBlank
    @Size(max = 204800, message = "Avatar data must not exceed 200 KB")
    private String avatar;

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
