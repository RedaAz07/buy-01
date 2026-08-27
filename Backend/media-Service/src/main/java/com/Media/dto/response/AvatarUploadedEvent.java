package com.Media.dto.response;

public record AvatarUploadedEvent(
    String userId,
    String avatarUrl
) {}